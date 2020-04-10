package com.tingle.tingle.domain.certificates;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.bouncycastle.asn1.x500.X500Name;

public class IssuerData {

    private X500Name x500name;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public IssuerData() {
    }

    public IssuerData(PrivateKey privateKey, X500Name x500name) {
        this.privateKey = privateKey;
        this.x500name = x500name;
    }

    public IssuerData(PrivateKey privateKey, X500Name x500name, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.x500name = x500name;
        this.publicKey = publicKey;
    }


    public X500Name getX500name() {
        return x500name;
    }

    public void setX500name(X500Name x500name) {
        this.x500name = x500name;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

}