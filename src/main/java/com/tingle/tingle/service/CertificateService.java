package com.tingle.tingle.service;

import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.repository.CertificateRepository;
import org.apache.catalina.filters.RemoteIpFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tingle.tingle.domain.Certificate;

import java.util.ArrayList;
import java.util.List;

@Service
public class CertificateService {

    @Autowired
    private CertificateRepository certificateRepository;

    public List<CertificateDTO> findAll() {
        List<Certificate> certificates = certificateRepository.findAll();
        List<CertificateDTO> dtoList = new ArrayList<CertificateDTO>();

        for (Certificate certificate : certificates) {
            CertificateDTO dto = new CertificateDTO(
                    certificate.getId(),
                    certificate.getSerialNumber(),
                    certificate.getAlias(),
                    certificate.isActive(),
                    certificate.getCertificateRole()
            );

            dtoList.add(dto);
        }

        return dtoList;
    }
}
