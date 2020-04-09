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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public X509Certificate generateCertificate(SubjectData subjectData, IssuerData issuerData, ExtensionsDTO extensions) throws CertIOException {
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
            

            //Ekstenzije 
            BasicConstraintsDTO basicConstraintsDTO = extensions.getBasicConstraints();
            KeyUsageDTO keyUsageDTO = extensions.getKeyUsage();
            
            BasicConstraints basicConstraints = new BasicConstraints(basicConstraintsDTO.isCertificateAuthority()); // <-- true for CA, false for EndEntity
            certGen.addExtension(Extension.basicConstraints, basicConstraintsDTO.isCritical(), basicConstraints); // Basic Constraints is usually marked as critical.

            int usages = generateKeyUsages(extensions.getKeyUsage());
            KeyUsage keyUsage = new KeyUsage(usages);
            certGen.addExtension(Extension.keyUsage, keyUsageDTO.isCritical(), keyUsage);
            
            JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
            certConverter = certConverter.setProvider("BC");

            X509CertificateHolder certHolder = certGen.build(contentSigner);
            
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
    
    private int generateKeyUsages(KeyUsageDTO dto) {
    	Optional<Integer> ret;
    	
    	List<Integer> thoseWhoAreTrue = new ArrayList<Integer>();
    	if(dto.isDigitalSignature()) {
    		thoseWhoAreTrue.add(KeyUsage.digitalSignature);
    	}
    	if(dto.isNonRepudation()) {
    		thoseWhoAreTrue.add(KeyUsage.nonRepudiation);
    	}
    	if(dto.isKeyEncipherment()) {
    		thoseWhoAreTrue.add(KeyUsage.keyEncipherment);
    	}
    	if(dto.isDataEncipherment()) {
    		thoseWhoAreTrue.add(KeyUsage.dataEncipherment);
    	}
    	if(dto.isKeyAgreement()) {
    		thoseWhoAreTrue.add(KeyUsage.keyAgreement);
    	}
    	if(dto.isKeyCertSign()) {
    		thoseWhoAreTrue.add(KeyUsage.keyCertSign);
    	}
    	if(dto.isCrlSign()) {
    		thoseWhoAreTrue.add(KeyUsage.cRLSign);
    	}
    	if(dto.isEncipherOnly()) {
    		thoseWhoAreTrue.add(KeyUsage.encipherOnly);
    	}
    	if(dto.isDecipherOnly()) {
    		thoseWhoAreTrue.add(KeyUsage.decipherOnly);
    	}
    	
    	ret = thoseWhoAreTrue.stream().reduce((a,b)-> a | b);
    	
    	return ret.get();
    }
}
