# HL7v2 to FHIR Pipeline

This directory contains a reference Cloud Dataflow pipeline to convert HL7v2 messages to FHIR resources. Please note that additional configurations and hardening are required before processing PHI data with this pipeline.

## Prerequisites

### (Option 1) Docker

It is recommended that you install [Docker Desktop](https://docs.docker.com/engine/install/) in order to build the image and run the container.

#### Build Docker Image

- Clone the repository locally.
- From the project root, you can build the image using any custom tag name.

```
docker build -t [YOUR_PROJECT_IMAGE_NAME] .
```

The build will take several minutes to complete.

#### Running the Container Locally

- Start an interactive session and bind mount to allow you to copy a service account key and deploy the pipeline.

```
docker run -it --rm -v "$(pwd)"/key:/var/local/pipeline/key [YOUR_PROJECT_IMAGE_NAME]
```

- The application will need to run using a Service Account. [Follow the instructions](https://cloud.google.com/docs/authentication/production#create_service_account) to create a Service Account and export a JSON key. See the [Permissions](#permissions) section below for the required roles.
- Copy the generated JSON key into the `/key/` folder on your host machine that was created when you started the container. This will automatically copy the key into the container's `/key/` folder as well.
- From within your container's interative session, [follow the instructions](https://cloud.google.com/docs/authentication/production#passing_variable) to make your key available. For example run, `export GOOGLE_APPLICATION_CREDENTIALS="/var/local/pipeline/key/[YOUR_KEY_FILENAME]"`.
- Run the project. See [How to Run](#how-to-run) below. Note: You do not need to run `gradle` or `gradlew` as the binary has already been built and exists under `/build/libs/`.

#### Deploying as a Temporary GKE Pod

You can alternatively deploy the pipeline via a temporary GKE pod.

- [Follow the instructions](https://cloud.google.com/artifact-registry/docs/docker/pushing-and-pulling) for Pushing and pulling images to the Artifact Registry.
- [Login to your project](https://console.cloud.google.com/) and start a [Cloud Shell](https://cloud.google.com/shell/docs/using-cloud-shell). Alternatively, you can also [follow the instructions](https://cloud.google.com/kubernetes-engine/docs/how-to/cluster-access-for-kubectl) for installing and configuring cluster access to GKE.
- [Create an Autopilot cluster](https://cloud.google.com/kubernetes-engine/docs/how-to/creating-an-autopilot-cluster).
- The GKE cluster will need to run using a Service Account. [Follow the instructions](https://cloud.google.com/docs/authentication/production#create_service_account) to create a Service Account. You will not need to export a JSON key. See the [Permissions](#permissions) section below for the required roles.
- Bind your IAM Service Account to the default GKE service account.

```
gcloud container clusters get-credentials [YOUR_CLUSTER_NAME] --region "[YOUR_CLUSTER_REGION]"
```

```
gcloud iam service-accounts add-iam-policy-binding [YOUR_SA_NAME]@[YOUR_GCP_PROJECT_NAME].iam.gserviceaccount.com \
    --role roles/iam.workloadIdentityUser \
    --member "serviceAccount:[YOUR_GCP_PROJECT_NAME].svc.id.goog[default/default]"
```

```
gcloud iam service-accounts add-iam-policy-binding [YOUR_SA_NAME]@[YOUR_GCP_PROJECT_NAME].iam.gserviceaccount.com \
    --role roles/iam.serviceAccountTokenCreator \
    --member "serviceAccount:[YOUR_GCP_PROJECT_NAME].svc.id.goog[default/default]"
```

```
kubectl annotate serviceaccount default \
    --namespace default \
    iam.gke.io/gcp-service-account=[YOUR_SA_NAME]@[YOUR_GCP_PROJECT_NAME].iam.gserviceaccount.com
```

- Create a deployment config named `pipline-deployment.yaml` with the following contents. `YOUR_PIPELINE_PROJECT_NAME` can be anything, but it should be consistent throughout the yaml config.:

```
apiVersion: v1
kind: Pod
metadata:
 labels:
   run: [YOUR_PIPELINE_PROJECT_NAME]
 name: [YOUR_PIPELINE_PROJECT_NAME]
spec:
 containers:
 - command:
   - /usr/bin/java
   - -jar
   - /var/local/pipeline/build/libs/converter-0.1.0-all.jar
   - --pubSubSubscription=projects/[YOUR_GCP_PROJECT_NAME/subscriptions/[YOUR_SUBSCRIPTION]
   - --readErrorPath=gs://[YOUR_STORE_PATH]/error/read/read_error.txt
   - --writeErrorPath=gs://[YOUR_STORE_PATH]/error/write/write_error.txt
   - --mappingErrorPath=gs://[YOUR_STORE_PATH]/error/mapping/mapping_error.txt
   - --mappingPath=gs://[YOUR_STORE_PATH]/mapping/mapping_configs/hl7v2_fhir_r4/configurations/main.textproto
   - --fhirStore=projects/[YOUR_GCP_PROJECT_NAME/locations/[REGION]/datasets/[DATASET_NAME]/fhirStores/[STORE_NAME]
   - --runner=DataflowRunner
   - --project=[YOUR_GCP_PROJECT_NAME]
   - --region=[REGION]
   - --enableStreamingEngine
   - --experiments=enable_stackdriver_agent_metrics
   - --numberOfWorkerHarnessThreads=10
   image: [YOUR_ARTIFACT_REGISTRY_REGION]-docker.pkg.dev/[YOUR_GCP_PROJECT_NAME/[YOUR_REPO]/[YOUR_IMAGE_NAME]:latest
   imagePullPolicy: Always
   name: [YOUR_PIPELINE_PROJECT_NAME]
 restartPolicy: Never
```

- Deploy the GKE pod by running `kubectl apply -f pipeline-deployment.yaml`.
- The deployment will take several minutes to complete, the pod will self terminate, and the Dataflow job will be created.

### (Option 2) Manual Installation

- Have a Linux (Ubuntu & Debian preferred) machine ready.
  - Install [GCC compiler](https://gcc.gnu.org/install/).
  - Install [Go tools](https://golang.org/doc/install), versions >= [1.14](https://golang.org/dl/) are recommended.
  - Install [Gradle](https://gradle.org/install/), version [6.3.0](https://gradle.org/next-steps/?version=6.3&format=bin) is recommended.
  - Install [Protoc](https://github.com/protocolbuffers/protobuf/releases), version [3.14.0](https://github.com/protocolbuffers/protobuf/releases) is recommended.
- Add your public key to [GitHub](https://help.github.com/en/github/authenticating-to-github/adding-a-new-ssh-key-to-your-github-account).
- Install the latest [GCloud SDK](https://cloud.google.com/sdk/install).

### GCP Environment Setup

- Create a [project](https://cloud.google.com/resource-manager/docs/creating-managing-projects).
- Create an [HL7v2 Store](https://cloud.google.com/healthcare/docs/how-tos/hl7v2).
  - Make sure to use beta endpoints and provide `NotificationConfig`s and a schematized `ParserConfig`.
- Create a [FHIR Store](https://cloud.google.com/healthcare/docs/how-tos/fhir).
  - Set [enableUpdateCreate](https://cloud.google.com/healthcare/docs/reference/rest/v1/projects.locations.datasets.fhirStores#FhirStore.FIELDS.enable_update_create) and [disableReferentialIntegrity](https://cloud.google.com/healthcare-api/docs/reference/rest/v1/projects.locations.datasets.fhirStores#FhirStore.FIELDS.disable_referential_integrity) for the FHIR store.
- Enable [Cloud Dataflow API](https://cloud.google.com/endpoints/docs/openapi/enable-api).
- (Highly recommended) Enable [audit logging](https://cloud.google.com/logging/docs/audit).

### Permissions

Make sure you have [enough permissions](https://cloud.google.com/dataflow/docs/concepts/access-control#creating_jobs) to run Cloud Dataflow jobs.

The [Cloud Dataflow Controller Service Account](https://cloud.google.com/dataflow/docs/concepts/security-and-permissions#controller_service_account) needs the following permissions.

- `roles/pubsub.subscriber`.
  - To listen for PubSub notifications from new messages. The service account only needs the role on the specific PubSub subscription.
- `roles/healthcare.hl7V2Consumer`.
  - To access messages in your HL7v2 store. The service account only needs the role on the source HL7v2 Store.
- `roles/healthcare.fhirResourceEditor`.
  - To write transformed resources to your FHIR store. The service account only needs this role on the target FHIR Store.
- `roles/storage.objectAdmin`.
  - To access mapping and harmonization configurations on GCS. The service account needs this role on all GCS buckets that the mappings reside in.

## How to Run

Build a fat JAR of the pipeline by running the following from the project directory.

- Please make sure gradle is added to PATH before running the following commands.

```bash
# Generate wrapper classes.
gradle wrapper --gradle-version 6.7.1
./gradlew shadowJar
```

A JAR file should be generated in `build/libs` folder.

Now run the pipeline with the following command:

```bash
# Please set the environment variables in the following command.

java -jar build/libs/converter-0.1.0-all.jar --pubSubSubscription="projects/${PROJECT?}/subscriptions/${SUBSCRIPTION?}" \
                                             --readErrorPath="gs://${ERROR_BUCKET?}/read/" \
                                             --writeErrorPath="gs://${ERROR_BUCKET?}/write/" \
                                             --mappingErrorPath="gs://${ERROR_BUCKET?}/mapping/" \
                                             --mappingPath="gs://${MAPPING_BUCKET?}/mapping.textproto" \
                                             --fhirStore="projects/${PROJECT?}/locations/${LOCATION?}/datasets/${DATASET?}/fhirStores/${FHIRSTORE?}" \
                                             --runner=DataflowRunner \
                                             --region=${REGION?} \
                                             --project=${PROJECT?}
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

- Have a Linux (Ubuntu & Debian preferred) machine ready.
  - Install [GCC compiler](https://gcc.gnu.org/install/).
  - Install [Go tools](https://golang.org/doc/install), versions >= [1.14](https://golang.org/dl/) are recommended.
  - Install [Gradle](https://gradle.org/install/), version [6.3.0](https://gradle.org/next-steps/?version=6.3&format=bin) is recommended.
  - Install [Protoc](https://github.com/protocolbuffers/protobuf/releases), version [3.14.0](https://github.com/protocolbuffers/protobuf/releases) is recommended.
- Add your public key to [GitHub](https://help.github.com/en/github/authenticating-to-github/adding-a-new-ssh-key-to-your-github-account).
- Install the latest [GCloud SDK](https://cloud.google.com/sdk/install).
- Create a [project](https://cloud.google.com/resource-manager/docs/creating-managing-projects).
- Create a [DICOM Store](https://cloud.google.com/healthcare/docs/how-tos/dicom).
- Create an R4 [FHIR Store](https://cloud.google.com/healthcare/docs/how-tos/fhir).
  - Set [disableReferentialIntegrity](https://cloud.google.com/healthcare-api/docs/reference/rest/v1/projects.locations.datasets.fhirStores#FhirStore.FIELDS.disable_referential_integrity) for the FHIR store.
- Enable [Cloud Dataflow API](https://cloud.google.com/endpoints/docs/openapi/enable-api).

### Permissions

Make sure you have [enough permissions](https://cloud.google.com/dataflow/docs/concepts/access-control#creating_jobs) to run Cloud Dataflow jobs.

The [Cloud Dataflow Controller Service Account](https://cloud.google.com/dataflow/docs/concepts/security-and-permissions#controller_service_account) needs the following permissions.

- `roles/pubsub.subscriber`.
  - To listen for PubSub notifications from new messages. The service account only needs the role on the specific PubSub subscription.
- `roles/healthcare.dicomEditor`.
  - To access metadata of DICOM stores.
- `roles/healthcare.fhirResourceEditor`.
  - To write transformed resources to your FHIR store. The service account only needs this role on the target FHIR Store.
- `roles/storage.objectAdmin`.
  - To access mapping and harmonization configurations on GCS. The service account needs this role on all GCS buckets that the mappings reside in.

## How to Run

Build a fat JAR of the pipeline by running the following from the project directory.

- Please make sure gradle is added to PATH before running the following commands.

```bash
# Generate wrapper classes.
gradle wrapper
./gradlew shadowJar -PmainClass=com.google.cloud.healthcare.etl.runner.dicomtofhir.DicomToFhirStreamingRunner
```

A JAR file should be generated in `build/libs` folder.

Now run the pipeline with the following command:

```bash
# Please set the environment variables in the following command.

java -jar build/libs/converter-0.1.0-all.jar --pubSubSubscription="projects/${PROJECT?}/subscriptions/${SUBSCRIPTION?}" \
                                             --readErrorPath="gs://${ERROR_BUCKET?}/read/" \
                                             --writeErrorPath="gs://${ERROR_BUCKET?}/write/" \
                                             --mappingErrorPath="gs://${ERROR_BUCKET?}/mapping/" \
                                             --mappingPath="gs://${MAPPING_BUCKET?}/main.textproto" \
                                             --fhirStore="projects/${PROJECT?}/locations/${LOCATION}/datasets/${DATASET?}/fhirStores/${FHIRSTORE?}" \
                                             --runner=DataflowRunner \
                                             --region=${REGION?} \
                                             --project=${PROJECT?}
```

A few notes:

- By default, streaming pipelines do not have autoscaling enabled, please use
  either `--enableStreamingEngine` (recommended) or a combination of `--autoscalingAlgorithm=THROUGHPUT_BASED` and
  `--maxNumWorkers=N` to manually enable it. See [this page](https://cloud.google.com/dataflow/docs/guides/deploying-a-pipeline#autotuning-features) for more details.
- For production use, we recommend enabling agent metrics by appending `--experiments=enable_stackdriver_agent_metrics` as an option (you will need to grant `roles/monitoring.metricWriter` to Dataflow controller service account as well), see [this page](https://cloud.google.com/dataflow/docs/guides/using-cloud-monitoring#receive_worker_vm_metrics_from_monitoring_agent) for more details. Additionally, we **highly** recommend limiting the number of threads on each worker, e.g. `--numberOfWorkerHarnessThreads=10`. You can tune the limit based on your workload.
- To generate a template instead of running the pipeline, add `--stagingLocation=gs://${STAGING_LOCATION} --templateLocation=gs://${TEMPLATE_LOCATION}` to the above command. See [here](https://cloud.google.com/dataflow/docs/guides/templates/creating-templates)
- The mappingPath file (main.textproto) configures the mapping library. Ensure that the paths inside the file exist (References the following repository: https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/). The required binaries should be installed by the build JAR command. There is a sample main.textproto at src/main/java/com/google/cloud/healthcare/etl/runner/dicomtofhir/main.textproto, if specifying GCS (non-local) paths use `gcs_location:` instead of `local_path:`.
- As the mappings do not assign an ID to the mapped FHIR resource, each input creates a new output in the FHIR store. TODO: evaluate maintaining an ID for DICOM Instances.

Please take a look at the `PipelineRunner` class to see the concrete meaning of
each argument.

You should be able to verify that a Dataflow pipeline is running from the cloud
console UI. Data should start flowing through the pipeline and arrive at the
FHIR Store, use the SearchResources API to verify that FHIR Resources are
written correctly.

# Custom to FHIR Pipeline

This directory contains a reference Cloud Dataflow pipeline to convert custom/non standard messages to FHIR resources. Please note that additional configurations and hardening are required before processing PHI data with this pipeline.

## Prerequisites

- Have a Linux (Ubuntu & Debian preferred) machine ready.
  - Install [GCC compiler](https://gcc.gnu.org/install/).
  - Install [Go tools](https://golang.org/doc/install), versions >= [1.14](https://golang.org/dl/) are recommended.
  - Install [Gradle](https://gradle.org/install/), version [6.3.0](https://gradle.org/next-steps/?version=6.3&format=bin) is recommended.
  - Install [Protoc](https://github.com/protocolbuffers/protobuf/releases), version [3.14.0](https://github.com/protocolbuffers/protobuf/releases) is recommended.
- Add your public key to [GitHub](https://help.github.com/en/github/authenticating-to-github/adding-a-new-ssh-key-to-your-github-account).
- Install the latest [GCloud SDK](https://cloud.google.com/sdk/install).
- Create a [project](https://cloud.google.com/resource-manager/docs/creating-managing-projects).
- Create a [FHIR Store](https://cloud.google.com/healthcare/docs/how-tos/fhir).
  - Set [enableUpdateCreate](https://cloud.google.com/healthcare/docs/reference/rest/v1/projects.locations.datasets.fhirStores#FhirStore.FIELDS.enable_update_create) and [disableReferentialIntegrity](https://cloud.google.com/healthcare-api/docs/reference/rest/v1/projects.locations.datasets.fhirStores#FhirStore.FIELDS.disable_referential_integrity) for the FHIR store.
- Enable [Cloud Dataflow API](https://cloud.google.com/endpoints/docs/openapi/enable-api).
- (Highly recommended) Enable [audit logging](https://cloud.google.com/logging/docs/audit).

### Permissions

Make sure you have [enough permissions](https://cloud.google.com/dataflow/docs/concepts/access-control#creating_jobs) to run Cloud Dataflow jobs.

The [Cloud Dataflow Controller Service Account](https://cloud.google.com/dataflow/docs/concepts/security-and-permissions#controller_service_account) needs the following permissions.

- `roles/pubsub.subscriber`.
  - To listen for PubSub notifications from new messages. The service account only needs the role on the specific PubSub subscription.
- `roles/healthcare.fhirResourceEditor`.
  - To write transformed resources to your FHIR store. The service account only needs this role on the target FHIR Store.
- `roles/storage.objectAdmin`.
  - To access mapping and harmonization configurations on GCS. The service account needs this role on all GCS buckets that the mappings reside in.
- `roles/pubsub.viewer`.
  - To access the PubSub subscription.
- `roles/dataflow.worker`.
  - To execute the Dataflow job.

## How to Run

Build a fat JAR of the pipeline by running the following from the project directory.

- Please make sure gradle is added to PATH before running the following commands.

```bash
# Generate wrapper classes.
gradle wrapper --gradle-version 6.7.1
./gradlew shadowJar
```

A JAR file should be generated in `build/libs` folder.

Now run the pipeline with the following command:

- Edit build.gradle and make the change to ensure the mainClassName is set as thus

shadowJar {
mainClassName = project.findProperty('mainClass') ?: 'com.google.cloud.healthcare.etl.runner.customtofhir.CustomToFhirStreamingRunner'
dependsOn('buildDeps')
}

- (Optional) Edit the build.gradle
  Depending on the java environment you might need the change as well for the code to build.

// sourceCompatibility = 11
sourceCompatibility = 1.8

```bash
# Please set the environment variables in the following command.

java -jar build/libs/converter-0.1.0-all.jar --pubSubSubscription="projects/${PROJECT?}/subscriptions/${SUBSCRIPTION?}" \
                                             --readErrorPath="gs://${ERROR_BUCKET?}/read/" \
                                             --writeErrorPath="gs://${ERROR_BUCKET?}/write/" \
                                             --mappingErrorPath="gs://${ERROR_BUCKET?}/mapping/" \
                                             --mappingPath="gs://${MAPPING_BUCKET?}/mapping.textproto" \
                                             --fhirStore="projects/${PROJECT?}/locations/${LOCATION?}/datasets/${DATASET?}/fhirStores/${FHIRSTORE?}" \
                                             --runner=DataflowRunner \
                                             --region=${REGION?} \
                                             --project=${PROJECT?} \
                                             --serviceAccount=dataflow-0222@smede-276406.iam.gserviceaccount.com
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

## Support

Please file GitHub issues if you encounter any problems.
