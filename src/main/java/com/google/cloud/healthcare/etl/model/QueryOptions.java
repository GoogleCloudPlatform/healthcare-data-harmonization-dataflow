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
package com.google.cloud.healthcare.etl.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;
import java.io.Serializable;
import java.util.Map;

/** Represents tuples of table names and their corresponding BigQuery query as pipeline input. */
@AutoValue
@JsonDeserialize(builder = QueryOptions.Builder.class)
public abstract class QueryOptions implements Serializable {
  @JsonProperty
  public abstract Map<String, String> queries();

  public static Builder newBuilder() {
    return new AutoValue_QueryOptions.Builder();
  }

  @AutoValue.Builder
  @JsonPOJOBuilder(withPrefix = "set")
  abstract static class Builder {
    abstract Builder setQueries(Map<String, String> queries);
    abstract QueryOptions build();
    @JsonCreator
    public static Builder create() {
      return QueryOptions.newBuilder();
    }
  }
}
