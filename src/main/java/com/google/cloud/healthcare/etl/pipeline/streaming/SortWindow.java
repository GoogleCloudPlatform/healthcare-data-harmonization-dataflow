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
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import org.apache.beam.sdk.transforms.Combine.CombineFn;

/** Sorts the messages accumulated in a window. */
public class SortWindow extends CombineFn<Message, List<Message>, List<Message>> {

  @Override
  public List<Message> createAccumulator() {
    return Lists.newArrayList();
  }

  @Override
  public List<Message> addInput(List<Message> accumulator, Message input) {
    accumulator.add(input);
    return accumulator;
  }

  @Override
  public List<Message> mergeAccumulators(Iterable<List<Message>> accumulators) {
    List<Message> merged = Lists.newArrayList();
    accumulators.forEach(merged::addAll);
    return merged;
  }

  @Override
  public List<Message> extractOutput(List<Message> accumulator) {
    // TODO(b/124106453): if our QPS is high enough, we should consider sorting the messages each
    // addInput all and merge the sorted sub-lists.
    Collections.sort(accumulator);
    return accumulator;
  }
}
