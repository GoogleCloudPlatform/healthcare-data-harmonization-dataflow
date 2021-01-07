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

import com.google.cloud.healthcare.etl.runner.dicomtofhir.DicomToFhirStreamingRunner.ExtractWebpathFromPubsub;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

/**
 * Testing the ability of this DoFn to extract the webpath of a DICOM instance from the PubSub
 * message.
 */
public class ExtractWebpathFromPubsubTest {
  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  @Test
  public void test_successfulExtraction() {
    String webPathIn =
        "projects/foo/locations/earth/datasets/bar/dicomStores/fee/dicomWeb/studies/abc/series/xyz/instances/123";
    byte[] pubsubPayload = webPathIn.getBytes();
    PubsubMessage pubsubMessage = new PubsubMessage(pubsubPayload, null);

    PCollection<String> extractedWebpath =
        pipeline.apply(Create.of(pubsubMessage)).apply(ParDo.of(new ExtractWebpathFromPubsub()));

    PAssert.that(extractedWebpath).containsInAnyOrder(webPathIn);

    pipeline.run();
  }
}
