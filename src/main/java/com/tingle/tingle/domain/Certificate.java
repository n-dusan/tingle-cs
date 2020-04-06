package com.tingle.tingle.domain;

import com.tingle.tingle.domain.enums.CRLReason;
import com.tingle.tingle.domain.enums.Role;

import javax.persistence.*;
import java.io.Serializable;

@Entity
public class Certificate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="serialNumber", unique = true)
    private String serialNumber;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name="certificateRole", nullable = false)
    private Role certificateRole;


    @Enumerated(EnumType.STRING)
    @Column(name="revokeReason")
    private CRLReason revokeReason;


    public Certificate() {}

    public Certificate(String serialNumber, boolean active, Role role) {
        this.serialNumber = serialNumber;
        this.active = active;
        this.certificateRole = role;
    }

    public Role getCertificateRole() {
        return certificateRole;
    }

    public void setCertificateRole(Role certificateRole) {
        this.certificateRole = certificateRole;
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

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }


    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    public CRLReason getRevokeReason() { return revokeReason; }

    public void setRevokeReason(CRLReason revokeReason) { this.revokeReason = revokeReason; }

    @Override
    public String toString() {
        return "Certificate{" +
                "id=" + id +
                ", serialNumber='" + serialNumber + '\'' +
                ", active=" + active +
                ", certificateRole=" + certificateRole +
                '}';
    }
}
