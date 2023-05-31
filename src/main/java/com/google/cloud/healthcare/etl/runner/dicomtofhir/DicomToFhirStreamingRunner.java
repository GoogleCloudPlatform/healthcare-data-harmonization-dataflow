// Copyright 2021 Google LLC.
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
package com.google.cloud.healthcare.etl.runner.dicomtofhir;

import com.google.cloud.healthcare.etl.model.ErrorEntry;
import com.google.cloud.healthcare.etl.model.converter.ErrorEntryConverter;
import com.google.cloud.healthcare.etl.model.mapping.HclsApiDicomMappableMessage;
import com.google.cloud.healthcare.etl.model.mapping.HclsApiDicomMappableMessageCoder;
import com.google.cloud.healthcare.etl.model.mapping.MappedFhirMessageWithSourceTimeCoder;
import com.google.cloud.healthcare.etl.model.mapping.MappingOutput;
import com.google.cloud.healthcare.etl.pipeline.MappingFn;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.healthcare.DicomIO;
import org.apache.beam.sdk.io.gcp.healthcare.FhirIO;
import org.apache.beam.sdk.io.gcp.healthcare.HealthcareIOError;
import org.apache.beam.sdk.io.gcp.healthcare.HealthcareIOErrorToTableRow;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Repeatedly;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.joda.time.Duration;

/**
 * The entry point of the pipeline. It will be triggered upon receiving a PubSub message from the
 * given subscription. From the message, it will extract the webpath of a DICOM instance and collect
 * it's metadata from DICOM IO. With the response, it will map the study metadata to a FHIR
 * ImagingStudyResource and upload it to the given FHIR store.
 *
 * <p>The errors for each component are handled separately, e.g. you can specify file paths for each
 * stage (read - DICOM IO, mapping, write - FHIR IO).
 */
public class DicomToFhirStreamingRunner {
  private static Duration ERROR_LOG_WINDOW_SIZE = Duration.standardSeconds(5);

  /** Pipeline options. */
  public interface Options extends PipelineOptions {
    @Description(
        "The PubSub subscription to listen to, must be of the full format: "
            + "projects/project_id/subscriptions/subscription_id")
    @Required
    String getPubSubSubscription();

    void setPubSubSubscription(String param1String);

    @Description(
        "The path to the mapping configurations. The path will be treated as a GCS path if the "
            + "path starts with the GCS scheme (\"gs\"), otherwise a local file. Please see: "
            + "https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/"
            + "baa4e0c7849413f7b44505a8410ee7f52745427a/mapping_configs/README.md "
            + "for more details on the mapping configuration structure.")
    @Required
    String getMappingPath();

    void setMappingPath(String param1String);

    @Description(
        "The target FHIR Store to write data to, must be of the full format: "
            + "projects/project_id/locations/location/datasets/dataset_id/fhirStores/fhir_store_id")
    @Required
    String getFhirStore();

    void setFhirStore(String param1String);

    @Description(
        "The path that is used to record all read errors. The path will be treated "
            + "as a GCS path if the path starts with the GCS scheme (\"gs\"), otherwise a local"
            + " file.")
    @Required
    String getReadErrorPath();

    void setReadErrorPath(String readErrorPath);

    @Description(
        "The path that is used to record all write errors. The path will be "
            + "treated as a GCS path if the path starts with the GCS scheme (\"gs\"), otherwise a "
            + "local file.")
    @Required
    String getWriteErrorPath();

    void setWriteErrorPath(String writeErrorPath);

    @Description(
        "The path that is used to record all mapping errors. The path will be "
            + "treated as a GCS path if the path starts with the GCS scheme (\"gs\"), otherwise a "
            + "local file.")
    @Required
    String getMappingErrorPath();

    void setMappingErrorPath(String mappingErrorPath);

    @Description("The number of shards when writing errors to GCS.")
    @Default.Integer(1)
    Integer getErrorLogShardNum();

    void setErrorLogShardNum(Integer shardNum);
  }

  /** A DoFn to collect the webpath of the DICOM instance from the PubSub Message. */
  static class ExtractWebpathFromPubsub extends DoFn<PubsubMessage, String> {
    @ProcessElement
    public void processElement(DoFn<PubsubMessage, String>.ProcessContext context)
        throws UnsupportedEncodingException {
      PubsubMessage msg = context.element();
      String webpath = new String(msg.getPayload(), StandardCharsets.UTF_8);
      context.output(webpath);
    }
  }

  /**
   * A DoFn that will take the response of the Study Metadata Read call from the DICOM API and
   * reformat it to be consumed by the mapping library.
   */
  static class CreateMappingFnInput extends DoFn<String, String> {
    public static final Gson gson = new Gson();

    @ProcessElement
    public void processElement(DoFn<String, String>.ProcessContext context) {
      String dicomMetadataResponse = context.element();
      JsonArray jsonArray = gson.fromJson(dicomMetadataResponse, JsonArray.class);
      JsonObject jsonObject = new JsonObject();
      jsonObject.add("study", jsonArray);
      context.output(gson.toJson(jsonObject));
    }
  }

  /**
   * A DoFn that will take the response of the mapping library (a FHIR ImagingStudy resource as a
   * json string) and wrap it into a FHIR bundle to be written to the FHIR store. See
   * https://cloud.google.com/healthcare/docs/how-tos/fhir-bundles#executing_a_bundle
   * TODO(b/174594428): Add a unique ID for each ImagingStudy FHIR resource in the mappings, and use
   * a PUT instead. This will prevent creation of a new FHIR resource for every DICOM instance in an
   * ImagingStudy.
   */
  static class CreateFhirResourceBundle extends DoFn<String, String> {
    private static final String POST_HTTP_METHOD = "POST";
    private static final String IMAGING_STUDY_TYPE = "ImagingStudy";

    @ProcessElement
    public void processElement(DoFn<String, String>.ProcessContext context) {
      String mappingOutputString = context.element();

      Gson gson = new Gson();
      JsonObject mappingFhirResource = gson.fromJson(mappingOutputString, JsonObject.class);
      JsonObject requestObj = new JsonObject();

      requestObj.addProperty("method", POST_HTTP_METHOD);
      requestObj.addProperty("url", IMAGING_STUDY_TYPE);
      JsonObject entryObj = new JsonObject();
      entryObj.add("resource", mappingFhirResource);
      entryObj.add("request", requestObj);
      JsonArray entries = new JsonArray();
      entries.add(entryObj);
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("resourceType", "Bundle");
      jsonObject.addProperty("type", "transaction");
      jsonObject.add("entry", entries);
      context.output(gson.toJson(jsonObject));
    }
  }

  /**
   * Read the study metadata of the instance given in the body of the PubSub Message.
   *
   * @param pubsubMsg The PubSub message containing the webpath of the instance to be mapped.
   * @param options The pipeline configuration.
   * @return A PCollection of Strings containing successful read operations.
   */
  private PCollection<String> readDicomStudyMetadata(
      PCollection<PubsubMessage> pubsubMsg, Options options) {
    DicomIO.ReadStudyMetadata.Result dicomResult =
        pubsubMsg
            .apply(ParDo.of(new ExtractWebpathFromPubsub()))
            .apply(DicomIO.readStudyMetadata());

    PCollection<String> successfulReads = dicomResult.getReadResponse();
    PCollection<String> failedReads = dicomResult.getFailedReads();

    HealthcareIOErrorToTableRow<String> errorConverter = new HealthcareIOErrorToTableRow<>();
    failedReads
        .apply(
            Window.<String>into(FixedWindows.of(ERROR_LOG_WINDOW_SIZE))
                .triggering(
                    Repeatedly.forever(
                        AfterProcessingTime.pastFirstElementInPane()
                            .plusDelayOf(ERROR_LOG_WINDOW_SIZE)))
                .withAllowedLateness(Duration.ZERO)
                .discardingFiredPanes())
        .apply(
            "WriteReadErrors",
            TextIO.write()
                .to(options.getReadErrorPath())
                .withWindowedWrites()
                .withNumShards(options.getErrorLogShardNum()));

    return successfulReads;
  }

  /**
   * Map the given study to a FHIR ImagingStudy resource.
   *
   * @param studyMetadata A PCollection of Strings containing successful read operations.
   * @param options The pipeline configuration.
   * @return A PCollection of String containing successfully mapped FHIR resources.
   */
  private PCollection<String> mapDicomStudyMetadataToFhirResource(
      PCollection<String> studyMetadata, Options options) {
    // TODO(b/176925046): support performance metrics.
    MappingFn<HclsApiDicomMappableMessage> mappingFn =
        MappingFn.of(options.getMappingPath(), false);

    PCollectionTuple mapDicomStudyToFhirBundleRequest =
        studyMetadata
            .apply(ParDo.of(new CreateMappingFnInput()))
            .apply(
                MapElements.into(TypeDescriptor.of(HclsApiDicomMappableMessage.class))
                    .via(HclsApiDicomMappableMessage::from))
            .setCoder(HclsApiDicomMappableMessageCoder.of())
            .apply(
                "MapMessages",
                ParDo.of(mappingFn)
                    .withOutputTags(
                        MappingFn.MAPPING_TAG, TupleTagList.of(ErrorEntry.ERROR_ENTRY_TAG)));

    PCollection<ErrorEntry> mappingError =
        mapDicomStudyToFhirBundleRequest.get(ErrorEntry.ERROR_ENTRY_TAG);
    mappingError
        .apply(
            "SerializeMappingErrors",
            MapElements.into(TypeDescriptors.strings())
                .via(e -> ErrorEntryConverter.toTableRow(e).toString()))
        .apply(
            Window.<String>into(FixedWindows.of(ERROR_LOG_WINDOW_SIZE))
                .triggering(
                    Repeatedly.forever(
                        AfterProcessingTime.pastFirstElementInPane()
                            .plusDelayOf(ERROR_LOG_WINDOW_SIZE)))
                .withAllowedLateness(Duration.ZERO)
                .discardingFiredPanes())
        .apply(
            "ReportMappingErrors",
            TextIO.write()
                .to(options.getMappingErrorPath())
                .withWindowedWrites()
                .withNumShards(options.getErrorLogShardNum()));

    return mapDicomStudyToFhirBundleRequest
        .get(MappingFn.MAPPING_TAG)
        .setCoder(MappedFhirMessageWithSourceTimeCoder.of())
        .apply(MapElements.into(TypeDescriptors.strings()).via(MappingOutput::getOutput));
  }

  /**
   * Write the mapped FHIR resources to the given FHIR store.
   *
   * @param fhirResource A PCollection of String containing successfully mapped FHIR resources.
   * @param options The pipeline configuration.
   */
  private void writeToFhirStore(PCollection<String> fhirResource, Options options) {
    FhirIO.Write.AbstractResult writeResults =
        fhirResource
            .apply(ParDo.of(new CreateFhirResourceBundle()))
            .apply("WriteFHIRBundles", FhirIO.Write.executeBundles(options.getFhirStore()));

    PCollection<HealthcareIOError<String>> failedWrites = writeResults.getFailedBodies();

    HealthcareIOErrorToTableRow<String> bundleErrorConverter = new HealthcareIOErrorToTableRow<>();
    failedWrites
        .apply(
            "ConvertBundleErrors",
            MapElements.into(TypeDescriptors.strings())
                .via(resp -> bundleErrorConverter.apply(resp).toString()))
        .apply(
            Window.<String>into(FixedWindows.of(ERROR_LOG_WINDOW_SIZE))
                .triggering(
                    Repeatedly.forever(
                        AfterProcessingTime.pastFirstElementInPane()
                            .plusDelayOf(ERROR_LOG_WINDOW_SIZE)))
                .withAllowedLateness(Duration.ZERO)
                .discardingFiredPanes())
        .apply(
            "RecordWriteErrors",
            TextIO.write()
                .to(options.getWriteErrorPath())
                .withWindowedWrites()
                .withNumShards(options.getErrorLogShardNum()));
  }

  public static void main(String[] args) {
    DicomToFhirStreamingRunner runner = new DicomToFhirStreamingRunner();
    Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
    Pipeline p = Pipeline.create(options);

    PCollection<PubsubMessage> newPubsubMsgCollection =
        p.apply(PubsubIO.readMessages().fromSubscription(options.getPubSubSubscription()));

    PCollection<String> studyMetadata =
        runner.readDicomStudyMetadata(newPubsubMsgCollection, options);
    PCollection<String> fhirResource =
        runner.mapDicomStudyMetadataToFhirResource(studyMetadata, options);
    runner.writeToFhirStore(fhirResource, options);

    p.run();
  }
}
