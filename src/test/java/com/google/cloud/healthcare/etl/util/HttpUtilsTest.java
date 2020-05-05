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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpStatus;
import org.junit.Test;

/** Test for HttpUtils. */
public class HttpUtilsTest {

  @Test
  public void isSuccess_other_false() {
    assertFalse("Represents other.", HttpUtils.isSuccess(HttpStatus.SC_BAD_REQUEST));
    assertFalse("Represents other.", HttpUtils.isSuccess(HttpStatus.SC_INTERNAL_SERVER_ERROR));
    assertFalse("Represents other.", HttpUtils.isSuccess(1234567));
    assertFalse("Represents other.", HttpUtils.isSuccess(0));
    assertFalse("Represents other.", HttpUtils.isSuccess(-100));
    assertFalse("Represents other.", HttpUtils.isSuccess(Integer.MAX_VALUE));
    assertFalse("Represents other.", HttpUtils.isSuccess(Integer.MIN_VALUE));
  }

  @Test
  public void isSuccess_200_true() {
    assertTrue("Represents success.", HttpUtils.isSuccess(HttpStatus.SC_OK));
  }
}
