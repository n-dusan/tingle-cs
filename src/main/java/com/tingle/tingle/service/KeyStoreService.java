package com.tingle.tingle.service;

import com.tingle.tingle.config.CertificateConfig;
import com.tingle.tingle.config.KeyStoreConfig;
import com.tingle.tingle.util.keystores.KeyStoreReader;
import com.tingle.tingle.util.keystores.KeyStoreWriter;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.security.x509.X500Name;

import java.io.FileNotFoundException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KeyStoreService {

    @Autowired
    private KeyStoreWriter keyStoreWriter;

    @Autowired
    private KeyStoreReader keyStoreReader;

    @Autowired
    private CertificateService certificateService;



    public void saveCertificate(X509Certificate certificate, String alias, PrivateKey privateKey, Role role) {

        if(role == Role.ROOT) {
            keyStoreWriter.loadKeyStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
            //certificate password is the same as keystore password
            keyStoreWriter.write(alias, privateKey, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray(), certificate);
            keyStoreWriter.saveKeyStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
        } else if(role == Role.INTERMEDIATE) {
            keyStoreWriter.loadKeyStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());
            keyStoreWriter.write(alias, privateKey, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray(), certificate);
            keyStoreWriter.saveKeyStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());
        } else {
            keyStoreWriter.loadKeyStore(KeyStoreConfig.END_ENTITY_KEYSTORE_LOCATION, KeyStoreConfig.END_ENTITY_KEYSTORE_PASSWORD.toCharArray());
            keyStoreWriter.write(alias, privateKey, KeyStoreConfig.END_ENTITY_KEYSTORE_PASSWORD.toCharArray(), certificate);
            keyStoreWriter.saveKeyStore(KeyStoreConfig.END_ENTITY_KEYSTORE_LOCATION, KeyStoreConfig.END_ENTITY_KEYSTORE_PASSWORD.toCharArray());
        }
    }


    /**
     * @param certificateRole: Role from which the keystore will provide a list of certificates
     * @return List<X509Certificate>: returns a X509 casted list of certificates from the appropriate keystore
     * */
    public List<X509Certificate> findKeyStoreCertificates(Role certificateRole) throws FileNotFoundException {

        if(certificateRole == Role.ROOT) {
            List<Certificate> rootCerts = keyStoreReader.readAllCertificates(
                    KeyStoreConfig.ROOT_KEYSTORE_LOCATION, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
            return rootCerts.stream().map(e -> (X509Certificate) e).collect(Collectors.toList());
        }
        else if(certificateRole == Role.INTERMEDIATE) {
            List<Certificate> intermediateCerts = keyStoreReader.readAllCertificates(
                    KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());
            return intermediateCerts.stream().map(e -> (X509Certificate) e).collect(Collectors.toList());
        } else {
            List<Certificate> endEntityCerts = keyStoreReader.readAllCertificates(
                    KeyStoreConfig.END_ENTITY_KEYSTORE_LOCATION, KeyStoreConfig.END_ENTITY_KEYSTORE_PASSWORD.toCharArray());
            return endEntityCerts.stream().map(e -> (X509Certificate) e).collect(Collectors.toList());
        }
    }


    /**
     * Metoda pravi root .jks, ako ga nema, i ubacuje u njega jedan self-signed sertifikat
     *
     *  */
    public void generateRootKeyStore() throws CertificateException, ParseException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        keyStoreWriter.loadKeyStore(null, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());

        //generate root certificate, simulacija podataka sa front-enda
        CertificateX500NameDTO dto = new CertificateX500NameDTO();
        dto.setSerialNumber("123456");
        dto.setC(CertificateConfig.ROOT_C);
        dto.setCN(CertificateConfig.ROOT_CN);
        dto.setE(CertificateConfig.ROOT_MAIL);
        dto.setL(CertificateConfig.ROOT_L);
        dto.setOU(CertificateConfig.ROOT_OU);
        dto.setO(CertificateConfig.ROOT_O);
        dto.setST(CertificateConfig.ROOT_ST);
        dto.setCertificateRole(Role.ROOT);

        certificateService.generateSelfSignedCertificate(dto);

        //sacuvaj stanje keystora
        keyStoreWriter.saveKeyStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
    }

    public void generateCAKeyStore(CertificateX500NameDTO dto) {
        keyStoreWriter.loadKeyStore(null, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());

        dto.setC(dto.getC());
        dto.setCN(dto.getCN());
        dto.setE(dto.getE());
        dto.setL(dto.getL());
        dto.setOU(dto.getOU());
        dto.setO(dto.getO());
        dto.setST(dto.getST());
        dto.setCertificateRole(dto.getCertificateRole());
        dto.setSerialNumber(dto.getSerialNumber());

        try {
            certificateService.generateCACertificate(dto);
        } catch(Exception e) {
            e.printStackTrace();
        }
        //sacuvaj stanje keystora
        keyStoreWriter.saveKeyStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());
    }


}
