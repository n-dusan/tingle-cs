package com.tingle.tingle.service;

import com.tingle.tingle.config.KeyStoreConfig;
import com.tingle.tingle.util.keystores.KeyStoreReader;
import com.tingle.tingle.util.keystores.KeyStoreWriter;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.tingle.tingle.domain.dto.EndEntityDTO;


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
     * forward null to get all certificates from all keystores
     * @return List<X509Certificate>: returns a X509 casted list of certificates from the appropriate keystore
     * */
    public List<X509Certificate> findKeyStoreCertificates(Role certificateRole) {

        List<Certificate> rootCerts = keyStoreReader.readAllCertificates(
                KeyStoreConfig.ROOT_KEYSTORE_LOCATION, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());

        List<Certificate> intermediateCerts = keyStoreReader.readAllCertificates(
                KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());

        List<Certificate> endEntityCerts = keyStoreReader.readAllCertificates(
                KeyStoreConfig.END_ENTITY_KEYSTORE_LOCATION, KeyStoreConfig.END_ENTITY_KEYSTORE_PASSWORD.toCharArray());

        if(certificateRole != null) {
            if (certificateRole == Role.ROOT) {

                return rootCerts.stream().map(e -> (X509Certificate) e).collect(Collectors.toList());
            } else if (certificateRole == Role.INTERMEDIATE) {

                return intermediateCerts.stream().map(e -> (X509Certificate) e).collect(Collectors.toList());
            } else {

                return endEntityCerts.stream().map(e -> (X509Certificate) e).collect(Collectors.toList());
            }
        } else {
            List<Certificate> all = new ArrayList<Certificate>();
            all.addAll(rootCerts);
            all.addAll(intermediateCerts);
            all.addAll(endEntityCerts);
            return all.stream().map(e -> (X509Certificate) e).collect(Collectors.toList());
        }
    }


    /**
     * Metoda pravi root .jks, ako ga nema, i ubacuje u njega jedan self-signed sertifikat
     * @param: dto: DTO sent from frontend
     *  */
    public void generateRootKeyStore(CertificateX500NameDTO dto) throws CertificateException, ParseException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        keyStoreWriter.loadKeyStore(null, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());

        certificateService.generateSelfSignedCertificate(dto);

        //sacuvaj stanje keystora
        keyStoreWriter.saveKeyStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
    }

    public void generateCAKeyStore(CertificateX500NameDTO dto) {
        keyStoreWriter.loadKeyStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());

        try {
            certificateService.generateCACertificate(dto);
        } catch(Exception e) {
            e.printStackTrace();
        }
        //sacuvaj stanje keystora
        keyStoreWriter.saveKeyStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());
    }

    /**
     * Metoda pravi end-entity.jks, ako ga nema, i ubacuje u njega jedan self-signed sertifikat
     *
     *  */
    public void generateEndEntityKeyStore(EndEntityDTO ee_dto) throws CertificateException, ParseException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
    	System.out.println("==== MAKING NEW END ENTITY CERTIFICATE ====");

    	keyStoreWriter.loadKeyStore(null, KeyStoreConfig.END_ENTITY_KEYSTORE_PASSWORD.toCharArray());

    	CertificateX500NameDTO subject = ee_dto.getSubject();
    	CertificateX500NameDTO issuer = ee_dto.getIssuer();

        try {
			certificateService.generateEndEntityCertificate(subject, issuer);
		} catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldn't make end entity certificate");
		}

        //sacuvaj stanje keystora
        keyStoreWriter.saveKeyStore(KeyStoreConfig.END_ENTITY_KEYSTORE_LOCATION, KeyStoreConfig.END_ENTITY_KEYSTORE_PASSWORD.toCharArray());
    }


    /**
     * Returns a X509 Certificate from selected keystore
     * @param alias: Serial number
     * @param role: Where to search the keystore
     * */
    public X509Certificate getCertificate(String alias, Role role) {

        String keyStoreLocation = "";
        String keyStorePassword = "";

        if(role == Role.ROOT) {
            keyStoreLocation = KeyStoreConfig.ROOT_KEYSTORE_LOCATION;
            keyStorePassword = KeyStoreConfig.ROOT_KEYSTORE_PASSWORD;
        } else if(role == Role.INTERMEDIATE) {
            keyStoreLocation = KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION;
            keyStorePassword = KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD;
        } else {
            keyStoreLocation = KeyStoreConfig.END_ENTITY_KEYSTORE_LOCATION;
            keyStorePassword = KeyStoreConfig.END_ENTITY_KEYSTORE_PASSWORD;
        }

        return (X509Certificate) keyStoreReader.readCertificate(keyStoreLocation, keyStorePassword, alias);
    }


    /** returns null if it can't find the certificate */
    public X509Certificate findCertificate(String alias) {

        X509Certificate x509 = (X509Certificate) keyStoreReader.readCertificate(KeyStoreConfig.ROOT_KEYSTORE_LOCATION, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD, alias);

        if(x509 == null) {
            x509 = (X509Certificate) keyStoreReader.readCertificate(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD, alias);
        }

        if(x509 == null) {
            x509 = (X509Certificate) keyStoreReader.readCertificate(KeyStoreConfig.END_ENTITY_KEYSTORE_LOCATION, KeyStoreConfig.END_ENTITY_KEYSTORE_PASSWORD, alias);
        }

        return x509;
    }
}
