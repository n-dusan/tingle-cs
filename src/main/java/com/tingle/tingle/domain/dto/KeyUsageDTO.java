package com.tingle.tingle.domain.dto;

public class KeyUsageDTO {

    private boolean critical;

    private boolean digitalSignature;
    private boolean nonRepudation;
    private boolean keyEncipherment;
    private boolean dataEncipherment;
    private boolean keyAgreement;
    private boolean keyCertSign;
    private boolean crlSign;
    private boolean encipherOnly;
    private boolean decipherOnly;

    public KeyUsageDTO() {}

    public void setUsages(boolean[] usages) {
    	this.setDigitalSignature(usages[0]);
    	this.setNonRepudation(usages[1]);
    	this.setKeyEncipherment(usages[2]);
    	this.setDataEncipherment(usages[3]);
    	this.setKeyAgreement(usages[4]);
    	this.setKeyCertSign(usages[5]);
    	this.setCrlSign(usages[6]);
    	this.setEncipherOnly(usages[7]);
    	this.setDecipherOnly(usages[8]);
    }
    
    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public boolean isDigitalSignature() {
        return digitalSignature;
    }

    public void setDigitalSignature(boolean digitalSignature) {
        this.digitalSignature = digitalSignature;
    }

    public boolean isNonRepudation() {
        return nonRepudation;
    }

    public void setNonRepudation(boolean nonRepudation) {
        this.nonRepudation = nonRepudation;
    }

    public boolean isKeyEncipherment() {
        return keyEncipherment;
    }

    public void setKeyEncipherment(boolean keyEncipherment) {
        this.keyEncipherment = keyEncipherment;
    }

    public boolean isDataEncipherment() {
        return dataEncipherment;
    }

    public void setDataEncipherment(boolean dataEncipherment) {
        this.dataEncipherment = dataEncipherment;
    }

    public boolean isKeyAgreement() {
        return keyAgreement;
    }

    public void setKeyAgreement(boolean keyAgreement) {
        this.keyAgreement = keyAgreement;
    }

    public boolean isKeyCertSign() {
        return keyCertSign;
    }

    public void setKeyCertSign(boolean keyCertSign) {
        this.keyCertSign = keyCertSign;
    }

    public boolean isCrlSign() {
        return crlSign;
    }

    public void setCrlSign(boolean crlSign) {
        this.crlSign = crlSign;
    }

    public boolean isEncipherOnly() {
        return encipherOnly;
    }

    public void setEncipherOnly(boolean encipherOnly) {
        this.encipherOnly = encipherOnly;
    }

    public boolean isDecipherOnly() {
        return decipherOnly;
    }

    public void setDecipherOnly(boolean decipherOnly) {
        this.decipherOnly = decipherOnly;
    }
}
