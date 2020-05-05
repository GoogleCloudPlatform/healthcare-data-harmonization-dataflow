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

import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

/** Helper file that handles file interactions. */
public class FileUtils {
  // A FileVisitor that deletes all files all files it comes across.
  public static final SimpleFileVisitor<Path> DELETE_VISITOR =
      new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.deleteIfExists(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
          if (e == null) {
            Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
          } else {
            // directory iteration failed
            throw e;
          }
        }
      };

  /** Lists all the direct files within a directory. */
  public static List<String> listFiles(Path path) throws IOException {
    if (!path.toFile().exists()) {
      return Lists.newArrayList();
    }

    return Files.walk(path).filter(Files::isRegularFile)
        .map(Path::toString).collect(Collectors.toList());
  }
}
