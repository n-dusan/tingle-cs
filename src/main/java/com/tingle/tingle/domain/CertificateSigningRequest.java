package com.tingle.tingle.domain;

import javax.persistence.*;
import java.io.Serializable;

@Entity
public class CertificateSigningRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="publicKey", nullable = false)
    private String publicKey;

    @Column(name="commonName", nullable = false)
    private String commonName;

    @Column(name="organization", nullable = false)
    private String organization;

    @Column(name="organizationalUnit", nullable = false)
    private String organizationalUnit;

    @Column(name="location", nullable = false)
    private String location;

    @Column(name="state", nullable = false)
    private String state;

    @Column(name="country", nullable = false)
    private String country;

    @Column(name="email", nullable = false)
    private String email;

    @Column(name="CA", nullable = false)
    private boolean CA;

    @ManyToOne(fetch = FetchType.LAZY)
    private User requestedUser;



    public CertificateSigningRequest() {}

    public User getRequestedUser() {
        return requestedUser;
    }

    public void setRequestedUser(User requestedUser) {
        this.requestedUser = requestedUser;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getOrganizationalUnit() {
        return organizationalUnit;
    }

    public void setOrganizationalUnit(String organizationalUnit) {
        this.organizationalUnit = organizationalUnit;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isCA() {
        return CA;
    }

    public void setCA(boolean CA) {
        this.CA = CA;
    }
}
