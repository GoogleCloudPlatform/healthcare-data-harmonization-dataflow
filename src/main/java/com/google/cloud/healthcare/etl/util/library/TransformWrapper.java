/*
 * Copyright 2020 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.healthcare.etl.util.library;

import java.io.IOException;

/**
 * A wrapper class around the mapping engine. Native APIs are exposed to initialize the mapping
 * configurations, and execute the mapping.
 */
public class TransformWrapper {
  // Loads Whistler mapping library.
  private static final String LIBRARY_ROOT_IN_JAR = "/";
  private static final String WHISTLER_SHARED_OBJECT_FILE = "libwhistler.so";

  static {
    try {
      LibraryLoader.loadLibraryFromJar(LIBRARY_ROOT_IN_JAR + WHISTLER_SHARED_OBJECT_FILE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static TransformWrapper instance;

  private TransformWrapper() {}

  /**
   * Calls Whistler mapping library to map an input JSON to an output JSON.
   *
   * @param input a JSON string
   * @return a mapped JSON string
   */
  public static native String transform(String input);

  /**
   * Initializes Whistler mapping library with a Whistle code.
   *
   * @param config a Whistle code
   */
  public static native void initializeWhistle(String config);

  /**
   * Initializes Whistler mapping libray with a DataHarmonizationConfig proto.
   *
   * @param config a textproto of DataHarmoniationConfig
   */
  public static native void initializeWhistler(String config);

  public static TransformWrapper getInstance() {
    // Thread safe way of creating an instance, the outer check is to eliminate the cost of locking
    // every time, the inner check is to ensure we only create one instance.
    if (instance == null) {
      synchronized (TransformWrapper.class) {
        if (instance == null) {
          instance = new TransformWrapper();
          return instance;
        }
      }
    }
    return instance;
  }
}
