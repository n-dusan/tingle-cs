package com.tingle.tingle.util;

import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.springframework.stereotype.Component;

import javax.naming.InvalidNameException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

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


    /** Creates a certificate chain
     *
     * @param startingPoint the X509Certificate for which we want to find
     *                      ancestors
     *
     * @param certificates  A pool of certificates in which we expect to find
     *                      the startingPoint's ancestors.
     *
     * @return Array of X509Certificates, starting with the "startingPoint" and
     *         ending with highest level ancestor we could find in the supplied
     *         collection.
     */
    public static X509Certificate[] buildPath(
            X509Certificate startingPoint, Collection certificates
    ) throws NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, CertificateException {

        LinkedList path = new LinkedList();
        path.add(startingPoint);
        boolean nodeAdded = true;
        // Keep looping until an iteration happens where we don't add any nodes
        // to our path.
        while (nodeAdded) {
            // We'll start out by assuming nothing gets added.  If something
            // gets added, then nodeAdded will be changed to "true".
            nodeAdded = false;
            X509Certificate top = (X509Certificate) path.getLast();
            if (isSelfSigned(top)) {
                // We're self-signed, so we're done!
                break;
            }

            // Not self-signed.  Let's see if we're signed by anyone in the
            // collection.
            Iterator it = certificates.iterator();
            while (it.hasNext()) {
                X509Certificate x509 = (X509Certificate) it.next();
                if (verify(top, x509.getPublicKey())) {
                    // We're signed by this guy!  Add him to the chain we're
                    // building up.
                    path.add(x509);
                    nodeAdded = true;
                    it.remove(); // Not interested in this guy anymore!
                    break;
                }
                // Not signed by this guy, let's try the next guy.
            }
        }
        X509Certificate[] results = new X509Certificate[path.size()];
        path.toArray(results);
        return results;
    }

    public static boolean isSelfSigned(X509Certificate cert)
            throws CertificateException, InvalidKeyException,
            NoSuchAlgorithmException, NoSuchProviderException {

        return verify(cert, cert.getPublicKey());
    }

    public static boolean verify(X509Certificate cert, PublicKey key)
            throws CertificateException, InvalidKeyException,
            NoSuchAlgorithmException, NoSuchProviderException {

        String sigAlg = cert.getSigAlgName();
        String keyAlg = key.getAlgorithm();
        sigAlg = sigAlg != null ? sigAlg.trim().toUpperCase() : "";
        keyAlg = keyAlg != null ? keyAlg.trim().toUpperCase() : "";
        if (keyAlg.length() >= 2 && sigAlg.endsWith(keyAlg)) {
            try {
                cert.verify(key);
                return true;
            } catch (SignatureException se) {
                return false;
            }
        } else {
            return false;
        }
    }
}
