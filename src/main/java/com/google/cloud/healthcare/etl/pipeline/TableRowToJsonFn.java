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

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.apache.beam.sdk.transforms.SerializableFunction;

/** A function to transform a BigQuery row to a JSON string nested inside a table name. */
public class TableRowToJsonFn implements SerializableFunction<TableRow, String> {

  private final ValueProvider<String> tableName;

  public TableRowToJsonFn(ValueProvider<String> tableName) {
    this.tableName = tableName;
  }

  public TableRowToJsonFn(String tableName) {
    this.tableName = StaticValueProvider.of(tableName);
  }

  @Override
  public String apply(TableRow row) {
    GenericJson json = new GenericJson().set(this.tableName.get(), row);
    json.setFactory(GsonFactory.getDefaultInstance());
    return json.toString();
  }
}
