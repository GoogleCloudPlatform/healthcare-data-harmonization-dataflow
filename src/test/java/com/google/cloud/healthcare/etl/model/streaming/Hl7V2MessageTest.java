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

import static org.junit.Assert.assertEquals;

import com.google.api.services.healthcare.v1beta1.model.Message;
import java.text.ParseException;
import java.time.Instant;
import org.junit.Test;

/** Test for Hl7v2Message. */
public class Hl7V2MessageTest {

  @Test
  public void fromModel_returnExpectedMessage() throws ParseException {
    Instant now = Instant.now();
    assertEquals(
        "Should return same message.",
        new Hl7v2Message(now),
        Hl7v2Message.fromModel(
            new Message().setSendTime(now.toString()).setData("")));
  }
}
