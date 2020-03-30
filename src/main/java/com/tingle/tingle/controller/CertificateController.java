package com.tingle.tingle.controller;

import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value="api/certificate")
public class CertificateController {


    @Autowired
    private CertificateService certificateService;

    /**
     * @return list of all certificates */
    @GetMapping(value="/all")
    public ResponseEntity<List<CertificateDTO>> findAll() {
        return new ResponseEntity<List<CertificateDTO>>(certificateService.findAll(), HttpStatus.OK);
    }
}
