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

package com.google.cloud.healthcare.etl.client;

import com.google.cloud.healthcare.etl.model.streaming.Hl7v2Message;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.text.ParseException;

/** Defines a client which talks to the Cloud Healthcare API. */
public interface ApiClient extends Serializable {

  /** Fetches an Hl7v2 message by its name from a Hl7v2 store. */
  Hl7v2Message getMessage(String msgName) throws IOException, ParseException;

  /**
   * Commits a bundle to a FHIR store.
   *
   * @param fhirStore the name of the FHIR store to commit to. This doesn't include project, dataset
   *     or location.
   * @param bundle the content of the bundle to commit, should be a JSON string.
   */
  String commitBundle(String fhirStore, String bundle) throws IOException, URISyntaxException;
}
