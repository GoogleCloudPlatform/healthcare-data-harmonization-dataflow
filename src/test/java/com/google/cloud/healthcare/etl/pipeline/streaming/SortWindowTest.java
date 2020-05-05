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

package com.google.cloud.healthcare.etl.pipeline.streaming;

import com.google.cloud.healthcare.etl.model.streaming.Message;
import com.google.cloud.healthcare.etl.model.streaming.TestMsg;
import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Rule;
import org.junit.Test;

/** Test for SortWindow. */
public class SortWindowTest {

  private static final Instant NOW = Instant.now();
  private static final Instant DATE_TIME1 = NOW.plus(1L, ChronoUnit.SECONDS);
  private static final Instant DATE_TIME2 = NOW.plus(2L, ChronoUnit.SECONDS);
  private static final Instant DATE_TIME3 = NOW.plus(3L, ChronoUnit.SECONDS);

  @Rule
  public final transient TestPipeline p = TestPipeline.create();

  @Test
  public void sort_expectedOrder() {
    PCollection<Message> msgs = p.apply(Create.of(Lists.newArrayList(
        new TestMsg(DATE_TIME2),
        new TestMsg(DATE_TIME1),
        new TestMsg(DATE_TIME3)
    )));
    PCollection<List<Message>> output = msgs.apply(Combine.globally(new SortWindow()));
    PAssert.thatSingleton(output).isEqualTo(Lists.newArrayList(
        new TestMsg(DATE_TIME1),
        new TestMsg(DATE_TIME2),
        new TestMsg(DATE_TIME3)));
    p.run();
  }
}