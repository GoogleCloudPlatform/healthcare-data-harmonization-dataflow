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

import static com.google.cloud.healthcare.etl.model.ErrorEntry.ERROR_ENTRY_TAG;
import static com.google.cloud.healthcare.etl.pipeline.MappingFn.MAPPING_TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.api.client.util.Lists;
import com.google.cloud.healthcare.etl.model.ErrorEntry;
import com.google.cloud.healthcare.etl.model.mapping.HclsApiHl7v2MappableMessage;
import com.google.cloud.healthcare.etl.model.mapping.HclsApiHl7v2MappableMessageCoder;
import com.google.cloud.healthcare.etl.model.mapping.MappedFhirMessageWithSourceTimeCoder;
import com.google.cloud.healthcare.etl.model.mapping.MappingOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link MappingFn}. */
public class MappingFnTest {

  private static final String INVALID_CONFIG = "random string";
  private static final String VALID_CONFIG =
      "structure_mapping_config: {\n"
          + "  mapping_language_string: \"out Output: Test(root); def Test(input) {foo:"
          + " input.bar;}\"\n"
          + "}\n";

  private static final String MESSAGE_ID = "id";
  private static final String INPUT = "{\"bar\":\"test\"}";
  private static final String INPUT2 = "{\"bar\":2}";
  private static final String OUTPUT = "{\"Output\":[{\"foo\":\"test\"}]}";
  private static final String OUTPUT2 = "{\"Output\":[{\"foo\":2}]}";

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void process_invalidConfig_exception() throws IOException, InterruptedException {
    Path path = prepareConfigFile(INVALID_CONFIG);
    try {
      MappingFn.of(path.toAbsolutePath().toString(), false).initialize();
      fail();
    } catch (RuntimeException e) {
      // no-op.
    }
  }

  @Test
  public void process_invalidInput_error() throws IOException {
    Path path = prepareConfigFile(VALID_CONFIG);
    Pipeline p = TestPipeline.create();
    p.getCoderRegistry()
        .registerCoderForClass(
            HclsApiHl7v2MappableMessage.class, HclsApiHl7v2MappableMessageCoder.of());
    MappingFn<HclsApiHl7v2MappableMessage> mappingFn =
        MappingFn.of(path.toAbsolutePath().toString(), false);
    PCollectionTuple output =
        p.apply(Create.of(new HclsApiHl7v2MappableMessage(MESSAGE_ID, "{")))
            .apply(
                ParDo.of(mappingFn).withOutputTags(MAPPING_TAG, TupleTagList.of(ERROR_ENTRY_TAG)));
    PAssert.that(output.get(MAPPING_TAG).setCoder(MappedFhirMessageWithSourceTimeCoder.of()))
        .empty();
    PAssert.that(output.get(ERROR_ENTRY_TAG))
        .satisfies(
            new SerializableFunction<Iterable<ErrorEntry>, Void>() {
              @Override
              public Void apply(Iterable<ErrorEntry> input) {
                List<ErrorEntry> errors = Lists.newArrayList(input);
                assertEquals("Only one error", 1, errors.size());
                assertEquals("Message id matches", errors.get(0).getSources().get(0), MESSAGE_ID);
                return null;
              }
            });
  }

  @Test
  public void process_oneElement_result() throws InterruptedException, IOException {
    Path path = prepareConfigFile(VALID_CONFIG);
    Pipeline p = TestPipeline.create();
    p.getCoderRegistry()
        .registerCoderForClass(
            HclsApiHl7v2MappableMessage.class, HclsApiHl7v2MappableMessageCoder.of());
    MappingFn<HclsApiHl7v2MappableMessage> mappingFn =
        MappingFn.of(path.toAbsolutePath().toString(), false);
    PCollectionTuple output =
        p.apply(Create.of(new HclsApiHl7v2MappableMessage(MESSAGE_ID, INPUT)))
            .apply(
                ParDo.of(mappingFn).withOutputTags(MAPPING_TAG, TupleTagList.of(ERROR_ENTRY_TAG)));
    PAssert.that(
            output
                .get(MAPPING_TAG)
                .setCoder(MappedFhirMessageWithSourceTimeCoder.of())
                .apply(MapElements.into(TypeDescriptors.strings()).via(MappingOutput::getOutput)))
        .containsInAnyOrder(OUTPUT);
    PAssert.that(output.get(ERROR_ENTRY_TAG)).empty();
  }

  @Test
  public void process_multipleElement_result() throws InterruptedException, IOException {
    Path path = prepareConfigFile(VALID_CONFIG);
    Pipeline p = TestPipeline.create();
    p.getCoderRegistry()
        .registerCoderForClass(
            HclsApiHl7v2MappableMessage.class, HclsApiHl7v2MappableMessageCoder.of());
    MappingFn<HclsApiHl7v2MappableMessage> mappingFn =
        MappingFn.of(path.toAbsolutePath().toString(), false);
    PCollectionTuple output =
        p.apply(
                Create.of(
                    new HclsApiHl7v2MappableMessage(MESSAGE_ID, INPUT),
                    new HclsApiHl7v2MappableMessage(MESSAGE_ID, INPUT2)))
            .apply(
                ParDo.of(mappingFn).withOutputTags(MAPPING_TAG, TupleTagList.of(ERROR_ENTRY_TAG)));
    PAssert.that(
            output
                .get(MAPPING_TAG)
                .setCoder(MappedFhirMessageWithSourceTimeCoder.of())
                .apply(MapElements.into(TypeDescriptors.strings()).via(MappingOutput::getOutput)))
        .containsInAnyOrder(OUTPUT, OUTPUT2);
    PAssert.that(output.get(ERROR_ENTRY_TAG)).empty();
  }

  private Path prepareConfigFile(String content) throws IOException {
    Path path = folder.newFile().toPath();
    Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
    return path;
  }
}
