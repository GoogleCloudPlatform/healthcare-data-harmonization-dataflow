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

import com.google.common.collect.Lists;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.values.TupleTag;

/** Includes the information about an error for logging and debugging purposes. */
public class ErrorEntry implements Serializable {
  public static final TupleTag<ErrorEntry> ERROR_ENTRY_TAG = new TupleTag<ErrorEntry>("errors") {};
  public static final Coder<ErrorEntry> CODER = new ErrorEntryCoder();

  private String errorResource;
  private String stackTrace;
  private String errorMessage;
  private String timestamp;
  private String step;
  private List<String> sources;

  protected ErrorEntry(
      String errorResource, String errorMessage, String stackTrace, String timestamp) {
    this.errorResource = errorResource;
    this.stackTrace = stackTrace;
    this.errorMessage = errorMessage;
    this.timestamp = timestamp;
  }

  /**
   * Creates an {@link ErrorEntry} from a {@link Throwable}, and records the occuring time of the
   * event based on the default system clock.
   */
  public static ErrorEntry of(Throwable t) {
    return of(t, "", Clock.systemDefaultZone());
  }

  /**
   * Creates an {@link ErrorEntry} from a {@link Throwable}, with the resource that caused the
   * error, and records the occurring time of the event based on the default system clock.
   */
  public static ErrorEntry of(Throwable t, String errorResource) {
    return of(t, errorResource, Clock.systemDefaultZone());
  }

  /**
   * Creates an {@link ErrorEntry} from a {@link Throwable}, and records the occuring time of the
   * event based on the {@code clock}.
   */
  public static ErrorEntry of(Throwable t, String errorResource, Clock clock) {
    StringWriter stringWriter = new StringWriter();
    t.printStackTrace(new PrintWriter(stringWriter));
    return new ErrorEntry(
        errorResource,
        t.getMessage(),
        stringWriter.toString(),
        ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_DATE_TIME));
  }

  public ErrorEntry setStep(String step) {
    this.step = step;
    return this;
  }

  public ErrorEntry setSources(List<String> sources) {
    this.sources = sources;
    return this;
  }

  public String getErrorResource() {
    return errorResource;
  }

  public List<String> getSources() {
    if (sources == null) {
      return Lists.newArrayList();
    }
    return sources;
  }

  public String getStep() {
    return step;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public boolean equals(Object other) {
    if (!(other instanceof ErrorEntry)) {
      return false;
    }
    if (!(Objects.equals(this.errorResource, ((ErrorEntry) other).errorResource))) {
      return false;
    }
    if (!(Objects.equals(this.stackTrace, ((ErrorEntry) other).stackTrace))) {
      return false;
    }
    if (!(Objects.equals(this.errorMessage, ((ErrorEntry) other).errorMessage))) {
      return false;
    }
    if (!(Objects.equals(this.timestamp, ((ErrorEntry) other).timestamp))) {
      return false;
    }
    if (!(Objects.equals(this.step, ((ErrorEntry) other).step))) {
      return false;
    }
    if (!(Objects.equals(this.sources, ((ErrorEntry) other).sources))) {
      return false;
    }
    return true;
  }
}
