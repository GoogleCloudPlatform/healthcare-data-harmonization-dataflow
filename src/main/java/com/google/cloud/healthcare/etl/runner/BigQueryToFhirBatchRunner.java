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
package com.google.cloud.healthcare.etl.runner;

import static com.google.cloud.healthcare.etl.model.ErrorEntry.ERROR_ENTRY_TAG;
import static com.google.cloud.healthcare.etl.pipeline.MappingFn.MAPPING_TAG;

import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.healthcare.etl.model.QueryOptions;
import com.google.cloud.healthcare.etl.pipeline.MappingFn;
import com.google.cloud.healthcare.etl.pipeline.TableRowToJsonFn;
import com.google.common.base.Strings;
import java.util.Map;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.healthcare.FhirIO;
import org.apache.beam.sdk.io.gcp.healthcare.HealthcareIOErrorToTableRow;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptors;

/** Entry point for a batch pipeline converting BigQuery messages to FHIR resources. */
public class BigQueryToFhirBatchRunner {
  /** Pipeline options. */
  public interface Options extends DataflowPipelineOptions {
    @Description(
        "The BigQuery queries to use as input to the pipeline, in JSON format. "
            + "In the format of \"{ \"queries\": { \"Patient\": "
            + "\"SELECT * FROM `test-project.test-dataset.test-table`\"}}")
    @Required
    QueryOptions getSourceQueries();

    void setSourceQueries(QueryOptions sourceQueries);

    @Description(
        "The path to the mapping configurations. The path will be treated as a GCS path if the"
            + " path starts with the GCS scheme (\"gs\"), otherwise a local file. Please see: "
            + "https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/baa4e0c7849413f7b44505a8410ee7f52745427a/mapping_configs/README.md"
            + " for more details on the mapping configuration structure.")
    @Required
    String getMappingPath();

    void setMappingPath(String gcsPath);

    @Description(
        "The target FHIR Store to write data to, must be of the full format: "
            + "projects/project_id/locations/location/datasets/dataset_id/fhirStores/fhir_store_id")
    @Required
    ValueProvider<String> getFhirStore();

    void setFhirStore(ValueProvider<String> fhirStore);

    @Description(
        "The path that is used to record all mapping errors. The path will be "
            + "treated as a GCS path if the path starts with the GCS scheme (\"gs\"), otherwise a "
            + "local file.")
    @Required
    ValueProvider<String> getMappingErrorPath();

    void setMappingErrorPath(ValueProvider<String> mappingErrorPath);

    @Description(
        "The path that is used to record all FHIR executeBundle errors. The path will be "
            + "treated as a GCS path if the path starts with the GCS scheme (\"gs\"), otherwise a "
            + "local file.")
    @Required
    ValueProvider<String> getWriteErrorPath();

    void setWriteErrorPath(ValueProvider<String> writeErrorPath);
  }

  public static void main(String[] args) {
    Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
    Pipeline pipeline = Pipeline.create(options);

    // Validation
    validateOptions(options);

    MappingFn mappingFn = MappingFn.of(options.getMappingPath());
    for (Map.Entry<String, String> entry : options.getSourceQueries().queries().entrySet()) {
      String query = entry.getValue();
      String tableName = entry.getKey();
      PCollection<TableRow> bqRows =
          pipeline.apply(
              getStepName(tableName, "BigQueryRead"),
              BigQueryIO.readTableRows()
                  .fromQuery(query)
                  .usingStandardSql()
                  .withTemplateCompatibility());
      PCollectionTuple mappingResults =
          bqRows
              .apply(
                  MapElements.into(TypeDescriptors.strings()).via(new TableRowToJsonFn(tableName)))
              .apply(
                  getStepName(tableName, "MapMessages"),
                  ParDo.of(mappingFn)
                      .withOutputTags(MAPPING_TAG, TupleTagList.of(ERROR_ENTRY_TAG)));

      ErrorWriter.writeErrorEntriesToFile(
          mappingResults.get(ERROR_ENTRY_TAG),
          options.getMappingErrorPath(),
          getStepName(tableName, "WriteMappingErrors"));
      // Commit FHIR resources.
      FhirIO.Write.Result writeResult =
          mappingResults
              .get(MAPPING_TAG)
              .apply(
                  getStepName(tableName, "WriteFHIRBundles"),
                  FhirIO.Write.executeBundles(options.getFhirStore()));

      HealthcareIOErrorToTableRow<String> bundleErrorConverter =
          new HealthcareIOErrorToTableRow<>();
      writeResult
          .getFailedBodies()
          .apply(
              getStepName(tableName, "ConvertBundleErrors"),
              MapElements.into(TypeDescriptors.strings())
                  .via(resp -> bundleErrorConverter.apply(resp).toString()))
          .apply(
              getStepName(tableName, "RecordWriteErrors"),
              TextIO.write().to(options.getWriteErrorPath()));
    }
    pipeline.run();
  }

  private static String getStepName(String tableName, String s) {
    return s + " " + tableName;
  }

  private static void validateOptions(Options options) {
    QueryOptions queryOptions = options.getSourceQueries();
    if (queryOptions.queries() == null || queryOptions.queries().size() == 0) {
      throw new IllegalArgumentException("Must specify one or more queries as input.");
    }
    for (Map.Entry<String, String> entry : queryOptions.queries().entrySet()) {
      if (Strings.isNullOrEmpty(entry.getKey()) || Strings.isNullOrEmpty(entry.getValue())) {
        throw new IllegalArgumentException("Table name and query must not be empty.");
      }
    }
  }
}
