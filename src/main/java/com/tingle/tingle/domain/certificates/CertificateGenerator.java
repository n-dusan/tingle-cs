package com.tingle.tingle.domain.certificates;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.tingle.tingle.config.KeyStoreConfig;
import com.tingle.tingle.domain.dto.*;
import com.tingle.tingle.domain.enums.Role;
import com.tingle.tingle.util.CConverter;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.InvalidNameException;

@Component
public class CertificateGenerator {

	@Autowired
	private CConverter cConverter;

	@Autowired
	private KeyStoreConfig config;

    public CertificateGenerator() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public X509Certificate generateCertificate(SubjectData subjectData, IssuerData issuerData, ExtensionsDTO extensions) throws CertIOException, InvalidNameException {
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
            BasicConstraints basicConstraints = new BasicConstraints(basicConstraintsDTO.isCertificateAuthority()); // <-- true for CA, false for EndEntity
            certGen.addExtension(Extension.basicConstraints, basicConstraintsDTO.isCritical(), basicConstraints); // Basic Constraints is usually marked as critical.

            KeyUsageDTO keyUsageDTO = extensions.getKeyUsage();
            int usages = generateKeyUsages(extensions.getKeyUsage());
            KeyUsage keyUsage = new KeyUsage(usages);
            certGen.addExtension(Extension.keyUsage, keyUsageDTO.isCritical(), keyUsage);

            //### necessary for extended key usages
			ExtendedKeyUsageDTO extendedKeyUsageDTO;
			if(extensions.getExtendedKeyUsage() == null) {
				extendedKeyUsageDTO = new ExtendedKeyUsageDTO();
			} else {
				extendedKeyUsageDTO = extensions.getExtendedKeyUsage();
			}

//			extendedKeyUsageDTO.setClientAuth(true);
//			extendedKeyUsageDTO.setServerAuth(true);
			KeyPurposeId[] extendedUsages = generateExtendedKeyUsage(extendedKeyUsageDTO);
			ExtendedKeyUsage extendedKeyUsage = new ExtendedKeyUsage(extendedUsages);
			certGen.addExtension(Extension.extendedKeyUsage, extendedKeyUsageDTO.isCritical(), extendedKeyUsage);

            //subject key identifier
			//The SubjectKeyIdentifier extension is a standard X509v3 extension which MUST NOT be marked as being critical. .
			JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
            SubjectKeyIdentifier subjectKeyIdentifier = extensionUtils.createSubjectKeyIdentifier(subjectData.getPublicKey());
			certGen.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);


			//authority key identifier
			//if publicKey isn't null means we aren't a self-signed certificate, so we fill out the authority key identifier
			AuthorityKeyIdentifier authorityKeyIdentifier;
			if(issuerData.getPublicKey() != null) {
				authorityKeyIdentifier = extensionUtils.createAuthorityKeyIdentifier(issuerData.getPublicKey());
			} else {
				authorityKeyIdentifier = extensionUtils.createAuthorityKeyIdentifier(subjectData.getPublicKey());

			}
			certGen.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);



			//##### subjectAlternativeName
			// name that points to localhost
			List<GeneralName> altNames = new ArrayList<GeneralName>();
			//get the CN from the existing form, there you will find the commonName that's supposed to associate to DN's
			CertificateX500NameDTO[] x509dto = cConverter.convertFromX500Principals(subjectData.getX500name(), subjectData.getX500name());


			altNames.add(new GeneralName(GeneralName.dNSName, x509dto[0].getCN()));
			altNames.add(new GeneralName(GeneralName.dNSName, "localhost"));
			altNames.add(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
			GeneralNames subjectAltNames = GeneralNames.getInstance(new DERSequence((GeneralName[]) altNames.toArray(new GeneralName[] {})));
			certGen.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);

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
		} catch (NoSuchAlgorithmException e) {
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
    
    private KeyPurposeId[] generateExtendedKeyUsage(ExtendedKeyUsageDTO dto) {
    	
    	List<KeyPurposeId> thoseWhoAreTrue = new ArrayList<KeyPurposeId>();
    	if(dto.isClientAuth()) {
    		thoseWhoAreTrue.add(KeyPurposeId.id_kp_clientAuth);
    	}
    	if(dto.isCodeSigning()) {
    		thoseWhoAreTrue.add(KeyPurposeId.id_kp_codeSigning);
    	}
    	if(dto.isEmailProtection()) {
    		thoseWhoAreTrue.add(KeyPurposeId.id_kp_emailProtection);
    	}
    	if(dto.isOcspSigning()) {
    		thoseWhoAreTrue.add(KeyPurposeId.id_kp_OCSPSigning);
    	}
    	if(dto.isServerAuth()) {
    		thoseWhoAreTrue.add(KeyPurposeId.id_kp_serverAuth);
    	}
    	if(dto.isTimeStamping()) {
    		thoseWhoAreTrue.add(KeyPurposeId.id_kp_timeStamping);
    	}
    	
    	KeyPurposeId[] ret = new KeyPurposeId[thoseWhoAreTrue.size()];
    	thoseWhoAreTrue.toArray(ret); // fill the array
    	return ret;
    }


	public SubjectData generateSubjectData(CertificateX500NameDTO dto) throws ParseException {

		// based on role, generate RSA key pair length
		KeyPair keyPair;

		if (dto.getCertificateRole() == Role.ROOT || dto.getCertificateRole() == Role.INTERMEDIATE) {
			keyPair = generateKeyPair(true);
		} else {
			keyPair = generateKeyPair(false);
		}

		// Datumi od kad do kad vazi sertifikat
		Calendar cal = new GregorianCalendar();
		SimpleDateFormat iso8601Formater = new SimpleDateFormat("dd-MM-yyyy");
		iso8601Formater.setTimeZone(cal.getTimeZone());

		String startDateString = iso8601Formater.format(cal.getTime());

		// based on role, set certificate expiration date
		if (dto.getCertificateRole() == Role.ROOT) {
			cal.add(Calendar.YEAR, config.getRootYears());
		} else if (dto.getCertificateRole() == Role.INTERMEDIATE) {
			cal.add(Calendar.YEAR, config.getIntermediateYears());
		} else {
			cal.add(Calendar.YEAR, config.getEndEntityYears());
		}

		String endDateString = iso8601Formater.format(cal.getTime());

		System.out.println("Start date: " + startDateString);
		System.out.println("End date: " + endDateString);

		Date startDate = iso8601Formater.parse(startDateString);
		Date endDate = iso8601Formater.parse(endDateString);

		// Serial number je vazan, za sad JAKO VELIKI random broj
		BigInteger upperLimit = new BigInteger("1000000000000");
		BigInteger randomNumber;
		do {
			randomNumber = new BigInteger(upperLimit.bitLength(), new SecureRandom());
		} while (randomNumber.compareTo(upperLimit) >= 0);

		String serialNumber = String.valueOf(randomNumber);

		X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
		builder.addRDN(BCStyle.CN, dto.getCN());
		builder.addRDN(BCStyle.L, dto.getL());
		builder.addRDN(BCStyle.ST, dto.getST());
		builder.addRDN(BCStyle.O, dto.getO());
		builder.addRDN(BCStyle.OU, dto.getOU());
		builder.addRDN(BCStyle.C, dto.getC());
		builder.addRDN(BCStyle.E, dto.getE());
		builder.addRDN(BCStyle.UID, "123456");

		return new SubjectData(keyPair.getPublic(), builder.build(), serialNumber, startDate, endDate,
				keyPair.getPrivate());
	}

	/**
	 * @param dto:        DTO sa fronta
	 * @param privateKey: privatni kljuc issuera generisan negde van ove metode i
	 *                    pripojen sertifikatu koji se skladisti
	 */
	public IssuerData generateIssuerData(CertificateX500NameDTO dto, PrivateKey privateKey) {

		X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

		builder.addRDN(BCStyle.CN, dto.getCN());
		builder.addRDN(BCStyle.O, dto.getO());
		builder.addRDN(BCStyle.OU, dto.getOU());
		builder.addRDN(BCStyle.C, dto.getC());
		builder.addRDN(BCStyle.E, dto.getE());
		builder.addRDN(BCStyle.L, dto.getL());
		builder.addRDN(BCStyle.ST, dto.getST());
		// TODO: id usera koji kreira zahtev, sad je hardkodovano
		builder.addRDN(BCStyle.UID, "123456");

		return new IssuerData(privateKey, builder.build());
	}


	public BasicConstraintsDTO generateBasicConstraints(X509Certificate certificate)
			throws CertificateEncodingException {
		BasicConstraintsDTO ret = new BasicConstraintsDTO();
		Extension basicConstraints = new JcaX509CertificateHolder(certificate).getExtension(Extension.basicConstraints);
		BasicConstraints bc = BasicConstraints.getInstance(basicConstraints.getExtnValue().getOctets());
		ret.setCritical(basicConstraints.isCritical());
		ret.setCertificateAuthority(bc.isCA());

		return ret;
	}

	public KeyUsageDTO generateKeyUsage(X509Certificate certificate) throws CertificateEncodingException {
		Extension keyUsage = new JcaX509CertificateHolder(certificate).getExtension(Extension.keyUsage);
		KeyUsageDTO ret = new KeyUsageDTO();
		ret.setCritical(keyUsage.isCritical());
		ret.setUsages(certificate.getKeyUsage());

		return ret;
	}

	public ExtendedKeyUsageDTO generateExtendedKeyUsage(X509Certificate certificate) throws Exception {
		Extension extendedKeyUsage = new JcaX509CertificateHolder(certificate).getExtension(Extension.extendedKeyUsage);
		ExtendedKeyUsageDTO ret = new ExtendedKeyUsageDTO();
		ret.setCritical(extendedKeyUsage.isCritical());
		ret.setUsages(certificate.getExtendedKeyUsage());

		return ret;
	}

}
