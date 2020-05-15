package com.tingle.tingle.service;

import com.tingle.tingle.config.KeyStoreConfig;
import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.util.keystores.KeyStoreReader;
import com.tingle.tingle.util.keystores.KeyStoreWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrustStoreService {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private KeyStoreWriter keyStoreWriter;

    @Autowired
    private KeyStoreReader keyStoreReader;

    @Autowired
    private KeyStoreConfig config;


    public void save(List<String> serialNumbers) {
        List<X509Certificate> certificates = certificateService.findAllCertificates(serialNumbers);
        keyStoreWriter.loadKeyStore(null, config.getTrustStorePassword().toCharArray());
        for (X509Certificate certificate : certificates) {
            keyStoreWriter.setCertificateEntry(certificate.getSerialNumber().toString(), ((Certificate) certificate));
        }
        keyStoreWriter.saveKeyStore(config.getTrustStoreLocation(), config.getTrustStorePassword().toCharArray());
    }

    public List<CertificateDTO> getAll() {
        List<Certificate> certificates = keyStoreReader.readAllCertificates(
                config.getTrustStoreLocation(),
                config.getTrustStorePassword().toCharArray());

        @SuppressWarnings("unchecked")
        List<X509Certificate> x509CertificateList = (List<X509Certificate>)(List<?>) certificates;

        return x509CertificateList.stream().map(e -> new CertificateDTO(e.getSerialNumber().toString())).collect(Collectors.toList());
    }

}
