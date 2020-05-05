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

import java.time.Instant;

/** Fake message for testing. */
public class TestMsg implements Message {
  private final Instant sendTime;
  private final String content;

  public TestMsg(Instant sendTime, String content) {
    this.sendTime = sendTime;
    this.content = content;
  }

  public TestMsg(Instant sendTime) {
    this(sendTime, "");
  }

  @Override
  public Instant getSendTime() {
    return sendTime;
  }

  @Override
  public String getBody() {
    return "";
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TestMsg)) {
      return false;
    }

    TestMsg m = (TestMsg) o;
    return getSendTime().equals(m.getSendTime()) && content.equals(m.content);
  }

  @Override
  public String toString() {
    return content;
  }
}
