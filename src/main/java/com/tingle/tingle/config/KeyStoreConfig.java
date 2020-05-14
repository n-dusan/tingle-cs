package com.tingle.tingle.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeyStoreConfig {

    @Value("${tingle.truststore.location}")
    private String trustStoreLocation;

    @Value("${tingle.truststore.password}")
    private String trustStorePassword;


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

    public String getIntermediateKeyStoreLocation() {
        return intermediateKeyStoreLocation;
    }

    public String getEndEntityKeyStoreLocation() {
        return endEntityKeyStoreLocation;
    }


    public String getRootKeyStorePassword() {
        return rootKeyStorePassword;
    }

    public String getIntermediateKeyStorePassword() {
        return intermediateKeyStorePassword;
    }


    public String getEndEntityKeyStorePassword() {
        return endEntityKeyStorePassword;
    }

    public Integer getRootYears() {
        return rootYears;
    }

    public Integer getIntermediateYears() {
        return intermediateYears;
    }

    public Integer getEndEntityYears() {
        return endEntityYears;
    }

    public String getTrustStoreLocation() {
        return trustStoreLocation;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

}
