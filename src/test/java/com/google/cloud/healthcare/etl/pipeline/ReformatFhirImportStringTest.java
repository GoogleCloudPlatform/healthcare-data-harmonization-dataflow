package com.google.cloud.healthcare.etl.pipeline;

import com.google.cloud.healthcare.etl.runner.dicomtofhir.DicomToFhirStreamingRunner.ReformatFhirImportString;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

public class ReformatFhirImportStringTest {
    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void test_successfulReformat() {
        PCollection<String> reformatedString = pipeline
                .apply(Create.of(fhirResource))
                .apply(ParDo.of(new ReformatFhirImportString()));

        PAssert.that(reformatedString).containsInAnyOrder(fhirInputString);

        pipeline.run();
    }

    private final String fhirResource = "{\"resourceType\": \"ImagingStudy\"}";

    private final String fhirInputString = "{\"resourceType\":\"Bundle\",\"type\":\"batch\",\"entry\":[{\"resource\":{\"resourceType\":\"ImagingStudy\"},\"request\":{\"method\":\"PUT\",\"url\":\"ImagingStudy\"}}]}";
}
