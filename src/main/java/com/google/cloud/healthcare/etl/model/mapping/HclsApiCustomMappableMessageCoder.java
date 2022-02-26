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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.coders.InstantCoder;
import org.apache.beam.sdk.coders.NullableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.joda.time.Instant;

/** Coder for {@link HclsApiCustomMappableMessage}. */
public class HclsApiCustomMappableMessageCoder extends CustomCoder<HclsApiCustomMappableMessage> {

  private static final NullableCoder<String> STRING_CODER = NullableCoder.of(StringUtf8Coder.of());
  private static final NullableCoder<Instant> INSTANT_CODER = NullableCoder.of(InstantCoder.of());

  public static HclsApiCustomMappableMessageCoder of() {
    return new HclsApiCustomMappableMessageCoder();
  }

  @Override
  public void encode(HclsApiCustomMappableMessage value, OutputStream outStream) throws IOException {
    STRING_CODER.encode(value.getId(), outStream);
    STRING_CODER.encode(value.getData(), outStream);
    INSTANT_CODER.encode(value.getCreateTime().orElse(null), outStream);
  }

  @Override
  public HclsApiCustomMappableMessage decode(InputStream inStream) throws IOException {
    String id = STRING_CODER.decode(inStream);
    String data = STRING_CODER.decode(inStream);
    Instant createTime = INSTANT_CODER.decode(inStream);
    return new HclsApiCustomMappableMessage(id, data, createTime);
  }
}
