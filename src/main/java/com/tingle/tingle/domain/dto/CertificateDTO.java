package com.tingle.tingle.domain.dto;

import com.tingle.tingle.domain.enums.CRLReason;
import com.tingle.tingle.domain.enums.Role;

/**
 * DTO koji ce odlaziti u tabelu koja prikazuje spisak svih sertifikata
 * */
public class CertificateDTO {

    private Long id;
    private String serialNumber;
    private boolean active;

    private Role certificateRole;
    private CRLReason revokationReason;



    public CertificateDTO() {}

    public CertificateDTO(Long id, String serialNumber, boolean active, Role role, CRLReason revokationReason) {
        this.id = id;
        this.serialNumber = serialNumber;
        this.active = active;
        this.certificateRole = role;
        this.revokationReason = revokationReason;
    }

    public CertificateDTO(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public Role getCertificateRole() {
        return certificateRole;
    }

    public void setCertificateRole(Role certificateRole) {
        this.certificateRole = certificateRole;
    }
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }


    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public CRLReason getRevokationReason() { return revokationReason; }

    public void setRevokationReason(CRLReason revocationReason) { this.revokationReason = revocationReason; }

}
