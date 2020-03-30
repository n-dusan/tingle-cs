package com.tingle.tingle.repository;

import com.tingle.tingle.domain.CertificateSigningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateSigningRequestRepository extends JpaRepository<CertificateSigningRequest, Long> {
}
