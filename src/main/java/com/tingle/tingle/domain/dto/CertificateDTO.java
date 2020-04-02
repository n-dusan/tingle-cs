package com.tingle.tingle.domain.dto;

import com.tingle.tingle.domain.enums.Role;

/**
 * DTO koji ce odlaziti u tabelu koja prikazuje spisak svih sertifikata
 * */
public class CertificateDTO {

    private Long id;
    private String serialNumber;
    private String alias;
    private boolean active;


    private Role certificateRole;


    public CertificateDTO() {}

    public CertificateDTO(Long id, String serialNumber, String alias, boolean active, Role role) {
        this.id = id;
        this.serialNumber = serialNumber;
        this.alias = alias;
        this.active = active;
        this.certificateRole = role;
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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
