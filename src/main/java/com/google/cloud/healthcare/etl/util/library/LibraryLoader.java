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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * A simple library class which helps with loading dynamic libraries stored in the JAR archive.
 * These libraries usually contain implementation of some methods in native code (using JNI - Java
 * Native Interface).
 */
public class LibraryLoader {

  private static final String PATH_PREFIX = "cloud_healthcare";

  /** Temporary directory which will contain the library file extracted from JAR. */
  private static File tmpDir;

  private LibraryLoader() {}

  /**
   * Loads library from current JAR archive.
   *
   * <p>The file from JAR is copied into system temporary directory and then loaded. The temporary
   * file is deleted after exiting.
   *
   * @param path The path of file inside JAR as absolute path (beginning with '/'), e.g.
   *     /package/libTest.so.
   * @throws IOException If temporary file creation or read/write operation fails.
   * @throws IllegalArgumentException If source file (param path) does not exist.
   * @throws FileNotFoundException If the file could not be found inside the JAR.
   */
  public static void loadLibraryFromJar(String path) throws IOException {

    if (path == null || !path.startsWith("/")) {
      throw new IllegalArgumentException("The path has to be absolute (start with '/').");
    }

    String[] parts = path.split("/");
    String filename = (parts.length > 1) ? parts[parts.length - 1] : null;

    if (filename == null || filename.isEmpty()) {
      throw new IllegalArgumentException("The filename is empty or invalid.");
    }

    if (tmpDir == null) {
      String tempDir = System.getProperty("java.io.tmpdir");
      tmpDir = new File(tempDir, PATH_PREFIX + "_" + System.nanoTime());
      if (!tmpDir.mkdir()) {
        throw new IOException("Failed to create temp directory " + tmpDir.getName());
      }
      tmpDir.deleteOnExit();
    }

    File tmpFile = new File(tmpDir, filename);

    try (InputStream is = LibraryLoader.class.getResourceAsStream(path)) {
      Files.copy(is, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      tmpFile.delete();
      throw e;
    } catch (NullPointerException e) {
      tmpFile.delete();
      throw new FileNotFoundException("File " + path + " was not found inside JAR.");
    }

    try {
      System.load(tmpFile.getAbsolutePath());
    } finally {
      tmpFile.deleteOnExit();
    }
  }
}
