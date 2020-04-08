package com.tingle.tingle.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeyStoreConfig {

    @Value("${tingle.root.keystore.location}")
    private String rootKeyStoreLocation;

    @Value("${tingle.intermediate.keystore.location}")
    private String intermediateKeyStoreLocation;

    @Value("${tingle.endentity.keystore.location}")
    private String endEntityKeyStoreLocation;

    @Value("${tingle.root.keystore.password}")
    private String rootKeyStorePassword;

    @Value("${tingle.intermediate.keystore.password}")
    private String intermediateKeyStorePassword;

    @Value("${tingle.endentity.keystore.password}")
    private String endEntityKeyStorePassword;

    @Value("${tingle.root.duration.years}")
    private Integer rootYears;

    @Value("${tingle.intermediate.duration.years}")
    private Integer intermediateYears;

    @Value("${tingle.endentity.duration.years}")
    private Integer endEntityYears;

    public String getRootKeyStoreLocation() {
        return rootKeyStoreLocation;
    }

    public void setRootKeyStoreLocation(String rootKeyStoreLocation) {
        this.rootKeyStoreLocation = rootKeyStoreLocation;
    }

    public String getIntermediateKeyStoreLocation() {
        return intermediateKeyStoreLocation;
    }

    public void setIntermediateKeyStoreLocation(String intermediateKeyStoreLocation) {
        this.intermediateKeyStoreLocation = intermediateKeyStoreLocation;
    }

    public String getEndEntityKeyStoreLocation() {
        return endEntityKeyStoreLocation;
    }

    public void setEndEntityKeyStoreLocation(String endEntityKeyStoreLocation) {
        this.endEntityKeyStoreLocation = endEntityKeyStoreLocation;
    }

    public String getRootKeyStorePassword() {
        return rootKeyStorePassword;
    }

    public void setRootKeyStorePassword(String rootKeyStorePassword) {
        this.rootKeyStorePassword = rootKeyStorePassword;
    }

    public String getIntermediateKeyStorePassword() {
        return intermediateKeyStorePassword;
    }

    public void setIntermediateKeyStorePassword(String intermediateKeyStorePassword) {
        this.intermediateKeyStorePassword = intermediateKeyStorePassword;
    }

    public String getEndEntityKeyStorePassword() {
        return endEntityKeyStorePassword;
    }

    public void setEndEntityKeyStorePassword(String endEntityKeyStorePassword) {
        this.endEntityKeyStorePassword = endEntityKeyStorePassword;
    }

    public Integer getRootYears() {
        return rootYears;
    }

    public void setRootYears(Integer rootYears) {
        this.rootYears = rootYears;
    }

    public Integer getIntermediateYears() {
        return intermediateYears;
    }

    public void setIntermediateYears(Integer intermediateYears) {
        this.intermediateYears = intermediateYears;
    }

    public Integer getEndEntityYears() {
        return endEntityYears;
    }

    public void setEndEntityYears(Integer endEntityYears) {
        this.endEntityYears = endEntityYears;
    }

}
