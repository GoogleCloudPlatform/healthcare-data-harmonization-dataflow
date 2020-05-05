// Copyright 2020 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.healthcare.etl;

import com.google.api.services.healthcare.v1beta1.model.HttpBody;
import com.google.cloud.healthcare.etl.pipeline.MappingFn;
import com.google.cloud.healthcare.etl.util.GcsUtils;
import com.google.cloud.healthcare.etl.util.GcsUtils.GcsPath;
import com.google.cloud.healthcare.etl.util.Pair;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.healthcare.FhirIO;
import org.apache.beam.sdk.io.gcp.healthcare.HL7v2IO;
import org.apache.beam.sdk.io.gcp.healthcare.HealthcareIOErrorToTableRow;
import org.apache.beam.sdk.io.gcp.healthcare.HttpBodyCoder;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.joda.time.Duration;

/** The entry point of all pipelines. */
public class PipelineRunner {

  // TODO(b/155226578): add more sophisticated validations.
  /** Pipeline options. */
  public interface Options extends DataflowPipelineOptions {
    @Description("The PubSub subscription to listen to, must be of the full format: "
        + "projects/project_id/subscriptions/subscription_id.")
    @Required
    String getPubSubSubscription();
    void setPubSubSubscription(String subSubscription);

    @Description("The path to the mapping configurations. The path will be treated as a GCS path "
        + "if the path starts with the GCS scheme (\"gs\"), otherwise a local file. Please see: "
        + "https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/baa4e0c7849413f7b44505a8410ee7f52745427a/mapping_configs/README.md"
        + " for more details on the mapping configuration structure.")
    @Required
    String getMappingPath();
    void setMappingPath(String gcsPath);

    @Description("The target FHIR Store to write data to, must be of the full format: "
        + "projects/project_id/locations/location/datasets/dataset_id/fhirStores/fhir_store_id")
    @Required
    String getFhirStore();
    void setFhirStore(String fhirStore);

    @Description("The path to a file that used to record all read errors. The path will be treated "
        + "as a GCS path if the path starts with the GCS scheme (\"gs\"), otherwise a local file.")
    @Required
    String getReadErrorPath();
    void setReadErrorPath(String readErrorPath);

    @Description("The path to a file that used to record all write errors. The path will be "
        + "treated as a GCS path if the path starts with the GCS scheme (\"gs\"), otherwise a "
        + "local file.")
    @Required
    String getWriteErrorPath();
    void setWriteErrorPath(String writeErrorPath);
  }

  public static void main(String[] args) {
    Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
    Pipeline pipeline = Pipeline.create(options);

    HL7v2IO.Read.Result readResult = pipeline
        .apply("ReadHL7v2Messages",
            PubsubIO.readStrings().fromSubscription(options.getPubSubSubscription()))
        .apply(HL7v2IO.getAll());

    HealthcareIOErrorToTableRow<String> errorConverter = new HealthcareIOErrorToTableRow<>();
    readResult
        .getFailedReads()
        .apply(
            "ConvertErrors",
            MapElements.into(TypeDescriptors.strings())
                .via(input -> errorConverter.apply(input).toString()))
        .apply(Window.into(FixedWindows.of(Duration.standardSeconds(5))))
        .apply(
            "WriteReadErrors",
            TextIO.write().to(options.getReadErrorPath()).withWindowedWrites().withNumShards(1));

    String mapping;
    try {
      mapping = readMapping(options.getMappingPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    MappingFn mappingFn = new MappingFn(mapping);
    PCollection<String> bundles =
        readResult
            .getMessages()
            // TODO(b/155226578): we should pass the message id along for provenance.
            .apply(MapElements.into(TypeDescriptors.strings()).via(msg -> msg.getSchematizedData()))
            // TODO(b/155226578): do we need such whether we need to wrap as list.
            .apply(MapElements.into(TypeDescriptors.lists(TypeDescriptors.strings()))
                .via(msg -> Collections.singletonList(msg)))
            .apply("MapMessages", ParDo.of(mappingFn))
            .apply(MapElements.into(TypeDescriptors.strings())
                // TODO(b/155226578): ideally we should emit errors if the resource list is empty.
                .via(res -> res.isEmpty() ? "" : res.get(0)));

    FhirIO.Write.Result writeResult = bundles
        .apply(MapElements.into(TypeDescriptor.of(HttpBody.class))
            .via(bundle -> new HttpBody().setData(bundle)))
        .setCoder(new HttpBodyCoder())
        .apply("WriteFHIRBundles", FhirIO.Write.executeBundles(options.getFhirStore()));

    HealthcareIOErrorToTableRow<HttpBody> bundleErrorConverter =
        new HealthcareIOErrorToTableRow<>();
    writeResult.getFailedInsertsWithErr()
        .apply("ConvertBundleErrors", MapElements.into(TypeDescriptors.strings())
            .via(resp -> bundleErrorConverter.apply(resp).toString()))
        .apply(Window.into(FixedWindows.of(Duration.standardSeconds(5))))
        .apply("RecordWriteErrors", TextIO.write().to(options.getWriteErrorPath())
            .withWindowedWrites().withNumShards(1));

    pipeline.run();
  }

  private static String readMapping(String path) throws IOException {
    if (Strings.isNullOrEmpty(path)) {
      throw new IllegalArgumentException("Mapping configuration path cannot be null or empty.");
    }
    GcsPath gcsPath = GcsUtils.parseGcsPath(path);
    byte[] content;
    if (gcsPath == null) {
      content = Files.readAllBytes(Paths.get(path));
    } else {
      Blob blob = GcsUtils.readFile(gcsPath);
      content = blob.getContent();
    }
    return new String(content, Charset.forName("UTF-8"));
  }
}
