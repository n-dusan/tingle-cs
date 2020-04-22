package com.tingle.tingle.controller;

import java.io.FileNotFoundException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.List;

import javax.naming.InvalidNameException;

import com.tingle.tingle.domain.enums.OCSPResponse;
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

import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.dto.EndEntityDTO;
import com.tingle.tingle.domain.enums.Role;
import com.tingle.tingle.service.CertificateService;
import com.tingle.tingle.service.KeyStoreService;

@RestController
@RequestMapping(value="api/certificate")
public class CertificateController {


    @Autowired
    private CertificateService certificateService;

    @Autowired
    private KeyStoreService keyStoreService;

    /**
     * @return list of all certificates */
    @GetMapping(value="/all")
    public ResponseEntity<List<CertificateDTO>> findAll() {
        return new ResponseEntity<List<CertificateDTO>>(certificateService.findAll(), HttpStatus.OK);
    }

    /**
     * Makes a new root.jks file and adds a self-signed certificate
     * @param: dto: DTO sent from frontend
     * @return: list of all root certificates in the database, also prints out in the console
     * the created certificate in string format
     * */
    @PostMapping(value="/new/root", consumes="application/json")
    public ResponseEntity<List<CertificateDTO>> makeNewRoot(@RequestBody CertificateX500NameDTO dto) {
        try {
            keyStoreService.generateRootKeyStore(dto);
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
    
    @PostMapping(value = "/new/ca")
    public ResponseEntity<List<CertificateDTO>> makeNewCA(@RequestBody CertificateX500NameDTO dto) {
        keyStoreService.generateCAKeyStore(dto);
        return new ResponseEntity<List<CertificateDTO>>(certificateService.findAll(), HttpStatus.OK);
    }

    /**
     * Endpoint for getting certificate issuer and subject data
     *
     * @param serialNumber: Serial number for a concrete certificate
     * @param certificateRole: Role of the certificate
     *
     * CertificateX500NameDTO[0] - Issuer
     * CertificateX500NameDTO[1] - Subject
     * */

    @GetMapping(value="/get/{serialNumber}/{certificateRole}")
    public ResponseEntity<CertificateX500NameDTO[]> getCertificateX500IssuerAndSubject(@PathVariable("serialNumber") String serialNumber,
                           @PathVariable("certificateRole") Role certificateRole) {
        CertificateX500NameDTO[] certs = this.certificateService.getCertificateIssuerAndSubjectData(serialNumber, certificateRole);

        if(certs == null) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        return new ResponseEntity<CertificateX500NameDTO[]>(certs, HttpStatus.OK);
    }


    /**
     * Endpoint for fetching all Certificate Authority certificates.
     * Important params in DTO: serialNumber and role.
     * That combination can be used to get concrete certificate from /get/{serialNumber}/{certificateRole} endpoint.
     * */

    @GetMapping(value="/all/ca")
    public ResponseEntity<List<CertificateX500NameDTO>> getAllCACertificateX500() throws FileNotFoundException, InvalidNameException {
        List<CertificateX500NameDTO> certs = null;
        try {
            certs = this.certificateService.getCertificateCASubjectData();
            if(certs == null) {
                return new ResponseEntity<>(HttpStatus.OK);
            }
            return new ResponseEntity<List<CertificateX500NameDTO>>(certs, HttpStatus.OK);
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Makes a new end-entity.jks file and adds a end-entity certificate
     * @param: dto from front-end
     * @return: list of all certificates in the database, also prints out in the console
     * the created certificate in string format
     * */
    @PostMapping(value="/new/end-entity", consumes="application/json")
    public ResponseEntity<List<CertificateDTO>> makeNewEndEntity(@RequestBody EndEntityDTO endEntityCertificate) {
        try {
            keyStoreService.generateEndEntityKeyStore(endEntityCertificate);
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
    
    @GetMapping(value = "/downloadCertificate/{serialNumber}", produces = "application/json")
    	public ResponseEntity<Boolean> downloadCert(@PathVariable String serialNumber) {
    		
    		Boolean ret = certificateService.downloadCertificate(serialNumber);
    		if(ret) {
    		    return new ResponseEntity<Boolean>(ret, HttpStatus.OK);
            }
    		    return new ResponseEntity<Boolean>(ret, HttpStatus.INTERNAL_SERVER_ERROR);

    	}

    @PutMapping(value="/revoke", consumes="application/json")
    public ResponseEntity<String> revoke(@RequestBody CertificateX500NameDTO certificate) {
        try {
            this.certificateService.revokeCertificate(certificate.getSerialNumber(), certificate.getRevokationReason());
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
