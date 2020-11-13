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
package com.google.cloud.healthcare.etl.model.mapping;

import java.util.Optional;
import javax.annotation.Nullable;
import org.joda.time.Instant;

/**
 * Represents a mapped message with source time as an output from {@link
 * com.google.cloud.healthcare.etl.pipeline.MappingFn}.
 */
public class MappedFhirMessageWithSourceTime implements MappingOutput {

  private final String output;
  @Nullable private Instant sourceTime;

  public MappedFhirMessageWithSourceTime(String output) {
    this.output = output;
  }

  public MappedFhirMessageWithSourceTime(String output, Instant sourceTime) {
    this.output = output;
    this.sourceTime = sourceTime;
  }

  public MappedFhirMessageWithSourceTime(String output, String sourceTime) {
    this.output = output;
    this.sourceTime = Instant.parse(sourceTime);
  }

  public String getOutput() {
    return output;
  }

  public Optional<Instant> getSourceTime() {
    return Optional.ofNullable(sourceTime);
  }
}
