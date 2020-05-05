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

package com.google.cloud.healthcare.etl.pipeline;

import com.google.cloud.healthcare.etl.client.ApiClient;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import org.apache.beam.sdk.values.TupleTag;

/**
 * CommitBundleFn takes a list of bundles (result from {@link MappingFn}) and commits them to
 * a specified FHIR store in order.
 */
public class CommitBundleFn extends ErrorEnabledDoFn<List<String>, List<String>> {
  public static final TupleTag<List<String>> COMMIT_BUNDLE_TAG =
      new TupleTag<>("commit_bundle");

  private final ApiClient client;
  private final String fhirStore;

  public CommitBundleFn(ApiClient client, String fhirStore) {
    this.client = client;
    this.fhirStore = fhirStore;
  }

  @Override
  public List<String> process(List<String> bundles) throws Exception {
      List<String> output = Lists.newArrayList();
      for (String bundle : bundles) {
        output.add(client.commitBundle(fhirStore, bundle));
      }
      return output;
  }

  @Override
  protected boolean reportOnly(Throwable e) {
    Class<? extends Throwable> clazz = e.getClass();
    return clazz == RuntimeException.class || clazz == IOException.class;
  }
}
