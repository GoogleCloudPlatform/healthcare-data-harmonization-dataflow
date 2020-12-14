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
import org.joda.time.Instant;

/**
 * MappingOutput defines an interface for outputs from {@link
 * com.google.cloud.healthcare.etl.pipeline.MappingFn}.
 */
public interface MappingOutput {

  /** Gets the output from the mapping engine. This is a valid JSON string. */
  String getOutput();

  /** Gets the create time of the source message before mapped. */
  default Optional<Instant> getSourceTime() {
    return Optional.empty();
  }
}
