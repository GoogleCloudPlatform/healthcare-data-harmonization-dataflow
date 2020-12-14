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

import static com.google.cloud.healthcare.etl.model.converter.ErrorEntryConverter.ERROR_MESSAGE_FIELD;
import static com.google.cloud.healthcare.etl.model.converter.ErrorEntryConverter.SOURCE_FIELD;
import static com.google.cloud.healthcare.etl.model.converter.ErrorEntryConverter.STACKTRACE_FIELD;
import static com.google.cloud.healthcare.etl.model.converter.ErrorEntryConverter.STEP_FIELD;
import static com.google.cloud.healthcare.etl.model.converter.ErrorEntryConverter.TIMESTAMP_FIELD;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.healthcare.etl.model.ErrorEntry;
import com.google.common.collect.Lists;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.Test;

/** Test for ErrorEntryConverter. */
public class ErrorEntryConverterTest {
  private static final String STEP = "step1";
  private static final String MESSAGE = "message";

  private static final String TORONTO_TIME_ZONE = "America/Toronto";

  @Test
  public void toTableRow_expectedResult() {
    Instant now = Instant.now();
    ZoneId zone = ZoneId.of(TORONTO_TIME_ZONE);
    ErrorEntry entry =
        ErrorEntry.of(new IllegalArgumentException(MESSAGE), "", Clock.fixed(now, zone))
            .setStep(STEP);
    assertWithMessage("Mapped fields do not match.")
        .that(ErrorEntryConverter.toTableRow(entry).set(STACKTRACE_FIELD, ""))
        .isEqualTo(
            new TableRow()
                .set(STACKTRACE_FIELD, "")
                .set(ERROR_MESSAGE_FIELD, MESSAGE)
                .set(
                    TIMESTAMP_FIELD,
                    ZonedDateTime.ofInstant(now, zone).format(DateTimeFormatter.ISO_DATE_TIME))
                .set(STEP_FIELD, STEP)
                .set(SOURCE_FIELD, Lists.newArrayList()));
  }
}
