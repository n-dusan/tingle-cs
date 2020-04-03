package com.tingle.tingle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.tingle.tingle.domain.Certificate;


@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {


    @Query(value="select c.* from certificate c where c.serial_number=?1", nativeQuery = true)
    public Certificate findCertificateBySerialNumber(String serialNumber);
}
