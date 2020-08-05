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

import static com.google.common.truth.Truth.assertThat;

import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.junit.Test;

/** Tests for {@link TableRowToJsonFn}. */
public class TableRowToJsonFnTest {
  @Test
  public void apply_string_valid() {
    TableRowToJsonFn fn = new TableRowToJsonFn("Patient");
    TableRow row = new TableRow();
    row.set("address", "first street");
    row.set("id", "abc");
    assertThat(fn.apply(row))
        .isEqualTo("{\"Patient\":{\"address\":\"first street\",\"id\":\"abc\"}}");
  }

  @Test
  public void apply_valueprovider_valid() {
    TableRowToJsonFn fn = new TableRowToJsonFn(StaticValueProvider.of("Patient"));
    TableRow row = new TableRow();
    row.set("address", "first street");
    row.set("id", "abc");
    assertThat(fn.apply(row))
        .isEqualTo("{\"Patient\":{\"address\":\"first street\",\"id\":\"abc\"}}");
  }
}
