package com.tingle.tingle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tingle.tingle.domain.Certificate;


@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
}
