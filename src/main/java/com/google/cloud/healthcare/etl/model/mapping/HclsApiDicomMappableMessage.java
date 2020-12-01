package com.google.cloud.healthcare.etl.model.mapping;

import org.apache.beam.sdk.io.gcp.healthcare.HL7v2Message;

/**
 * Represents DICOM metadata from the HCLS API for mapping. This class is meant to wrap the original response from the
 * API to be consumed by the mapping library. The ID is unused in this class.
 */
public class HclsApiDicomMappableMessage implements Mappable {
    private final String schematizedData;

    public HclsApiDicomMappableMessage(String schematizedData) {
        this.schematizedData = schematizedData;
    }

    public String getId() {
        return null;
    }

    public String getData() {
        return this.schematizedData;
    }

    public static HclsApiDicomMappableMessage from(String data) {
        return new HclsApiDicomMappableMessage(data);
    }
}
