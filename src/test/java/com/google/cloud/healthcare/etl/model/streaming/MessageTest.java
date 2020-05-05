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

import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

/** Tests for Message */
public class MessageTest {

  private static final Instant NOW = Instant.now();
  private static final Instant DATE_TIME1 = NOW.plus(1L, ChronoUnit.DAYS);
  private static final Instant DATE_TIME2 = NOW.plus(2L, ChronoUnit.DAYS);
  private static final Instant DATE_TIME3 = NOW.plus(3L, ChronoUnit.DAYS);

  @Test
  public void compareTo_returnsCorrectOrder() {
    List<Message> msgs = Lists.newArrayList(
        new TestMsg(DATE_TIME2),
        new TestMsg(DATE_TIME3),
        new TestMsg(DATE_TIME1));
    Collections.sort(msgs);
    List<Instant> dateTimes = msgs.stream()
        .map(Message::getSendTime).collect(Collectors.toList());
    assertEquals("Should return date times in expected order.",
        Lists.newArrayList(DATE_TIME1, DATE_TIME2, DATE_TIME3), dateTimes);
  }
  @Test
  public void compareTo_overrideCompareTo_returnsCorrectOrder() {
    List<Message> msgs = Lists.newArrayList(
        new OverriddenTestMsg(DATE_TIME2),
        new OverriddenTestMsg(DATE_TIME3),
        new OverriddenTestMsg(DATE_TIME1));
    Collections.sort(msgs);
    List<Instant> dateTimes = msgs.stream()
        .map(Message::getSendTime).collect(Collectors.toList());
    assertEquals("Should return date times in expected order.",
        Lists.newArrayList(DATE_TIME3, DATE_TIME2, DATE_TIME1), dateTimes);
  }
}