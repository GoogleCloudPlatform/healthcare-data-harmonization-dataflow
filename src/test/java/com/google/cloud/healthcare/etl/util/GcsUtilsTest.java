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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.cloud.healthcare.etl.util.GcsUtils.GcsPath;
import org.junit.Test;

/** Test for {@link GcsUtils}. */
public class GcsUtilsTest {

  private static final String BUCKET = "bucket";
  private static final String FILE = "this/is/my/file.txt";

  @Test
  public void parseGcsPath_invalid_null() {
    assertNull("empty", GcsUtils.parseGcsPath(""));
    assertNull("null", GcsUtils.parseGcsPath(null));
    assertNull("invalid scheme", GcsUtils.parseGcsPath("file://test/test"));
    assertNull("only scheme", GcsUtils.parseGcsPath("gs://"));
    assertNull("invalid bucket", GcsUtils.parseGcsPath("gs:///"));
  }

  @Test
  public void parseGcsPath_valid_bucketAndFile() {
    GcsPath parsed = GcsUtils.parseGcsPath(String.format("gs://%s/%s", BUCKET, FILE));
    assertEquals("bucket", BUCKET, parsed.getBucket());
    assertEquals("file", FILE, parsed.getFile());
  }

  @Test
  public void parseGcsPath_valid_bucket() {
    GcsPath parsed = GcsUtils.parseGcsPath(String.format("gs://%s/%s", BUCKET, ""));
    assertEquals("bucket", BUCKET, parsed.getBucket());
    assertEquals("file", "", parsed.getFile());
  }
}
