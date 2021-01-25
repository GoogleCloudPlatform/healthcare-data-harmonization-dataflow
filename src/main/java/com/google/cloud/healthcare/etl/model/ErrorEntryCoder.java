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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.coders.IterableCoder;
import org.apache.beam.sdk.coders.NullableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;

/** A custom coder for {@link ErrorEntry}. */
public class ErrorEntryCoder extends CustomCoder<ErrorEntry> {
  private static NullableCoder<String> STRING_CODER = NullableCoder.of(StringUtf8Coder.of());
  private static NullableCoder<Iterable<String>> LIST_CODER =
      NullableCoder.of(IterableCoder.of(STRING_CODER));

  @Override
  public void encode(ErrorEntry value, OutputStream outStream) throws CoderException, IOException {
    STRING_CODER.encode(value.getErrorResource(), outStream);
    STRING_CODER.encode(value.getStackTrace(), outStream);
    STRING_CODER.encode(value.getErrorMessage(), outStream);
    STRING_CODER.encode(value.getTimestamp(), outStream);
    STRING_CODER.encode(value.getStep(), outStream);
    LIST_CODER.encode(value.getSources(), outStream);
  }

  @Override
  public ErrorEntry decode(InputStream inStream) throws CoderException, IOException {
    String errorResource = STRING_CODER.decode(inStream);
    String stackTrace = STRING_CODER.decode(inStream);
    String errorMessage = STRING_CODER.decode(inStream);
    String timestamp = STRING_CODER.decode(inStream);
    String step = STRING_CODER.decode(inStream);
    List<String> sources = Lists.newArrayList(LIST_CODER.decode(inStream));
    return new ErrorEntry(errorResource, errorMessage, stackTrace, timestamp)
        .setStep(step)
        .setSources(sources);
  }
}
