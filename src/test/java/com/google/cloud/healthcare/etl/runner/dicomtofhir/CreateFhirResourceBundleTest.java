package com.google.cloud.healthcare.etl.runner.dicomtofhir;

import com.google.cloud.healthcare.etl.runner.dicomtofhir.DicomToFhirStreamingRunner.CreateFhirResourceBundle;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

public class CreateFhirResourceBundleTest {

    private final String fhirResource = "{\"resourceType\": \"ImagingStudy\"}";

    private final String fhirInputString = "{\"resourceType\":\"Bundle\",\"type\":\"batch\",\"entry\":[{\"resource\":{\"resourceType\":\"ImagingStudy\"},\"request\":{\"method\":\"PUT\",\"url\":\"ImagingStudy\"}}]}";

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void test_successfulReformat() {
        PCollection<String> reformatedString = pipeline
                .apply(Create.of(fhirResource))
                .apply(ParDo.of(new CreateFhirResourceBundle()));

        PAssert.that(reformatedString).containsInAnyOrder(fhirInputString);

        pipeline.run();
    }
}
