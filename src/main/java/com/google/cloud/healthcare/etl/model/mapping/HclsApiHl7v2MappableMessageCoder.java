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
import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.coders.NullableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;

/** Coder for {@link HclsApiHl7v2MappableMessage}. */
public class HclsApiHl7v2MappableMessageCoder extends CustomCoder<HclsApiHl7v2MappableMessage> {

  private static final NullableCoder<String> STRING_CODER = NullableCoder.of(StringUtf8Coder.of());

  public static HclsApiHl7v2MappableMessageCoder of() {
    return new HclsApiHl7v2MappableMessageCoder();
  }

  @Override
  public void encode(HclsApiHl7v2MappableMessage value, OutputStream outStream)
      throws CoderException, IOException {
    STRING_CODER.encode(value.getId(), outStream);
    STRING_CODER.encode(value.getData(), outStream);
  }

  @Override
  public HclsApiHl7v2MappableMessage decode(InputStream inStream)
      throws CoderException, IOException {
    String id = STRING_CODER.decode(inStream);
    String data = STRING_CODER.decode(inStream);
    return new HclsApiHl7v2MappableMessage(id, data);
  }
}
