# HL7v2 to FHIR Pipeline

This directory contains a reference Cloud Dataflow pipeline to convert HL7v2 messages to FHIR resources. Please note that additional configurations and hardening are required before processing PHI data with this pipeline.

## Prerequisites

* Have a Linux (Ubuntu & Debian preferred) machine ready.
  * Install [GCC compiler](https://gcc.gnu.org/install/).
  * Install [Go tools](https://golang.org/doc/install), versions >= [1.14](https://golang.org/dl/) are recommended.
  * Install [Gradle](https://gradle.org/install/), version [6.3.0](https://gradle.org/next-steps/?version=6.3&format=bin) is recommended.
  * Install [Protoc](https://github.com/protocolbuffers/protobuf/releases), version [3.14.0](https://github.com/protocolbuffers/protobuf/releases) is recommended.
* Add your public key to [GitHub](https://help.github.com/en/github/authenticating-to-github/adding-a-new-ssh-key-to-your-github-account).
* Install the latest [GCloud SDK](https://cloud.google.com/sdk/install).
* Create a [project](https://cloud.google.com/resource-manager/docs/creating-managing-projects).
* Create an [HL7v2 Store](https://cloud.google.com/healthcare/docs/how-tos/hl7v2).
  * Make sure to use beta endpoints and provide `NotificationConfig`s and a `ParserConfig`.
* Create a [FHIR Store](https://cloud.google.com/healthcare/docs/how-tos/fhir).
* Enable [Cloud Dataflow API](https://cloud.google.com/endpoints/docs/openapi/enable-api).
* (Highly recommended) Enable [audit logging](https://cloud.google.com/logging/docs/audit).

### Permissions

Make sure you have [enough permissions](https://cloud.google.com/dataflow/docs/concepts/access-control#creating_jobs) to run Cloud Dataflow jobs.

The [Cloud Dataflow Controller Service Account](https://cloud.google.com/dataflow/docs/concepts/security-and-permissions#controller_service_account) needs the following permissions.

* `roles/pubsub.subscriber`.
  * To listen for PubSub notifications from new messages. The service account only needs the role on the specific PubSub subscription.
* `roles/healthcare.hl7V2Consumer`.
  * To access messages in your HL7v2 store. The service account only needs the role on the source HL7v2 Store.
* `roles/healthcare.fhirResourceEditor`.
  * To write transformed resources to your FHIR store. The service account only needs this role on the target FHIR Store.
* `roles/storage.objectAdmin`.
  * To access mapping and harmonization configurations on GCS. The service account needs this role on all GCS buckets that the mappings reside in.

## How to Run

Build a fat JAR of the pipeline by running the following from the project directory.

* Please make sure gradle is added to PATH before running the following commands.

```bash
# Generate wrapper classes.
gradle wrapper --gradle-version 6.7.1
./gradlew shadowJar
```

A JAR file should be generated in `build/libs` folder.

Now run the pipeline with the following command:

```bash
# Please set the environment variables in the following command.

java -jar build/libs/converter-0.1.0-all.jar --pubSubSubscription="projects/${PROJECT}/subscriptions/${SUBSCRIPTION}" \
                                             --readErrorPath="gs://${ERROR_BUCKET}/read/read_error.txt" \
                                             --writeErrorPath="gs://${ERROR_BUCKET}/write/write_error.txt" \
                                             --mappingErrorPath="gs://${ERROR_BUCKET}/mapping/mapping_error.txt" \
                                             --mappingPath="gs://${MAPPING_BUCKET}/mapping.textproto" \
                                             --fhirStore="projects/${PROJECT}/locations/${LOCATION}/datasets/${DATASET}/fhirStores/${FHIRSTORE}" \
                                             --runner=DataflowRunner \
                                             --project=${PROJECT}
```

A few notes:

- By default, streaming pipelines do not have autoscaling enabled, please use
either `--enableStreamingEngine` (recommended) or a combination of `--autoscalingAlgorithm=THROUGHPUT_BASED` and
`--maxNumWorkers=N` to manually enable it. See [this page](https://cloud.google.com/dataflow/docs/guides/deploying-a-pipeline#autotuning-features) for more details.
- For production use, we recommend enabling agent metrics by appending `--experiments=enable_stackdriver_agent_metrics` as an option (you will need to grant `roles/monitoring.metricWriter` to Dataflow controller service account as well), see [this page](https://cloud.google.com/dataflow/docs/guides/using-cloud-monitoring#receive_worker_vm_metrics_from_monitoring_agent) for more details. Additionally, we **highly** recommend limiting the number of threads on each worker, e.g. `--numberOfWorkerHarnessThreads=10`. You can tune the limit based on your workload.
- To generate a template instead of running the pipeline, add `--stagingLocation=gs://${STAGING_LOCATION} --templateLocation=gs://${TEMPLATE_LOCATION}` to the above command. See [here](https://cloud.google.com/dataflow/docs/guides/templates/creating-templates)

Please take a look at the `PipelineRunner` class to see the concrete meaning of
each argument.

You should be able to verify that a Dataflow pipeline is running from the cloud
console UI. Data should start flowing through the pipeline and arrive at the
FHIR Store, use the SearchResources API to verify that FHIR Resources are
written correctly.

# DICOM to FHIR Pipeline

This directory contains a reference Cloud Dataflow pipeline to convert a DICOM Study to a FHIR ImagingStudy resource.

## Prerequisites

* Have a Linux (Ubuntu & Debian preferred) machine ready.
  * Install [GCC compiler](https://gcc.gnu.org/install/).
  * Install [Go tools](https://golang.org/doc/install), versions >= [1.14](https://golang.org/dl/) are recommended.
  * Install [Gradle](https://gradle.org/install/), version [6.3.0](https://gradle.org/next-steps/?version=6.3&format=bin) is recommended.
  * Install [Protoc](https://github.com/protocolbuffers/protobuf/releases), version [3.14.0](https://github.com/protocolbuffers/protobuf/releases) is recommended.
* Add your public key to [GitHub](https://help.github.com/en/github/authenticating-to-github/adding-a-new-ssh-key-to-your-github-account).
* Install the latest [GCloud SDK](https://cloud.google.com/sdk/install).
* Create a [project](https://cloud.google.com/resource-manager/docs/creating-managing-projects).
* Create a [DICOM Store](https://cloud.google.com/healthcare/docs/how-tos/dicom).
* Create a [FHIR Store](https://cloud.google.com/healthcare/docs/how-tos/fhir).
  * Set enableUpdateCreate for the FHIR store (https://cloud.google.com/healthcare/docs/reference/rest/v1/projects.locations.datasets.fhirStores#FhirStore.FIELDS.enable_update_create)
* Enable [Cloud Dataflow API](https://cloud.google.com/endpoints/docs/openapi/enable-api).

### Permissions

Make sure you have [enough permissions](https://cloud.google.com/dataflow/docs/concepts/access-control#creating_jobs) to run Cloud Dataflow jobs.

The [Cloud Dataflow Controller Service Account](https://cloud.google.com/dataflow/docs/concepts/security-and-permissions#controller_service_account) needs the following permissions.

* `roles/pubsub.subscriber`.
  * To listen for PubSub notifications from new messages. The service account only needs the role on the specific PubSub subscription.
* `roles/healthcare.dicomEditor`.
  * To access metadata of DICOM stores.
* `roles/healthcare.fhirResourceEditor`.
  * To write transformed resources to your FHIR store. The service account only needs this role on the target FHIR Store.
* `roles/storage.objectAdmin`.
  * To access mapping and harmonization configurations on GCS. The service account needs this role on all GCS buckets that the mappings reside in.

## How to Run

In build.gradle, change the main class of the build from "Hl7v2ToFhirStreamingRunner" to "DicomToFhirStreamingRunner"

ie.
```
shadowJar {
    mainClassName = project.findProperty('mainClass') ?: 'com.google.cloud.healthcare.etl.runner.dicomtofhir.DicomToFhirStreamingRunner'
}
```

Build a fat JAR of the pipeline by running the following from the project directory.

* Please make sure gradle is added to PATH before running the following commands.

```bash
# Generate wrapper classes.
gradle wrapper
./gradlew shadowJar
```

A JAR file should be generated in `build/libs` folder.

Now run the pipeline with the following command:

```bash
# Please set the environment variables in the following command.

java -jar build/libs/converter-0.1.0-all.jar --pubSubSubscription="projects/${PROJECT}/subscriptions/${SUBSCRIPTION}" \
                                             --readErrorPath="gs://${ERROR_BUCKET}/read/read_error.txt" \
                                             --writeErrorPath="gs://${ERROR_BUCKET}/write/write_error.txt" \
                                             --mappingErrorPath="gs://${ERROR_BUCKET}/mapping/mapping_error.txt" \
                                             --mappingPath="gs://${MAPPING_BUCKET}/main.textproto" \
                                             --fhirStore="projects/${PROJECT}/locations/${LOCATION}/datasets/${DATASET}/fhirStores/${FHIRSTORE}" \
                                             --runner=DataflowRunner \
                                             --project=${PROJECT}
```

A few notes:

- By default, streaming pipelines do not have autoscaling enabled, please use
either `--enableStreamingEngine` (recommended) or a combination of `--autoscalingAlgorithm=THROUGHPUT_BASED` and
`--maxNumWorkers=N` to manually enable it. See [this page](https://cloud.google.com/dataflow/docs/guides/deploying-a-pipeline#autotuning-features) for more details.
- For production use, we recommend enabling agent metrics by appending `--experiments=enable_stackdriver_agent_metrics` as an option (you will need to grant `roles/monitoring.metricWriter` to Dataflow controller service account as well), see [this page](https://cloud.google.com/dataflow/docs/guides/using-cloud-monitoring#receive_worker_vm_metrics_from_monitoring_agent) for more details. Additionally, we **highly** recommend limiting the number of threads on each worker, e.g. `--numberOfWorkerHarnessThreads=10`. You can tune the limit based on your workload.
- To generate a template instead of running the pipeline, add `--stagingLocation=gs://${STAGING_LOCATION} --templateLocation=gs://${TEMPLATE_LOCATION}` to the above command. See [here](https://cloud.google.com/dataflow/docs/guides/templates/creating-templates)
- The mappingPath file (main.textproto) configures the mapping library. Ensure that the paths inside the file exist (References the following repository: https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/). The required binaries should be installed by the build JAR command.


Please take a look at the `PipelineRunner` class to see the concrete meaning of
each argument.

You should be able to verify that a Dataflow pipeline is running from the cloud
console UI. Data should start flowing through the pipeline and arrive at the
FHIR Store, use the SearchResources API to verify that FHIR Resources are
written correctly.


## Support

Please file GitHub issues if you encounter any problems.
