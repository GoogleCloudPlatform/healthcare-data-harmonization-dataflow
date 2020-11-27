package com.google.cloud.healthcare.etl.model.mapping;

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
