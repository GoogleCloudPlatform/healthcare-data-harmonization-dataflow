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
package com.google.cloud.healthcare.etl.runner.dicomtofhir;

import com.google.cloud.healthcare.etl.runner.dicomtofhir.DicomToFhirStreamingRunner.CreateFhirResourceBundle;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

/**
 * Testing the class's ability to generate a FHIR resource of type "Bundle" as a json string.
 */
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
