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
 * Represents a custom json payload
 */
public class HclsApiCustomMappableMessage implements Mappable {

  private final String name;
  private final String schematizedData;
  @Nullable private Instant createTime;

  public HclsApiCustomMappableMessage(String name, String schematizedData) {
    this.name = name;
    this.schematizedData = schematizedData;
  }

 public HclsApiCustomMappableMessage(String name, String schematizedData, String createTime) {
    this.name = name;
    this.schematizedData = schematizedData;
    this.createTime = Instant.parse(createTime);
  }

  public HclsApiCustomMappableMessage(String name, String schematizedData, Instant createTime) {
    this.name = name;
    this.schematizedData = schematizedData;
    this.createTime = createTime;
  }


  public static HclsApiCustomMappableMessage from(String message) {
    return new HclsApiCustomMappableMessage(
        "\"input\"", message);
  }


  @Override
  public String getId() {
    return name;
  }

  @Override
  public String getData() {
    return schematizedData;
  }

  @Override
  public Optional<Instant> getCreateTime() {
    return Optional.ofNullable(createTime);
  }
}
