package com.tingle.tingle.domain.dto;

import com.tingle.tingle.domain.enums.CRLReason;
import com.tingle.tingle.domain.enums.Role;

public class CertificateX500NameDTO {

    private ExtensionsDTO extensions;

    private String CN;
    private String O;
    private String L;
    private String ST;
    private String C;
    private String E;
    private String OU;

    private String serialNumber;

    //rola je tip sertifikata -> ROOT, END_ENTITY ili INTERMEDIATE, sa fronta se mapira kao string
    private Role certificateRole;

    private CRLReason revokationReason;


    public CertificateX500NameDTO() {}
    
   
    public String getCN() {
        return CN;
    }

    public void setCN(String CN) {
        this.CN = CN;
    }

    public String getO() {
        return O;
    }

    public void setO(String o) {
        O = o;
    }

    public String getL() {
        return L;
    }

    public void setL(String l) {
        L = l;
    }

    public String getST() {
        return ST;
    }

    public void setST(String ST) {
        this.ST = ST;
    }

    public String getC() {
        return C;
    }

    public void setC(String c) {
        C = c;
    }

    public String getE() {
        return E;
    }

    public void setE(String e) {
        E = e;
    }

    public Role getCertificateRole() {
        return certificateRole;
    }

    public void setCertificateRole(Role certificateRole) {
        this.certificateRole = certificateRole;
    }

    public String getOU() {
        return OU;
    }


    public void setOU(String OU) {
        this.OU = OU;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public ExtensionsDTO getExtensions() { return extensions; }

    public void setExtensions(ExtensionsDTO extensions) { this.extensions = extensions; }


    public CRLReason getRevokationReason() { return revokationReason; }

    public void setRevokationReason(CRLReason revokationReason) {this.revokationReason = revokationReason; }

}
