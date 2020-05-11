# HL7v2 to FHIR Pipeline

This directory contains a reference Cloud Dataflow pipeline to convert HL7v2 messages to FHIR resources. Please note that additional configurations and hardening are required before processing PHI data with this pipeline.

## Prerequisites

* Have a Linux (Ubuntu & Debian preferred) machine ready.
  * Install [GCC compiler](https://gcc.gnu.org/install/).
  * Install [Go tools](https://golang.org/doc/install).
  * Install [Gradle](https://gradle.org/install/).
* Add your public key to [GitHub](https://help.github.com/en/github/authenticating-to-github/adding-a-new-ssh-key-to-your-github-account).
* Install the [GCloud SDK](https://cloud.google.com/sdk/install).
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
* `roles/storage.objectViewer`.
  * To access mapping and harmonization configurations on GCS. The service account needs this role on all GCS buckets that the mappings reside in.

## How to Run

The pipeline depends on the [mapping engine](https://github.com/GoogleCloudPlatform/healthcare-data-harmonization) to process the data. Please build the shared object by running the following command:

```bash
build_deps.sh --work_dir /tmp/work --output_dir `pwd`/lib
```

A shared object (.so) file gets generated in the `lib` directory after the command finishes.

Next we will build a fat JAR of the pipeline, the purpose is to properly include the shared object so that the Cloud Dataflow runner correctly recognizes it.

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
                                             --readErrorPath="gs://${BUCKET}/read_error.txt" \
                                             --writeErrorPath="gs://${BUCKET}/write_error.txt" \
                                             --mappingErrorPath="gs://${BUCKET}/mapping_error.txt" \
                                             --mappingPath="gs://${BUCKET}/mapping.textproto" \
                                             --fhirStore="projects/${PROJECT}/locations/${LOCATION}/datasets/${DATASET}/fhirStores/${FHIRSTORE}" \
                                             --runner=DataflowRunner \
                                             --project=${PROJECT}
```

NOTE: By default, streaming pipelines do not have autoscaling enabled, please use
either `--streamingEngine` (recommended) or a combination of `--autoscalingAlgorithm=THROUGHPUT_BASED` and
`--maxNumWorkers=N` to manually enable it. See [this page](https://cloud.google.com/dataflow/docs/guides/deploying-a-pipeline#autotuning-features) for more details.

Please take a look at the `PipelineRunner` class to see the concrete meaning of
each argument.

You should be able to verify that a Dataflow pipeline is running from the cloud
console UI. Data should start flowing through the pipeline and arrive at the
FHIR Store, use the SearchResources API to verify that FHIR Resources are
written correctly.

## Support

Please file GitHub issues if you encounter any problems.
