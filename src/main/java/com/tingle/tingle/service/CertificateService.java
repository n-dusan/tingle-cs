package com.tingle.tingle.service;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.InvalidNameException;

import com.tingle.tingle.exception.ValidateException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.CertIOException;

import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tingle.tingle.config.KeyStoreConfig;
import com.tingle.tingle.domain.Certificate;
import com.tingle.tingle.domain.certificates.CertificateGenerator;
import com.tingle.tingle.domain.certificates.IssuerData;
import com.tingle.tingle.domain.certificates.SubjectData;
import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.dto.ExtensionsDTO;
import com.tingle.tingle.domain.enums.CRLReason;
import com.tingle.tingle.domain.enums.OCSPResponse;
import com.tingle.tingle.domain.enums.Role;
import com.tingle.tingle.repository.CertificateRepository;
import com.tingle.tingle.util.CConverter;
import com.tingle.tingle.util.keystores.KeyStoreReader;

@Service
public class CertificateService {

	@Autowired
	private KeyStoreConfig config;

	@Autowired
	private CertificateRepository certificateRepository;

	@Autowired
	private CertificateGenerator certificateGenerator;

	@Autowired
	private KeyStoreService keyStoreService;

	@Autowired
	private KeyStoreReader keyStoreReader;

	@Autowired
	private CConverter converter;

	public List<CertificateDTO> findAll() {

		List<Certificate> certificates = certificateRepository.findAll();

		return certificates.stream().map(e -> new CertificateDTO(e.getId(), e.getSerialNumber(), e.isActive(),
				e.getCertificateRole(), e.getRevokeReason())).collect(Collectors.toList());
	}

	public List<CertificateDTO> findAllActive() {
		List<Certificate> certificates = certificateRepository.findAllActive();

		return certificates.stream().map(e -> new CertificateDTO(e.getId(), e.getSerialNumber(), e.isActive(),
				e.getCertificateRole(), e.getRevokeReason())).collect(Collectors.toList());
	}

	/**
	 * @param serialNumber:    Serial number of a certificate that's being converted
	 * @param certificateRole: ROOT;INTERMEDIATE;END_ENTITY, used for searching the
	 *                         corresponding keystore
	 *
	 * @return DTO Array where [0] element is Issuer DTO, and [1] element is
	 *         SubjectDTO
	 *
	 *         can be used for displaying basic issuer data in the form
	 *
	 */
	public CertificateX500NameDTO[] getCertificateIssuerAndSubjectData(String serialNumber, Role certificateRole) {
		try {
			X509Certificate certificate = keyStoreService.getCertificate(serialNumber, certificateRole);

			X500Name subjName = new JcaX509CertificateHolder(certificate).getSubject();
			X500Name issuerName = new JcaX509CertificateHolder(certificate).getIssuer();

			ExtensionsDTO extensionsDTO = new ExtensionsDTO();
			extensionsDTO.setBasicConstraints(certificateGenerator.generateBasicConstraints(certificate));
			extensionsDTO.setKeyUsage(certificateGenerator.generateKeyUsage(certificate));

			if (certificateRole == Role.END_ENTITY) {
				extensionsDTO.setExtendedKeyUsage(certificateGenerator.generateExtendedKeyUsage(certificate));
			}

			CertificateX500NameDTO[] x509dto = converter.convertFromX500Principals(subjName, issuerName);
			x509dto[1].setExtensions(extensionsDTO);
			x509dto[1].setCertificateRole(certificateRole);
			x509dto[1].setSerialNumber(serialNumber);

			return x509dto;
		} catch (InvalidNameException e) {
			e.printStackTrace();
		} catch (CertificateEncodingException e) {
			e.printStackTrace();
		} catch (CertificateParsingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns data of all CA's in the system. validated :)
	 */
	public List<CertificateX500NameDTO> getCertificateCASubjectData()
			throws InvalidNameException, CertificateException {

		List<X509Certificate> cAJoinedList = findCACertificates();

		if (cAJoinedList == null) {
			return null;
		}
		cAJoinedList = cAJoinedList.stream().distinct().collect(Collectors.toList());

		List<CertificateX500NameDTO> cADTOList = new ArrayList<CertificateX500NameDTO>();

		for (X509Certificate x509Certificate : cAJoinedList) {
			String serialNumber = x509Certificate.getSerialNumber().toString();
			Certificate repositoryCertificate = certificateRepository.findCertificateBySerialNumber(serialNumber);
			if (repositoryCertificate != null && repositoryCertificate.getCertificateRole() != Role.END_ENTITY) {
				// if this certificate isn't valid, don't append it
				if (!validate(x509Certificate))
					continue;

				// set ISSUER data -> chain implementation. USE THE CHAIN LUKE. THE CHAIN IS
				// WITHIN YOU.
				// X509Certificate chain[] = converter.buildPath(x509Certificate, cAJoinedList);
				// x509dto[0].setSerialNumber(chain[1].getSerialNumber().toString());

				X500Name subjName = new JcaX509CertificateHolder(x509Certificate).getSubject();
				CertificateX500NameDTO[] x509dto = converter.convertFromX500Principals(subjName, subjName);

				x509dto[1].setSerialNumber(serialNumber);
				// Izvuci rolu sertifikata iz repository sertifikata
				x509dto[1].setCertificateRole(repositoryCertificate.getCertificateRole());
				cADTOList.add(x509dto[1]);
			} else {
				System.out.println("FOUND AN END ENTITY.");
			}
		}

		return cADTOList;
	}

	/**
	 * Metoda generiše self-signed sertifikat i čuva u odgovarajući .jks fajl
	 * (root.jks)
	 *
	 * @param dto: DTO primljen sa front-enda. Sadrzi sve podatke o kreiranju
	 *             sertifikata U ovom slucaju, issuer i subject su ista firma
	 * @throws CertIOException
	 */
	public void generateSelfSignedCertificate(CertificateX500NameDTO dto) throws ParseException, CertIOException, InvalidNameException {

		// keypair za subjekta i issuera je isti, jer je self-signed sertifikat
		SubjectData subject = certificateGenerator.generateSubjectData(dto);
		IssuerData issuer = certificateGenerator.generateIssuerData(dto, subject.getPrivateKey());

		// changed the last param from isCA to extensions
		X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, dto.getExtensions());

		System.out.println("\n===== Certificate issuer=====");
		System.out.println(cert.getIssuerX500Principal().getName());
		System.out.println("\n===== Certicate owner =====");
		System.out.println(cert.getSubjectX500Principal().getName());
		System.out.println("\n===== Certificate =====");

		// save the cert in the keystore
		keyStoreService.saveCertificate(cert, subject.getSerialNumber(), issuer.getPrivateKey(), Role.ROOT);

		// save the cert in the database -> used for revokation check
		this.certificateRepository.save(new Certificate(subject.getSerialNumber(), true, Role.ROOT));
	}

	public void generateCACertificate(CertificateX500NameDTO dto) throws Exception {

		List<X509Certificate> cAJoinedList = findCACertificates();

		if (cAJoinedList == null) {
			throw new Exception("There isn't any root!");
		}

//		//ne znam zasto mi duplira root u cA
//		cAJoinedList = cAJoinedList.stream()
//				.distinct()
//				.collect(Collectors.toList());

		for (X509Certificate x509Certificate : cAJoinedList) {
			String serialNumber = x509Certificate.getSerialNumber().toString();

			if (dto.getSerialNumber().equalsIgnoreCase(serialNumber)) {

				// prvo ga citaj iz roota
				IssuerData issuer;

				issuer = keyStoreReader.readIssuerFromStore(config.getRootKeyStoreLocation(), serialNumber,
						config.getRootKeyStorePassword().toCharArray(), config.getRootKeyStorePassword().toCharArray());
				if (issuer == null) {
					issuer = keyStoreReader.readIssuerFromStore(config.getIntermediateKeyStoreLocation(), serialNumber,
							config.getIntermediateKeyStorePassword().toCharArray(),
							config.getIntermediateKeyStorePassword().toCharArray());
				}

				SubjectData subject = certificateGenerator.generateSubjectData(dto);

				X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, dto.getExtensions());

				// TODO: verify
				if (!validate(cert)) {
					System.out.println("I somehow managed to invalidate a perfectly valid chain.");
				}

				System.out.println("\n===== Certificate issuer=====");
				System.out.println(cert.getIssuerX500Principal().getName());
				System.out.println("\n===== Certicate owner =====");
				System.out.println(cert.getSubjectX500Principal().getName());

				keyStoreService.saveCertificate(cert, subject.getSerialNumber(), subject.getPrivateKey(),
						Role.INTERMEDIATE);

				// save the cert in the database -> to be used when ocsp implementation occurs
				this.certificateRepository.save(new Certificate(subject.getSerialNumber(), true, Role.INTERMEDIATE));
				break;

			}
		}
	}

	public void generateEndEntityCertificate(CertificateX500NameDTO subjectDTO, CertificateX500NameDTO issuerDTO)
			throws ParseException, CertIOException, InvalidNameException {
		SubjectData subject = certificateGenerator.generateSubjectData(subjectDTO);

		IssuerData issuer;
		System.out.println("issuer dto " + issuerDTO.getSerialNumber());
		issuer = keyStoreReader.readIssuerFromStore(config.getRootKeyStoreLocation(), issuerDTO.getSerialNumber(),
				config.getRootKeyStorePassword().toCharArray(), config.getRootKeyStorePassword().toCharArray());
		if (issuer == null) {
			issuer = keyStoreReader.readIssuerFromStore(config.getIntermediateKeyStoreLocation(),
					issuerDTO.getSerialNumber(), config.getIntermediateKeyStorePassword().toCharArray(),
					config.getIntermediateKeyStorePassword().toCharArray());
		}

		X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, subjectDTO.getExtensions());

		System.out.println("\n===== Certificate issuer=====");
		System.out.println(cert.getIssuerX500Principal().getName());
		System.out.println("\n===== Certicate owner =====");
		System.out.println(cert.getSubjectX500Principal().getName());
		System.out.println("\n===== Certificate =====");
		System.out.println("-------------------------------------------------------");
		System.out.println(cert);
		System.out.println("-------------------------------------------------------");

		// save the cert in the keystore
		keyStoreService.saveCertificate(cert, subject.getSerialNumber(), subject.getPrivateKey(), Role.END_ENTITY);

        // save the cert in the database -> to be used when ocsp implementation occurs
		this.certificateRepository.save(new Certificate(subject.getSerialNumber(), true, Role.END_ENTITY));
		System.out.println("===================== SUCCESS =====================");
	}



	/**
	 * OCSP request validation, a client sends the certificates' serialNumber that
	 * he wants to validate OCSP Request service checks the database to find the
	 * certificate.
	 *
	 * @returns: OCSPResponse enum wih the certificate status
	 */
	public OCSPResponse checkCertificate(String serialNumber) {

		Certificate requestedCertificate = certificateRepository.findCertificateBySerialNumber(serialNumber);

		if (requestedCertificate == null) {
			return OCSPResponse.UNKNOWN;
		}

		else if (!requestedCertificate.isActive()) {
			return OCSPResponse.REVOKED;
		}

		return OCSPResponse.GOOD;
	}

	/** merge lista koja vraća sve cA i root sertifikate */
	public List<X509Certificate> findCACertificates() {
		List<X509Certificate> x509rootList = keyStoreService.findKeyStoreCertificates(Role.ROOT);

		// No roots, how can I issue anything without them?
		if (x509rootList.isEmpty()) {
			return null;
		}

		List<X509Certificate> x509intermediateList = keyStoreService.findKeyStoreCertificates(Role.INTERMEDIATE);

		return Stream.concat(x509rootList.stream(), x509intermediateList.stream()).collect(Collectors.toList());

	}

	/**
	 * - constructs a chain - verifies their signatures - checks if the first chain
	 * element is valid (if it isn't expired) - checks if the chain leads to root
	 */
	private boolean validate(X509Certificate certificate) {

		List<X509Certificate> cAJoined = findCACertificates();

		try {
			X509Certificate[] chain = CConverter.buildPath(certificate, cAJoined);

			try {
				// proverava da li je istekao sertifikat
				chain[0].checkValidity();
			} catch (CertificateExpiredException e) {
				return false;
			} catch (CertificateNotYetValidException e) {
				return false;
			}

			if (converter.isSelfSigned(chain[chain.length - 1])) {
				// ocsp chain validation -> check if any certificate in the chain is revoked
				for (X509Certificate x509Cert : chain) {
					OCSPResponse response = checkCertificate(x509Cert.getSerialNumber().toString());
					// if a node in chain is revoked or unknown -> drop the chain
					if (response != OCSPResponse.GOOD) {
						return false;
					}
				}
				System.out.println("==========I AM GROOT");

				//better exception handling, PLZ.
				boolean ret = validateExtensions(chain);

				return ret;
			}

		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}

		return false;
	}

	public Boolean downloadCertificate(String serialNumber) {

		java.security.cert.Certificate certificate = null;
		Role role = Role.ROOT;
		String path = "";

		// prvo ga trazi u rootu
		certificate = keyStoreReader.readCertificate(config.getRootKeyStoreLocation(), config.getRootKeyStorePassword(),
				serialNumber);
		// nema ga u rootu, onda mozda u cA?
		if (certificate == null) {
			certificate = keyStoreReader.readCertificate(config.getIntermediateKeyStoreLocation(),
					config.getIntermediateKeyStorePassword(), serialNumber);
			role = Role.INTERMEDIATE;
		}
		// nema ga ni u cA, onda je end entity.
		if (certificate == null) {
			certificate = keyStoreReader.readCertificate(config.getEndEntityKeyStoreLocation(),
					config.getEndEntityKeyStorePassword(), serialNumber);
			role = Role.END_ENTITY;
		}

		X509Certificate x509Certificate = (X509Certificate) certificate;

		PrivateKey privateKey;

		privateKey = keyStoreReader.readPrivateKey(config.getRootKeyStoreLocation(), config.getRootKeyStorePassword(),
				x509Certificate.getSerialNumber().toString(), config.getRootKeyStorePassword());

		if(privateKey == null) {
			privateKey = keyStoreReader.readPrivateKey(config.getIntermediateKeyStoreLocation(), config.getIntermediateKeyStorePassword(),
					x509Certificate.getSerialNumber().toString(), config.getIntermediateKeyStorePassword());
		}
		if(privateKey == null) {
			privateKey = keyStoreReader.readPrivateKey(config.getEndEntityKeyStoreLocation(), config.getEndEntityKeyStorePassword(),
					x509Certificate.getSerialNumber().toString(), config.getEndEntityKeyStorePassword());
		}


		path = "./certificates/" + role.toString() + "-" + serialNumber + ".key";
		try {
			PemWriter writer = new PemWriter(new FileWriter(path));
			writer.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		keyStoreService.downloadCertificate(x509Certificate, x509Certificate.getSerialNumber().toString(), privateKey);


		JcaPEMWriter pemWrt = null;
		try {

			path = "./certificates/" + role.toString() + "-" + serialNumber + ".crt";

			pemWrt = new JcaPEMWriter(new FileWriter(path));
			pemWrt.writeObject(x509Certificate);
			pemWrt.flush();
			pemWrt.close();



		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pemWrt != null) {
				return true;
			}
			return false;
		}

	}


		public void revokeCertificate(String serialNumber, CRLReason reason)
			throws NoSuchProviderException, CertificateException, NoSuchAlgorithmException, InvalidKeyException {

		List<Certificate> allActiveCertificates = certificateRepository.findAllActive();

		for (Certificate allActiveCertificate : allActiveCertificates) {

			X509Certificate x509 = keyStoreService.findCertificate(allActiveCertificate.getSerialNumber());
			List<X509Certificate> allCertificates = keyStoreService.findKeyStoreCertificates(null);

			X509Certificate[] chain = CConverter.buildPath(x509, allCertificates);

			for (int i = 0; i < chain.length; i++) {
				if (chain[i].getSerialNumber().toString().equals(serialNumber)) {
					for (int j = 0; j <= i; j++) {
						Certificate forRevoke = certificateRepository
								.findCertificateBySerialNumber(chain[j].getSerialNumber().toString());
						forRevoke.setActive(false);
						forRevoke.setRevokeReason(reason);
						this.certificateRepository.save(forRevoke);
					}
				}
			}

		}

	}

	/** TODO: better exception handling
	 * this should never actually happen.
	 * */
	private boolean validateExtensions(X509Certificate[] chain) throws CertificateEncodingException {

		if(chain.length > 1) {

			//skip first element
			for (int i = 1; i < chain.length; i++) {
				Extension basicConstraints = new JcaX509CertificateHolder(chain[i]).getExtension(Extension.basicConstraints);
				BasicConstraints bc = BasicConstraints.getInstance(basicConstraints.getExtnValue().getOctets());
				//if you aren't cA, are a parent node, and you're critical, you're out, kiddo.
				if(!bc.isCA() && basicConstraints.isCritical()) {
					return false;
				}
			}
		}

		return true;
	}



	public List<X509Certificate> findAllCertificates(List<String> serialNumbers) {
		List<X509Certificate> x509CertificateList = new ArrayList<>();
		for (String serialNumber : serialNumbers) {
			X509Certificate x509 = keyStoreService.findCertificate(serialNumber);
			if(x509 == null) {
				throw new ValidateException("Couldn't find certificate with number: " + serialNumber);
			}
			x509CertificateList.add(x509);
		}

		return x509CertificateList;
	}

}
