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
package org.apache.beam.sdk.io.gcp.healthcare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import com.google.cloud.healthcare.etl.model.mapping.MappingOutput;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.io.gcp.healthcare.HttpHealthcareApiClient.HealthcareHttpException;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PInput;
import org.apache.beam.sdk.values.POutput;
import org.apache.beam.sdk.values.PValue;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link FhirIOWithMetrics} is almost the same as the upstream {@link FhirIO} but added some
 * Dataflow metrics for performance evaluation.
 */
public class FhirIOWithMetrics {

  /** The type Write. */
  @AutoValue
  public abstract static class Write extends PTransform<PCollection<MappingOutput>, Write.Result> {

    /** The tag for the failed writes to FHIR store`. */
    public static final TupleTag<HealthcareIOError<String>> FAILED_BODY =
        new TupleTag<HealthcareIOError<String>>() {};
    /** The tag for the files that failed to FHIR store`. */
    public static final TupleTag<HealthcareIOError<String>> FAILED_FILES =
        new TupleTag<HealthcareIOError<String>>() {};
    /** The tag for temp files for import to FHIR store`. */
    public static final TupleTag<ResourceId> TEMP_FILES = new TupleTag<ResourceId>() {};

    /** The enum Write method. */
    public enum WriteMethod {
      /**
       * Execute Bundle Method executes a batch of requests as a single transaction @see <a
       * href=https://cloud.google.com/healthcare/docs/reference/rest/v1/projects.locations.datasets.fhirStores.fhir/executeBundle></a>.
       */
      EXECUTE_BUNDLE,
    }

    /** The type Result. */
    public static class Result implements POutput {
      private final Pipeline pipeline;
      private final PCollection<HealthcareIOError<String>> failedBodies;
      private final PCollection<HealthcareIOError<String>> failedFiles;

      /**
       * Creates a {@link FhirIOWithMetrics.Write.Result} in the given {@link Pipeline}. @param
       * pipeline the pipeline
       *
       * @param failedBodies the failed inserts
       * @return the result
       */
      static Result in(Pipeline pipeline, PCollection<HealthcareIOError<String>> failedBodies) {
        return new Result(pipeline, failedBodies, null);
      }

      static Result in(
          Pipeline pipeline,
          PCollection<HealthcareIOError<String>> failedBodies,
          PCollection<HealthcareIOError<String>> failedFiles) {
        return new Result(pipeline, failedBodies, failedFiles);
      }

      /**
       * Gets failed bodies with err.
       *
       * @return the failed inserts with err
       */
      public PCollection<HealthcareIOError<String>> getFailedBodies() {
        return this.failedBodies;
      }

      /**
       * Gets failed file imports with err.
       *
       * @return the failed GCS uri with err
       */
      public PCollection<HealthcareIOError<String>> getFailedFiles() {
        return this.failedFiles;
      }

      @Override
      public Pipeline getPipeline() {
        return this.pipeline;
      }

      @Override
      public Map<TupleTag<?>, PValue> expand() {
        return ImmutableMap.of(Write.FAILED_BODY, failedBodies, Write.FAILED_FILES, failedFiles);
      }

      @Override
      public void finishSpecifyingOutput(
          String transformName, PInput input, PTransform<?, ?> transform) {}

      private Result(
          Pipeline pipeline,
          PCollection<HealthcareIOError<String>> failedBodies,
          @Nullable PCollection<HealthcareIOError<String>> failedFiles) {
        this.pipeline = pipeline;
        this.failedBodies = failedBodies;
        if (failedFiles == null) {
          failedFiles =
              (PCollection<HealthcareIOError<String>>)
                  pipeline.apply(Create.empty(HealthcareIOErrorCoder.of(StringUtf8Coder.of())));
        }
        this.failedFiles = failedFiles;
      }
    }

    /**
     * Gets Fhir store.
     *
     * @return the Fhir store
     */
    abstract ValueProvider<String> getFhirStore();

    /**
     * Gets write method.
     *
     * @return the write method
     */
    abstract WriteMethod getWriteMethod();

    /** The type Builder. */
    @AutoValue.Builder
    abstract static class Builder {

      /**
       * Sets Fhir store.
       *
       * @param fhirStore the Fhir store
       * @return the Fhir store
       */
      abstract Builder setFhirStore(ValueProvider<String> fhirStore);

      /**
       * Sets write method.
       *
       * @param writeMethod the write method
       * @return the write method
       */
      abstract FhirIOWithMetrics.Write.Builder setWriteMethod(
          FhirIOWithMetrics.Write.WriteMethod writeMethod);

      /**
       * Build write.
       *
       * @return the write
       */
      abstract Write build();
    }

    private static Write.Builder write(String fhirStore) {
      return new AutoValue_FhirIOWithMetrics_Write.Builder()
          .setFhirStore(StaticValueProvider.of(fhirStore));
    }

    /**
     * Execute Bundle Method executes a batch of requests as a single transaction @see <a
     * href=https://cloud.google.com/healthcare/docs/reference/rest/v1/projects.locations.datasets.fhirStores.fhir/executeBundle></a>.
     *
     * @param fhirStore the hl 7 v 2 store
     * @return the write
     */
    public static Write executeBundles(String fhirStore) {
      return new AutoValue_FhirIOWithMetrics_Write.Builder()
          .setFhirStore(StaticValueProvider.of(fhirStore))
          .setWriteMethod(WriteMethod.EXECUTE_BUNDLE)
          .build();
    }

    /**
     * Execute bundles write.
     *
     * @param fhirStore the fhir store
     * @return the write
     */
    public static Write executeBundles(ValueProvider<String> fhirStore) {
      return new AutoValue_FhirIOWithMetrics_Write.Builder()
          .setFhirStore(fhirStore)
          .setWriteMethod(WriteMethod.EXECUTE_BUNDLE)
          .build();
    }

    private static final Logger LOG = LoggerFactory.getLogger(Write.class);

    @Override
    public Result expand(PCollection<MappingOutput> input) {
      PCollection<HealthcareIOError<String>> failedBundles;
      switch (this.getWriteMethod()) {
        case EXECUTE_BUNDLE:
        default:
          failedBundles =
              input
                  .apply(
                      "Execute FHIR Bundles",
                      ParDo.of(new ExecuteBundles.ExecuteBundlesFn(this.getFhirStore())))
                  .setCoder(HealthcareIOErrorCoder.of(StringUtf8Coder.of()));
      }
      return Result.in(input.getPipeline(), failedBundles);
    }
  }

  /** The type Execute bundles. */
  public static class ExecuteBundles extends PTransform<PCollection<MappingOutput>, Write.Result> {
    private final ValueProvider<String> fhirStore;

    /**
     * Instantiates a new Execute bundles.
     *
     * @param fhirStore the fhir store
     */
    ExecuteBundles(ValueProvider<String> fhirStore) {
      this.fhirStore = fhirStore;
    }

    /**
     * Instantiates a new Execute bundles.
     *
     * @param fhirStore the fhir store
     */
    ExecuteBundles(String fhirStore) {
      this.fhirStore = StaticValueProvider.of(fhirStore);
    }

    @Override
    public FhirIOWithMetrics.Write.Result expand(PCollection<MappingOutput> input) {
      return Write.Result.in(
          input.getPipeline(),
          input
              .apply(ParDo.of(new ExecuteBundlesFn(fhirStore)))
              .setCoder(HealthcareIOErrorCoder.of(StringUtf8Coder.of())));
    }

    /** The type Write Fhir fn. */
    static class ExecuteBundlesFn extends DoFn<MappingOutput, HealthcareIOError<String>> {

      private final Counter failedBundles =
          Metrics.counter(ExecuteBundlesFn.class, "failed-bundles");
      private final Distribution latency = Metrics.distribution(ExecuteBundles.class, "latency");
      private transient HealthcareApiClient client;
      private final ObjectMapper mapper = new ObjectMapper();
      /** The Fhir store. */
      private final ValueProvider<String> fhirStore;

      /**
       * Instantiates a new Write Fhir fn.
       *
       * @param fhirStore the Fhir store
       */
      ExecuteBundlesFn(ValueProvider<String> fhirStore) {
        this.fhirStore = fhirStore;
      }

      /**
       * Initialize healthcare client.
       *
       * @throws IOException the io exception
       */
      @Setup
      public void initClient() throws IOException {
        this.client = new HttpHealthcareApiClient();
      }

      /**
       * Execute Bundles.
       *
       * @param context the context
       */
      @ProcessElement
      public void executeBundles(ProcessContext context) {
        MappingOutput message = context.element();
        String body = message.getOutput();
        try {
          // Validate that data was set to valid JSON.
          mapper.readTree(body);
          client.executeFhirBundle(fhirStore.get(), body);

          long finishTime = Instant.now().toEpochMilli();
          long createTime = message.getSourceTime().get().getMillis();
          latency.update(finishTime - createTime);
        } catch (IOException | HealthcareHttpException e) {
          failedBundles.inc();
          context.output(HealthcareIOError.of(body, e));
        }
      }
    }
  }
}
