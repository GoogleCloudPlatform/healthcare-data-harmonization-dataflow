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

package com.google.cloud.healthcare.etl.util;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Strings;
import javax.annotation.Nullable;

/** Utility methods for accessing GCS. */
public class GcsUtils {

  /**A wrapper class representing a GCS path. */
  public static class GcsPath {
    private final String bucket;
    private final String file;

    public GcsPath(String bucket, String file) {
      this.bucket = bucket;
      this.file = file;
    }

    public String getBucket() {
      return bucket;
    }

    public String getFile() {
      return file;
    }
  }

  private static final String SLASH = "/";
  private static final String SCHEME = "gs://";

  /** Parses a string as a GCS path. Returns {@code null} if the path is invalid. */
  @Nullable
  public static GcsPath parseGcsPath(String path) {
    if (Strings.isNullOrEmpty(path) || !path.startsWith(SCHEME)
        || path.length() <= SCHEME.length()) {
      return null;
    }

    path = path.substring(5);
    String[] parts = path.split(SLASH, 2);
    if (parts.length == 0 || Strings.isNullOrEmpty(parts[0])) {
      return null;
    }
    return new GcsPath(parts[0], parts.length == 2 ? parts[1] : "");
  }

  /** Reads a file from GCS. */
  public static Blob readFile(GcsPath path) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    return storage.get(BlobId.of(path.getBucket(), path.getFile()));
  }
}
