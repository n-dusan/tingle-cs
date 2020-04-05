package com.tingle.tingle.service;

import com.tingle.tingle.config.CertificateConfig;
import com.tingle.tingle.config.KeyStoreConfig;
import com.tingle.tingle.util.keystores.KeyStoreReader;
import com.tingle.tingle.util.keystores.KeyStoreWriter;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.enums.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
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
     * @param: dto: DTO sent from frontend
     * TODO: nova metoda za dodavanje roota,  iskomentarisati ovaj loadKeyStore i umesto null usmeriti putanju ka već kreiranom
     * root keystore fajlu
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
    	
    	//Mislim da ovde ne loaduje end-entity.jks (tj ne napravi ga ako ne postoji)
    	keyStoreWriter.loadKeyStore(null, KeyStoreConfig.END_ENTITY_KEYSTORE_PASSWORD.toCharArray());

    	CertificateX500NameDTO subject = ee_dto.getSubject();
    	CertificateX500NameDTO issuer = ee_dto.getIssuer();

        try {
			certificateService.generateEndEntityCertificate(subject, issuer);
		} catch (Exception e) {
			System.out.println("ERROR");
			e.printStackTrace();
		}

        //sacuvaj stanje keystora
        keyStoreWriter.saveKeyStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
    }
}
