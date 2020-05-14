package com.tingle.tingle.util.keystores;

import com.tingle.tingle.domain.certificates.IssuerData;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

@Component
public class KeyStoreReader {

    private KeyStore keyStore;



    public KeyStoreReader() {
        try {
            keyStore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }


    /**
     * Zadatak ove funkcije jeste da ucita podatke o izdavaocu i odgovarajuci privatni kljuc.
     * Ovi podaci se mogu iskoristiti da se novi sertifikati izdaju.
     *
     * @param keyStoreFile - datoteka odakle se citaju podaci
     * @param alias - alias putem kog se identifikuje sertifikat izdavaoca
     * @param password - lozinka koja je neophodna da se otvori key store
     * @param keyPass - lozinka koja je neophodna da se izvuce privatni kljuc
     * @return - podatke o izdavaocu i odgovarajuci privatni kljuc
     */
    public IssuerData readIssuerFromStore(String keyStoreFile, String alias, char[] password, char[] keyPass) {
        try {
            //Datoteka se ucitava
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(keyStoreFile));
            keyStore.load(in, password);
            //Iscitava se sertifikat koji ima dati alias
            Certificate cert = keyStore.getCertificate(alias);
            //Iscitava se privatni kljuc vezan za javni kljuc koji se nalazi na sertifikatu sa datim aliasom
            PrivateKey privKey = (PrivateKey) keyStore.getKey(alias, keyPass);
            X500Name issuerName = null;
            try {
                 issuerName = new JcaX509CertificateHolder((X509Certificate) cert).getSubject();
            } catch(Exception e) {
                return null;

            }

            return new IssuerData(privKey, issuerName, cert.getPublicKey());
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (CertificateException e) {
            e.printStackTrace();
            return null;
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch(NullPointerException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Ucitava sertifikat is KS fajla
     */
    public Certificate readCertificate(String keyStoreFile, String keyStorePass, String alias) {

        Certificate cert = null;

        try {
            //kreiramo instancu KeyStore
            KeyStore ks = KeyStore.getInstance("PKCS12");
            //ucitavamo podatke
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(keyStoreFile));
            ks.load(in, keyStorePass.toCharArray());

            if(ks.isKeyEntry(alias)) {
                cert = ks.getCertificate(alias);
                return cert;
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return cert;
        }
    }

    /**
     * Ucitava privatni kljuc is KS fajla
     */
    public PrivateKey readPrivateKey(String keyStoreFile, String keyStorePass, String alias, String pass) {
        try {
            //kreiramo instancu KeyStore
            KeyStore ks = KeyStore.getInstance("PKCS12");
            //ucitavamo podatke
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(keyStoreFile));
            ks.load(in, keyStorePass.toCharArray());

            if(ks.isKeyEntry(alias)) {
                PrivateKey pk = (PrivateKey) ks.getKey(alias, pass.toCharArray());
                return pk;
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return null;
        }  catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (CertificateException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Method of listing all certificates in keyStore file, and their private keys
     */
    public List<Certificate> readAllCertificates(String keystoreFile, char[] password) {

        InputStream is = null;
        List<Certificate> certList = new ArrayList<Certificate>();
        try {
            File file = new File(keystoreFile);
            is = new FileInputStream(file);

            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(is, password);

            Enumeration<String> enumeration = ks.aliases();
            while(enumeration.hasMoreElements()) {
                String alias = enumeration.nextElement();
                Certificate certificate = ks.getCertificate(alias);
                certList.add(certificate);
                System.out.println("alias name: " + alias);
                System.out.println(certificate);
            }

        } catch (java.security.cert.CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't open " + keystoreFile + ". It doesn't exist!");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }  finally {
            if(null != is)
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            return certList;
        }


    }

    public KeyStore getKeyStore() {
        return keyStore;
    }
}
