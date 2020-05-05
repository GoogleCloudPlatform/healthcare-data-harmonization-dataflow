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

package com.google.cloud.healthcare.etl.config;

import com.google.auth.Credentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collection;

/**
 * Singleton to hold GCP related configurations. These are separate from the ServerDefinition {@link
 * ServerDefinition} which vary by server.
 */
public class GcpConfiguration {

  // See https://developers.google.com/identity/protocols/googlescopes.
  private static final Collection<String> SCOPES =
      ImmutableSet.of(
          // General.
          "https://www.googleapis.com/auth/cloud-platform",
          // Dataflow.
          "https://www.googleapis.com/auth/compute",
          "https://www.googleapis.com/auth/userinfo.email",
          // Cloud Storage.
          "https://www.googleapis.com/auth/devstorage.full_control",
          // BigQuery.
          "https://www.googleapis.com/auth/bigquery");

  private static GcpConfiguration INSTANCE = new GcpConfiguration();

  private GcpConfiguration() {}

  public static GcpConfiguration getInstance() {
    return INSTANCE;
  }

  private Credentials credentials;

  public Credentials getCredentials() {
    return credentials;
  }

  public void setCredentials(Credentials credentials) throws IOException {
    this.credentials = credentials;
  }

  public Storage getGcsClient() {
    if (this.credentials == null) {
      return StorageOptions.getDefaultInstance().getService();
    }
    return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
  }
}
