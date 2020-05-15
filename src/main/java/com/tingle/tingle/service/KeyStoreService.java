package com.tingle.tingle.service;

import com.tingle.tingle.config.KeyStoreConfig;
import com.tingle.tingle.util.keystores.KeyStoreReader;
import com.tingle.tingle.util.keystores.KeyStoreWriter;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.enums.Role;

import org.bouncycastle.cert.CertIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.tingle.tingle.domain.dto.EndEntityDTO;

import javax.naming.InvalidNameException;


@Service
public class KeyStoreService {

    @Autowired
    private KeyStoreWriter keyStoreWriter;

    @Autowired
    private KeyStoreReader keyStoreReader;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private KeyStoreConfig config;



    public void saveCertificate(X509Certificate certificate, String alias, PrivateKey privateKey, Role role) {

        if(role == Role.ROOT) {
            keyStoreWriter.loadKeyStore(config.getRootKeyStoreLocation(), config.getRootKeyStorePassword().toCharArray());
            //certificate password is the same as keystore password
            keyStoreWriter.write(alias, privateKey, config.getRootKeyStorePassword().toCharArray(), certificate);
            keyStoreWriter.saveKeyStore(config.getRootKeyStoreLocation(), config.getRootKeyStorePassword().toCharArray());
        } else if(role == Role.INTERMEDIATE) {
            keyStoreWriter.loadKeyStore(config.getIntermediateKeyStoreLocation(), config.getIntermediateKeyStorePassword().toCharArray());
            keyStoreWriter.write(alias, privateKey, config.getIntermediateKeyStorePassword().toCharArray(), certificate);
            keyStoreWriter.saveKeyStore(config.getIntermediateKeyStoreLocation(), config.getIntermediateKeyStorePassword().toCharArray());
        } else {
            keyStoreWriter.loadKeyStore(config.getEndEntityKeyStoreLocation(), config.getEndEntityKeyStorePassword().toCharArray());
            keyStoreWriter.write(alias, privateKey, config.getEndEntityKeyStorePassword().toCharArray(), certificate);
            keyStoreWriter.saveKeyStore(config.getEndEntityKeyStoreLocation(), config.getEndEntityKeyStorePassword().toCharArray());
        }
    }


    public void downloadCertificate(X509Certificate certificate, String alias, PrivateKey privateKey) {
        keyStoreWriter.loadKeyStore(null, "password".toCharArray());
        keyStoreWriter.write(alias, privateKey, "password".toCharArray(), certificate);
        keyStoreWriter.saveKeyStore("./certificates/" + alias + ".p12", "password".toCharArray());
    }


    /**
     * @param certificateRole: Role from which the keystore will provide a list of certificates
     * forward null to get all certificates from all keystores
     * @return List<X509Certificate>: returns a X509 casted list of certificates from the appropriate keystore
     * */
    public List<X509Certificate> findKeyStoreCertificates(Role certificateRole) {

        List<Certificate> rootCerts = keyStoreReader.readAllCertificates(
                config.getRootKeyStoreLocation(), config.getRootKeyStorePassword().toCharArray());

        List<Certificate> intermediateCerts = keyStoreReader.readAllCertificates(
                config.getIntermediateKeyStoreLocation(), config.getIntermediateKeyStorePassword().toCharArray());

        List<Certificate> endEntityCerts = keyStoreReader.readAllCertificates(
                config.getEndEntityKeyStoreLocation(), config.getEndEntityKeyStorePassword().toCharArray());

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
     * Metoda pravi root .p12, ako ga nema, i ubacuje u njega jedan self-signed sertifikat
     * @param: dto: DTO sent from frontend
     *  */
    public void generateRootKeyStore(CertificateX500NameDTO dto) throws CertificateException, ParseException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        keyStoreWriter.loadKeyStore(null, config.getRootKeyStorePassword().toCharArray());

        try {
			certificateService.generateSelfSignedCertificate(dto);
		} catch (CertIOException e) {
			System.out.println("ERROR DURING GENERATING CERTIFICATE");
			e.printStackTrace();
		} catch (InvalidNameException e) {
            e.printStackTrace();
        }

        //sacuvaj stanje keystora
        keyStoreWriter.saveKeyStore(config.getRootKeyStoreLocation(), config.getRootKeyStorePassword().toCharArray());
    }

    public void generateCAKeyStore(CertificateX500NameDTO dto) {
        keyStoreWriter.loadKeyStore(config.getIntermediateKeyStoreLocation(), config.getIntermediateKeyStorePassword().toCharArray());

        try {
            certificateService.generateCACertificate(dto);
        } catch(Exception e) {
            e.printStackTrace();
        }
        //sacuvaj stanje keystora
        keyStoreWriter.saveKeyStore(config.getIntermediateKeyStoreLocation(), config.getIntermediateKeyStorePassword().toCharArray());
    }

    /**
     * Metoda pravi end-entity.jks, ako ga nema, i ubacuje u njega jedan self-signed sertifikat
     *
     *  */
    public void generateEndEntityKeyStore(EndEntityDTO ee_dto) throws CertificateException, ParseException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
    	System.out.println("==== MAKING NEW END ENTITY CERTIFICATE ====");

    	keyStoreWriter.loadKeyStore(null, config.getEndEntityKeyStorePassword().toCharArray());

    	CertificateX500NameDTO subject = ee_dto.getSubject();
    	CertificateX500NameDTO issuer = ee_dto.getIssuer();

        try {
			certificateService.generateEndEntityCertificate(subject, issuer);
		} catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldn't make end entity certificate");
		}

        //sacuvaj stanje keystora
        keyStoreWriter.saveKeyStore(config.getEndEntityKeyStoreLocation(), config.getEndEntityKeyStorePassword().toCharArray());
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
            keyStoreLocation = config.getRootKeyStoreLocation();
            keyStorePassword = config.getRootKeyStorePassword();
        } else if(role == Role.INTERMEDIATE) {
            keyStoreLocation = config.getIntermediateKeyStoreLocation();
            keyStorePassword = config.getIntermediateKeyStorePassword();
        } else {
            keyStoreLocation = config.getEndEntityKeyStoreLocation();
            keyStorePassword = config.getEndEntityKeyStorePassword();
        }

        return (X509Certificate) keyStoreReader.readCertificate(keyStoreLocation, keyStorePassword, alias);
    }


    /** returns null if it can't find the certificate */
    public X509Certificate findCertificate(String alias) {

        X509Certificate x509 = (X509Certificate) keyStoreReader.readCertificate(
                config.getRootKeyStoreLocation(),
                config.getRootKeyStorePassword(),
                alias);

        if(x509 == null) {
            x509 = (X509Certificate) keyStoreReader.readCertificate(
                    config.getIntermediateKeyStoreLocation(),
                    config.getIntermediateKeyStorePassword(),
                    alias);
        }

        if(x509 == null) {
            x509 = (X509Certificate) keyStoreReader.readCertificate(
                    config.getEndEntityKeyStoreLocation(),
                    config.getEndEntityKeyStorePassword(),
                    alias);
        }

        return x509;
    }
}
