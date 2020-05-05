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
import java.util.List;
import java.util.stream.Collectors;
import org.apache.beam.sdk.transforms.DoFn;

/** Extracts the message body. */
public class ExtractMessageBodyFn extends DoFn<List<Message>, List<String>> {

  @ProcessElement
  public void extract(ProcessContext ctx) {
    List<Message> messages = ctx.element();
    ctx.output(messages.stream().map(Message::getBody).collect(Collectors.toList()));
  }
}
