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

package com.google.cloud.healthcare.etl.model.converter;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.healthcare.etl.model.ErrorEntry;
import com.google.common.collect.ImmutableList;

/** Utility class for converting {@link ErrorEntry} to other formats for logging. */
public class ErrorEntryConverter {
  public static final String STACKTRACE_FIELD = "stacktrace";
  public static final String ERROR_MESSAGE_FIELD = "error_message";
  public static final String TIMESTAMP_FIELD = "timestamp";
  public static final String STEP_FIELD = "step";
  public static final String SOURCE_FIELD = "sources";

  // Schema for the BigQuery table to store error entries.
  public static final TableSchema BIG_QUERY_TABLE_SCHEMA =
      new TableSchema()
          .setFields(
              ImmutableList.of(
                  new TableFieldSchema()
                      .setName(STACKTRACE_FIELD)
                      .setType("STRING")
                      .setMode("NULLABLE"),
                  new TableFieldSchema()
                      .setName(ERROR_MESSAGE_FIELD)
                      .setType("STRING")
                      .setMode("NULLABLE"),
                  new TableFieldSchema()
                      .setName(TIMESTAMP_FIELD)
                      .setType("STRING")
                      .setMode("NULLABLE"),
                  new TableFieldSchema()
                      .setName(STEP_FIELD)
                      .setType("STRING")
                      .setMode("NULLABLE"),
                  new TableFieldSchema()
                      .setName(SOURCE_FIELD)
                      .setType("STRING")
                      .setMode("REPEATED")));


  public static TableRow toTableRow(ErrorEntry entry) {
    return new TableRow()
        .set(STACKTRACE_FIELD, entry.getStackTrace())
        .set(ERROR_MESSAGE_FIELD, entry.getErrorMessage())
        .set(TIMESTAMP_FIELD, entry.getTimestamp())
        .set(STEP_FIELD, entry.getStep())
        .set(SOURCE_FIELD, entry.getSources());
  }
}
