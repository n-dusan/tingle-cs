package com.tingle.tingle.util;

import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.springframework.stereotype.Component;

import javax.naming.InvalidNameException;

@Component
public class CConverter {

    public CertificateX500NameDTO[] convertFromX500Principals(X500Name subjectDN, X500Name issuerDN) throws InvalidNameException {

        CertificateX500NameDTO[] issuerAndSubjectDTO = new CertificateX500NameDTO[2];

        X500Name[] x500principals = { issuerDN, subjectDN };


        for(int i = 0; i < x500principals.length; i++) {

            issuerAndSubjectDTO[i] = new CertificateX500NameDTO();

            RDN cn = x500principals[i].getRDNs(BCStyle.CN)[0];
            issuerAndSubjectDTO[i].setCN(IETFUtils.valueToString(cn.getFirst().getValue()));

            RDN c = x500principals[i].getRDNs(BCStyle.C)[0];
            issuerAndSubjectDTO[i].setC(IETFUtils.valueToString(c.getFirst().getValue()));

            RDN ou = x500principals[i].getRDNs(BCStyle.OU)[0];
            issuerAndSubjectDTO[i].setOU(IETFUtils.valueToString(ou.getFirst().getValue()));

            RDN o = x500principals[i].getRDNs(BCStyle.O)[0];
            issuerAndSubjectDTO[i].setO(IETFUtils.valueToString(o.getFirst().getValue()));

            RDN st = x500principals[i].getRDNs(BCStyle.ST)[0];
            issuerAndSubjectDTO[i].setST(IETFUtils.valueToString(st.getFirst().getValue()));

            RDN l = x500principals[i].getRDNs(BCStyle.L)[0];
            issuerAndSubjectDTO[i].setL(IETFUtils.valueToString(l.getFirst().getValue()));

            RDN e = x500principals[i].getRDNs(BCStyle.E)[0];
            issuerAndSubjectDTO[i].setE(IETFUtils.valueToString(e.getFirst().getValue()));

        }

        return issuerAndSubjectDTO;
    }
}
