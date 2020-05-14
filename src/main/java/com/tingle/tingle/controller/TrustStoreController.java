package com.tingle.tingle.controller;

import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.domain.dto.TrustRequestDTO;
import com.tingle.tingle.service.TrustStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="api/trust-store")
public class TrustStoreController {

    @Autowired
    private TrustStoreService trustStoreService;

    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> save(@RequestBody TrustRequestDTO request) {
        trustStoreService.save(request.getSerials());
        return new ResponseEntity<List<CertificateDTO>>(trustStoreService.getAll(), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<CertificateDTO>> getAll() {
        return new ResponseEntity<List<CertificateDTO>>(trustStoreService.getAll(), HttpStatus.OK);
    }
}
