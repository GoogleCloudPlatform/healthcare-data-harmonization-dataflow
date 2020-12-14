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
package org.apache.beam.sdk.io.gcp.healthcare;

import java.io.IOException;

/** A utility class to aid in the parsing of Healthcare API webpaths. */
public class WebPathParser {

  /** An object that holds DICOM webpath components. */
  public static class DicomWebPath {
    public String studyId;
    public String seriesId;
    public String instanceId;
    public String dicomStorePath;
    public String project;
    public String location;
    public String dataset;
    public String storeId;
  }

  /**
   * Resolves a string webpath into a DicomWebPath object.
   *
   * @param unparsedWebpath The webpath as a raw string.
   * @return A parsed DicomWebPath Object.
   * @throws IOException
   */
  public DicomWebPath parseDicomWebpath(String unparsedWebpath) throws IOException {
    String[] webPathSplit = unparsedWebpath.split("/dicomWeb/");

    if (webPathSplit.length != 2) {
      throw new IOException("Invalid DICOM web path");
    }

    DicomWebPath dicomWebPath = new DicomWebPath();

    dicomWebPath.dicomStorePath = webPathSplit[0];
    String[] storePathElements = dicomWebPath.dicomStorePath.split("/");

    if (storePathElements.length < 8) {
      throw new IOException("Invalid DICOM web path");
    }
    dicomWebPath.project = storePathElements[1];
    dicomWebPath.location = storePathElements[3];
    dicomWebPath.dataset = storePathElements[5];
    dicomWebPath.storeId = storePathElements[7];

    String[] searchParameters;
    searchParameters = webPathSplit[1].split("/");
    if (searchParameters.length < 6) {
      throw new IOException("Invalid DICOM web path");
    }
    dicomWebPath.studyId = searchParameters[1];
    dicomWebPath.seriesId = searchParameters[3];
    dicomWebPath.instanceId = searchParameters[5];

    return dicomWebPath;
  }
}
