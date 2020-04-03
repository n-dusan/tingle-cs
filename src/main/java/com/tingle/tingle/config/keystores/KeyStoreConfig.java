package com.tingle.tingle.config.keystores;

import com.tingle.tingle.config.CertificateConfig;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.enums.Role;
import com.tingle.tingle.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.text.ParseException;

@Component
public class KeyStoreConfig {

    public static final String ROOT_KEYSTORE_LOCATION = "./.jks/root.jks";
    public static final String INTERMEDIATE_KEYSTORE_LOCATION = "./.jks/intermediate.jks";
    public static final String END_ENTITY_KEYSTORE_LOCATION = "./.jks/end-entity.jks";

    public static final String ROOT_KEYSTORE_PASSWORD = "root";
    public static final String INTERMEDIATE_KEYSTORE_PASSWORD = "intermediate";
    public static final String END_ENTITY_KEYSTORE_PASSWORD = "end-entity";

    @Autowired
    private KeyStoreWriter keyStoreWriter;

    @Autowired
    private CertificateService certificateService;

    public KeyStoreConfig() {}

    /**
     * Metoda pravi root .jks, ako ga nema, i ubacuje u njega jedan self-signed sertifikat
     *
     *  */

    public void generateRootKeyStore() throws CertificateException, ParseException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        keyStoreWriter.loadKeyStore(null, ROOT_KEYSTORE_PASSWORD.toCharArray());

        //generate root certificate, simulacija podataka sa front-enda
        CertificateX500NameDTO dto = new CertificateX500NameDTO();
        dto.setAlias("alias");
        dto.setC(CertificateConfig.ROOT_C);
        dto.setCN(CertificateConfig.ROOT_CN);
        dto.setE(CertificateConfig.ROOT_MAIL);
        dto.setL(CertificateConfig.ROOT_L);
        dto.setOU(CertificateConfig.ROOT_OU);
        dto.setO(CertificateConfig.ROOT_O);
        dto.setST(CertificateConfig.ROOT_ST);
        dto.setRole(Role.ROOT);

        certificateService.generateSelfSignedCertificate(dto);

        //sacuvaj stanje keystora
        keyStoreWriter.saveKeyStore(ROOT_KEYSTORE_LOCATION, ROOT_KEYSTORE_PASSWORD.toCharArray());
    }
    
   
}
