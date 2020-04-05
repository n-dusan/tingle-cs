package com.tingle.tingle.domain.dto;

public class ExtensionsDTO {

    private BasicConstraintsDTO basicConstraints;
    private KeyUsageDTO keyUsage;

    public ExtensionsDTO() {}

    public BasicConstraintsDTO getBasicConstraints() {
        return basicConstraints;
    }

    public void setBasicConstraints(BasicConstraintsDTO basicConstraints) {
        this.basicConstraints = basicConstraints;
    }

    public KeyUsageDTO getKeyUsage() {
        return keyUsage;
    }

    public void setKeyUsage(KeyUsageDTO keyUsage) {
        this.keyUsage = keyUsage;
    }
}
