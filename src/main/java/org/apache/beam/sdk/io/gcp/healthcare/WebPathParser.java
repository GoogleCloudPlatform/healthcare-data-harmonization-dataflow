package org.apache.beam.sdk.io.gcp.healthcare;

import java.io.IOException;

public class WebPathParser {

    public class DicomWebPath {
        public String studyId;
        public String seriesId;
        public String instanceId;
        public String dicomStorePath;
        public String project;
        public String location;
        public String dataset;
        public String storeId;
    }

    public DicomWebPath parseDicomWebpath(String unparsedWebpath) throws IOException {
        String[] webPathSplit = unparsedWebpath.split("/dicomWeb/");

        if (webPathSplit.length != 2) {
            throw new IOException("Invalid DICOM web path");
        }

        DicomWebPath dicomWebPath = new DicomWebPath();

        dicomWebPath.dicomStorePath = webPathSplit[0];
        String[] storePathElements = dicomWebPath.dicomStorePath.split("/");
        dicomWebPath.project = storePathElements[1];
        dicomWebPath.location = storePathElements[3];
        dicomWebPath.dataset = storePathElements[5];
        dicomWebPath.storeId = storePathElements[7];

        String[] searchParameters;
        searchParameters = webPathSplit[1].split("/");
        if (searchParameters.length < 2) {
            throw new IOException("Invalid DICOM web path");
        }
        dicomWebPath.studyId = searchParameters[1];
        dicomWebPath.seriesId = searchParameters[3];
        dicomWebPath.instanceId = searchParameters[5];

        return dicomWebPath;
    }

}
