package com.tingle.tingle.domain.certificates;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Component;

import com.tingle.tingle.domain.dto.BasicConstraintsDTO;
import com.tingle.tingle.domain.dto.ExtensionsDTO;
import com.tingle.tingle.domain.dto.KeyUsageDTO;

@Component
public class CertificateGenerator {

    public CertificateGenerator() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public X509Certificate generateCertificate(SubjectData subjectData, IssuerData issuerData, ExtensionsDTO extensions) {
        try {

            JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
            builder = builder.setProvider("BC");

            ContentSigner contentSigner = builder.build(issuerData.getPrivateKey());

            X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(issuerData.getX500name(),
                    new BigInteger(subjectData.getSerialNumber()),
                    subjectData.getStartDate(),
                    subjectData.getEndDate(),
                    subjectData.getX500name(),
                    subjectData.getPublicKey());
            X509CertificateHolder certHolder = certGen.build(contentSigner);

            //Ekstenzije za sertifikate (namenu i da li je CA ili nije)
            // Basic Constraints
            BasicConstraintsDTO basicConstraintsDTO = extensions.getBasicConstraints();
            KeyUsageDTO keyUsageDTO = extensions.getKeyUsage();
            
            BasicConstraints basicConstraints = new BasicConstraints(basicConstraintsDTO.isCertificateAuthority()); // <-- true for CA, false for EndEntity

            certGen.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), basicConstraintsDTO.isCritical(), basicConstraints); // Basic Constraints is usually marked as critical.
            
            // loop through key usages and add them to cert
            ArrayList<KeyUsage> keyUsages = getListOfKeyUsages(keyUsageDTO);
            boolean isKeyUsageCritical = keyUsageDTO.isCritical();
            for(KeyUsage ku : keyUsages) {
            	certGen.addExtension(Extension.keyUsage, isKeyUsageCritical, ku);
            }
            
            JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
            certConverter = certConverter.setProvider("BC");

            return certConverter.getCertificate(certHolder);
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (OperatorCreationException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (CertIOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public KeyPair generateKeyPair(boolean isCertificateAuthority) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = new SecureRandom();

            //CA keysize is bigger for more secure private key
            if(isCertificateAuthority) {
                keyGen.initialize(4096, random);
            } else {
                keyGen.initialize(2048, random);
            }
            return keyGen.generateKeyPair();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    private ArrayList<KeyUsage> getListOfKeyUsages(KeyUsageDTO dto) {
    	ArrayList<KeyUsage> ret = new ArrayList<KeyUsage>();
    	
    	if(dto.isDigitalSignature()) {
    		ret.add(new KeyUsage(KeyUsage.digitalSignature));
    	}
    	if(dto.isNonRepudation()) {
    		ret.add(new KeyUsage(KeyUsage.nonRepudiation));
    	}
    	if(dto.isKeyEncipherment()) {
    		ret.add(new KeyUsage(KeyUsage.keyEncipherment));
    	}
    	if(dto.isDataEncipherment()) {
    		ret.add(new KeyUsage(KeyUsage.dataEncipherment));
    	}
    	if(dto.isKeyAgreement()) {
    		ret.add(new KeyUsage(KeyUsage.keyAgreement));
    	}
    	if(dto.isKeyCertSign()) {
    		ret.add(new KeyUsage(KeyUsage.keyCertSign));
    	}
    	if(dto.isCrlSign()) {
    		ret.add(new KeyUsage(KeyUsage.cRLSign));
    	}
    	if(dto.isEncipherOnly()) {
    		ret.add(new KeyUsage(KeyUsage.encipherOnly));
    	}
    	if(dto.isDecipherOnly()) {
    		ret.add(new KeyUsage(KeyUsage.decipherOnly));
    	}
    	
    	return ret;
    }
}
