package com.tingle.tingle.domain.dto;

public class BasicConstraintsDTO {

    private boolean critical;
    private boolean certificateAuthority;

    public BasicConstraintsDTO() {}

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public boolean isCertificateAuthority() {
        return certificateAuthority;
    }

    public void setCertificateAuthority(boolean certificateAuthority) {
        this.certificateAuthority = certificateAuthority;
    }
}
