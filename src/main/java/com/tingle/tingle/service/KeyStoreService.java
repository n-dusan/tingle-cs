package com.tingle.tingle.service;

import com.tingle.tingle.config.keystores.KeyStoreConfig;
import com.tingle.tingle.config.keystores.KeyStoreWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Service
public class KeyStoreService {

    @Autowired
    private KeyStoreWriter keyStoreWriter;


    public void saveSelfSignedCertificate(X509Certificate certificate, String alias, PrivateKey privateKey) {
        keyStoreWriter.loadKeyStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
        //certificate password is the same as keystore password
        keyStoreWriter.write(alias, privateKey, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray(), certificate);
        keyStoreWriter.saveKeyStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
    }


}
