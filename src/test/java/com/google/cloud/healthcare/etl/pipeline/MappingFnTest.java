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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link MappingFn}. */
public class MappingFnTest {

  private static final String INVALID_CONFIG = "random string";
  private static final String VALID_CONFIG = "structure_mapping_config: {\n"
      + "  mapping_language_string: \"out Output: Test(root); def Test(input) {foo: input.bar;}\"\n"
      + "}\n";

  private static final String INPUT = "{\"bar\":\"test\"}";
  private static final String INPUT2 = "{\"bar\":2}";
  private static final String OUTPUT = "{\"Output\":[{\"foo\":\"test\"}]}";
  private static final String OUTPUT2 = "{\"Output\":[{\"foo\":2}]}";

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void process_invalidConfig_exception() throws IOException, InterruptedException {
    Path path = prepareConfigFile(INVALID_CONFIG);
    try {
      MappingFn.of(path.toAbsolutePath().toString()).initialize();
      fail();
    } catch (RuntimeException e) {
      // no-op.
    }
  }

  @Test
  public void process_notInitialized_exception() throws IOException {
    Path path = prepareConfigFile(VALID_CONFIG);
    try {
      MappingFn.of(path.toAbsolutePath().toString()).process("{}");
      fail();
    } catch (RuntimeException e) {
      // no-op.
    }
  }

  @Test
  public void process_oneElement_result() throws InterruptedException, IOException {
    Path path = prepareConfigFile(VALID_CONFIG);
    MappingFn fn = MappingFn.of(path.toAbsolutePath().toString());
    fn.initialize();
    String output = fn.process(INPUT);
    assertEquals("Output should have exactly one element, and match the expected output.",
        OUTPUT, output);
  }

  private Path prepareConfigFile(String content) throws IOException {
    Path path = folder.newFile().toPath();
    Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
    return path;
  }
}
