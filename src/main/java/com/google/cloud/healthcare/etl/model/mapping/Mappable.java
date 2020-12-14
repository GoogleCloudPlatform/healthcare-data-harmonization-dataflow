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

import java.io.Serializable;
import java.util.Optional;
import javax.annotation.Nullable;
import org.joda.time.Instant;

/** Mappable defines a class that can be consumed by the mapping step. */
public interface Mappable extends Serializable {

  /**
   * A mappable must have a way to identify itself. The identifier doesn't have to be unique or
   * exist, as long as the user of the mapping engine has a way to distinguish the messages whenever
   * errors occur.
   *
   * @return a string identifier for the input or {@code null}.
   */
  @Nullable
  String getId();

  /** The input to be fed to the mapping engine. This must be a valid JSON string. */
  String getData();

  /**
   * The lineage metadata for mappable data to track create time the data. This must be a valid
   * datetime string in instant format.
   */
  default Optional<Instant> getCreateTime() {
    return Optional.empty();
  }
}
