package com.google.cloud.healthcare.etl.runner.dicomtofhir;

import com.google.cloud.healthcare.etl.runner.dicomtofhir.DicomToFhirStreamingRunner.ExtractWebpathFromPubsub;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Test;
import org.junit.Rule;

public class ExtractWebpathFromPubsubTest {
    @Rule public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void test_successfulExtraction() {
        String webPathIn = "projects/foo/locations/earth/datasets/bar/dicomStores/fee/dicomWeb/studies/abc/series/xyz/instances/123";
        byte[] pubsubPayload = webPathIn.getBytes();
        PubsubMessage pubsubMessage = new PubsubMessage(pubsubPayload, null);

        PCollection<String> extractedWebpath = pipeline
                .apply(Create.of(pubsubMessage))
                .apply(ParDo.of(new ExtractWebpathFromPubsub()));

        PAssert.that(extractedWebpath).containsInAnyOrder(webPathIn);

        pipeline.run();
    }
}
