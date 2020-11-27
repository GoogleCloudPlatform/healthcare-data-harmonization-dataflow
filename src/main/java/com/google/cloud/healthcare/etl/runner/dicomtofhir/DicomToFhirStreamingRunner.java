package com.google.cloud.healthcare.etl.runner.dicomtofhir;

import com.google.cloud.healthcare.etl.model.ErrorEntry;
import com.google.cloud.healthcare.etl.model.mapping.HclsApiDicomMappableMessage;
import com.google.cloud.healthcare.etl.model.mapping.HclsApiDicomMappableMessageCoder;
import com.google.cloud.healthcare.etl.pipeline.MappingFn;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.healthcare.DicomIO;
import org.apache.beam.sdk.io.gcp.healthcare.FhirIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class DicomToFhirStreamingRunner {
    private static final Logger LOG = LoggerFactory.getLogger(DicomToFhirStreamingRunner.class);

    public static class ExtractWebpathFromPubsub extends DoFn<PubsubMessage, String> {
        @ProcessElement
        public void processElement(DoFn<PubsubMessage, String>.ProcessContext context) throws UnsupportedEncodingException {
            PubsubMessage msg = context.element();
            byte[] msgPayload = msg.getPayload();
            String webpath = new String(msgPayload, StandardCharsets.UTF_8);
            context.output(webpath);
            DicomToFhirStreamingRunner.LOG.info("Extracted webpath");
        }
    }

    public static class ReformatMappingFnInputString extends DoFn<String, String> {
        @ProcessElement
        public void processElement(DoFn<String, String>.ProcessContext context) {
            String dicomMetadataResponse = context.element();
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(dicomMetadataResponse, JsonArray.class);
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("study", jsonArray);
            context.output(gson.toJson(jsonObject));
            DicomToFhirStreamingRunner.LOG.info("Sending instance to be mapped");
        }
    }

    public static class ReformatFhirImportString extends DoFn<String, String> {
        @ProcessElement
        public void processElement(DoFn<String, String>.ProcessContext context) {
            String mappingOutputString = context.element();

            Gson gson = new Gson();
            JsonObject mappingOutput = gson.fromJson(mappingOutputString, JsonObject.class);
            JsonObject requestObj = new JsonObject();

            requestObj.addProperty("method", "PUT");
            requestObj.addProperty("url", "ImagingStudy");
            JsonObject entryObj = new JsonObject();
            entryObj.add("resource", mappingOutput);
            entryObj.add("request", requestObj);
            JsonArray entries = new JsonArray();
            entries.add(entryObj);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("resourceType", "Bundle");
            jsonObject.addProperty("type", "batch");
            jsonObject.add("entry", entries);
            context.output(gson.toJson(jsonObject));
            DicomToFhirStreamingRunner.LOG.info("Uploading to FHIR store");
        }
    }

    public static void main(String[] args) {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
        Pipeline p = Pipeline.create(options);

        MappingFn<HclsApiDicomMappableMessage> mappingFn = MappingFn.of(options.getMappingPath());

        DicomIO.ReadStudyMetadata.Result dicomResult = p
                .apply(PubsubIO.readMessages().fromSubscription(options.getPubSubSubscription()))
                .apply(ParDo.of(new ExtractWebpathFromPubsub()))
                .apply(DicomIO.readStudyMetadata());

        PCollection<String> successfulReads = dicomResult.getReadResponse();
        PCollection<String> failedReads = dicomResult.getFailedReads();

        PCollectionTuple mappingResult = successfulReads
                .apply(ParDo.of(new ReformatMappingFnInputString()))
                .apply(MapElements.into(
                        TypeDescriptor.of(HclsApiDicomMappableMessage.class))
                        .via(HclsApiDicomMappableMessage::from))
                .setCoder(HclsApiDicomMappableMessageCoder.of())
                .apply("MapMessages", ParDo.of(mappingFn)
                        .withOutputTags(MappingFn.MAPPING_TAG, TupleTagList.of(ErrorEntry.ERROR_ENTRY_TAG)));

        FhirIO.Write.Result writeResults = mappingResult
                .get(MappingFn.MAPPING_TAG)
                .apply(ParDo.of(new ReformatFhirImportString()))
                .apply("WriteFHIRBundles", FhirIO.Write.executeBundles(options.getFhirStore()));
        p.run();
    }

    public interface Options extends PipelineOptions {
        @Description("The PubSub subscription to listen to, must be of the full format: projects/project_id/subscriptions/subscription_id")
        @Required
        String getPubSubSubscription();

        void setPubSubSubscription(String param1String);

        @Description("The path to the mapping configurations. The path will be treated as a GCS path if the path starts with the GCS scheme (\"gs\"), otherwise a local file. Please see: https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/baa4e0c7849413f7b44505a8410ee7f52745427a/mapping_configs/README.md for more details on the mapping configuration structure.")
        @Required
        String getMappingPath();

        void setMappingPath(String param1String);

        @Description("The target FHIR Store to write data to, must be of the full format: projects/project_id/locations/location/datasets/dataset_id/fhirStores/fhir_store_id")
        @Required
        String getFhirStore();

        void setFhirStore(String param1String);
    }
}
