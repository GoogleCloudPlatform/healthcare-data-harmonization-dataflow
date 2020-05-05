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

package com.google.cloud.healthcare.etl.pipeline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.cloud.healthcare.etl.client.HttpApiClient;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/** Test for CommitBundleFn. */
@RunWith(PowerMockRunner.class)
@PrepareForTest(GoogleCredential.class)
public class CommitBundleFnTest {

  private static final String FHIR_STORE = "store";
  private static final List<String> INPUT = Lists.newArrayList("input1", "input2");
  private static final List<String> OUTPUT = Lists.newArrayList("output1", "output2");

  @Rule
  public final transient TestPipeline p = TestPipeline.create();

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(GoogleCredential.class);
    when(GoogleCredential.getApplicationDefault(any(), any())).thenReturn(null);
  }

  @Test
  public void commit_expectedResponse() throws IOException, URISyntaxException {
    HttpApiClient client = mock(HttpApiClient.class, withSettings().serializable());
    for (int i = 0; i < INPUT.size(); i++) {
      when(client.commitBundle(anyString(), eq(INPUT.get(i)))).thenReturn(OUTPUT.get(i));
    }
    PCollection<List<String>> msgNames = p.apply(Create.<List<String>>of(INPUT));
    PCollection<List<String>> output = msgNames.apply(
        ParDo.of(new CommitBundleFn(client, FHIR_STORE)));
    PAssert.thatSingleton(output).isEqualTo(OUTPUT);
    p.run();
  }

  @Test
  public void commit_empty_emptyResponse() throws IOException {
    PCollection<List<String>> msgNames = p.apply(Create.<List<String>>of(Lists.newArrayList(
        "", "{}", "{\"resourceType\":\"Bundle\",\"type\":\"transaction\"}")));
    PCollection<List<String>> output = msgNames.apply(
        ParDo.of(new CommitBundleFn(new HttpApiClient(), FHIR_STORE)));
    PAssert.thatSingleton(output).isEqualTo(Lists.newArrayList("", "", ""));
    p.run();
  }
}