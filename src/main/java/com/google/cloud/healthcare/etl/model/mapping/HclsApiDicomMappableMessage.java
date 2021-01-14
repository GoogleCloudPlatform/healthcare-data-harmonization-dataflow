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

/**
 * Represents DICOM metadata from the HCLS API for mapping. This class is meant to wrap the original
 * response from the API to be consumed by the mapping library. The ID is unused in this class.
 */
public class HclsApiDicomMappableMessage implements Mappable {
  private final String schematizedData;

  public HclsApiDicomMappableMessage(String schematizedData) {
    this.schematizedData = schematizedData;
  }

  public String getId() {
    return null;
  }

  public String getData() {
    return this.schematizedData;
  }

  public static HclsApiDicomMappableMessage from(String data) {
    return new HclsApiDicomMappableMessage(data);
  }
}
