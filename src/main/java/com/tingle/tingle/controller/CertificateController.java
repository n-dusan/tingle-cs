package com.tingle.tingle.controller;

import com.tingle.tingle.config.keystores.KeyStoreConfig;
import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping(value="api/certificate")
public class CertificateController {


    @Autowired
    private CertificateService certificateService;

    @Autowired
    private KeyStoreConfig config;

    /**
     * @return list of all certificates */
    @GetMapping(value="/all")
    public ResponseEntity<List<CertificateDTO>> findAll() {
        return new ResponseEntity<List<CertificateDTO>>(certificateService.findAll(), HttpStatus.OK);
    }

    /**
     * @param: literally nothing
     * @return: list of all root certificates in the database, also, prints out in the console
     * the created certificate in string format
     * */
    @PutMapping(value="/new-root")
    public ResponseEntity<List<CertificateDTO>> makeNewRoot() {
        try {
            config.generateRootKeyStore();
            return new ResponseEntity<List<CertificateDTO>>(certificateService.findAll(), HttpStatus.OK);
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @PutMapping(value = "/new-ca/{alias}")
    public ResponseEntity<List<CertificateDTO>> makeNewCA(@PathVariable("alias") String alias) {
        try {
            config.generateCAKeyStore(alias);
            return new ResponseEntity<List<CertificateDTO>>(certificateService.findAll(), HttpStatus.OK);
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    
}
