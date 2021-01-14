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

import com.google.cloud.healthcare.etl.runner.dicomtofhir.DicomToFhirStreamingRunner.CreateMappingFnInput;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

/** Testing the DoFn's ability to generate a json string to be consumed by the mapping library. */
public class CreateMappingFnInputTest {
  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  @Test
  public void test_successfulReformat() {
    String metadataResp =
        "[{\"00080005\":{\"vr\":\"CS\",\"Value\":[\"ISO_IR 100\"]},\"00080020\":{\"vr\":\"DA\",\"Value\":[\"20091210\"]},\"00080030\":{\"vr\":\"TM\",\"Value\":[\"102925\"]},\"00080050\":{\"vr\":\"SH\"},\"00080090\":{\"vr\":\"PN\"},\"00100010\":{\"vr\":\"PN\",\"Value\":[{\"Alphabetic\":\"John^Doe\"}]},\"00100020\":{\"vr\":\"LO\",\"Value\":[\"Joelle-del\"]},\"00100030\":{\"vr\":\"DA\"},\"00100040\":{\"vr\":\"CS\"},\"0020000D\":{\"vr\":\"UI\",\"Value\":[\"study_000000000\"]},\"00200010\":{\"vr\":\"SH\",\"Value\":[\"7329\"]}},{\"00080005\":{\"vr\":\"CS\",\"Value\":[\"ISO_IR 100\"]},\"00080020\":{\"vr\":\"DA\",\"Value\":[\"20180731\"]},\"00080030\":{\"vr\":\"TM\",\"Value\":[\"194340.000000\"]},\"00100020\":{\"vr\":\"LO\",\"Value\":[\"6b027f5e-ca65-4fe6-96f4-32db013448d8\"]},\"0020000D\":{\"vr\":\"UI\",\"Value\":[\"1.2.276.0.7230010.3.1.2.1784940379.231387.1533066220.970617\"]}}]\n";

    PCollection<String> reformatedString =
        pipeline.apply(Create.of(metadataResp)).apply(ParDo.of(new CreateMappingFnInput()));

    String expectedOut =
        "{\"study\":[{\"00080005\":{\"vr\":\"CS\",\"Value\":[\"ISO_IR 100\"]},\"00080020\":{\"vr\":\"DA\",\"Value\":[\"20091210\"]},\"00080030\":{\"vr\":\"TM\",\"Value\":[\"102925\"]},\"00080050\":{\"vr\":\"SH\"},\"00080090\":{\"vr\":\"PN\"},\"00100010\":{\"vr\":\"PN\",\"Value\":[{\"Alphabetic\":\"John^Doe\"}]},\"00100020\":{\"vr\":\"LO\",\"Value\":[\"Joelle-del\"]},\"00100030\":{\"vr\":\"DA\"},\"00100040\":{\"vr\":\"CS\"},\"0020000D\":{\"vr\":\"UI\",\"Value\":[\"study_000000000\"]},\"00200010\":{\"vr\":\"SH\",\"Value\":[\"7329\"]}},{\"00080005\":{\"vr\":\"CS\",\"Value\":[\"ISO_IR 100\"]},\"00080020\":{\"vr\":\"DA\",\"Value\":[\"20180731\"]},\"00080030\":{\"vr\":\"TM\",\"Value\":[\"194340.000000\"]},\"00100020\":{\"vr\":\"LO\",\"Value\":[\"6b027f5e-ca65-4fe6-96f4-32db013448d8\"]},\"0020000D\":{\"vr\":\"UI\",\"Value\":[\"1.2.276.0.7230010.3.1.2.1784940379.231387.1533066220.970617\"]}}]}";

    PAssert.that(reformatedString).containsInAnyOrder(expectedOut);

    pipeline.run();
  }
}
