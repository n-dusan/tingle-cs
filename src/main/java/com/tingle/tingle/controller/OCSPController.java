package com.tingle.tingle.controller;

import com.tingle.tingle.domain.enums.OCSPResponse;
import com.tingle.tingle.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="api/ocsp")
public class OCSPController {

    @Autowired
    private CertificateService certificateService;

    @GetMapping(value="/check/{serialNumber}")
    public ResponseEntity<OCSPResponse> ocspCheck(@PathVariable("serialNumber") String serialNumber) {
        return new ResponseEntity<OCSPResponse>(certificateService.checkCertificate(serialNumber), HttpStatus.OK);
    }

}
