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

package com.google.cloud.healthcare.etl.model.streaming;

import com.google.cloud.healthcare.etl.util.TimeUtils;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/** Hl7v2Message represents a message fetched from the HL7v2 stores. */
public class Hl7v2Message implements Message {

  private Instant sendTime;
  private String body;

  public Hl7v2Message(Instant sendTime) {
    this(sendTime, "");
  }

  public Hl7v2Message(Instant sendTime, String body) {
    this.sendTime = sendTime;
    this.body = body;
  }

  @Override
  public Instant getSendTime() {
    return sendTime;
  }

  /** Creates a {@link Hl7v2Message} based on a message from the Hl7v2 stores. */
  public static Hl7v2Message fromModel(com.google.api.services.healthcare.v1beta1.model.Message msg)
      throws ParseException {
    Timestamp timestamp = Timestamps.parse(msg.getSendTime());
    String body = new String(Base64.getDecoder().decode(msg.getData()));
    return new Hl7v2Message(TimeUtils.tsToInstant(timestamp), body);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Hl7v2Message)) {
      return false;
    }

    Hl7v2Message msg = (Hl7v2Message) obj;
    return Objects.equals(sendTime, msg.sendTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sendTime);
  }

  @Override
  public String getBody() {
    return body;
  }
}
