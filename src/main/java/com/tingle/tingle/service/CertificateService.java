package com.tingle.tingle.service;

import com.tingle.tingle.config.CertificateConfig;
import com.tingle.tingle.config.KeyStoreConfig;
import com.tingle.tingle.util.keystores.KeyStoreReader;
import com.tingle.tingle.domain.certificates.CertificateGenerator;
import com.tingle.tingle.domain.certificates.IssuerData;
import com.tingle.tingle.domain.certificates.SubjectData;
import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.enums.OCSPResponse;
import com.tingle.tingle.domain.enums.Role;
import com.tingle.tingle.repository.CertificateRepository;
import com.tingle.tingle.util.CConverter;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tingle.tingle.domain.Certificate;
import java.util.Base64;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.*;
import java.security.cert.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.InvalidNameException;


@Service
public class CertificateService {

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
        List<CertificateDTO> dtoList = new ArrayList<CertificateDTO>();

        for (Certificate certificate : certificates) {
            CertificateDTO dto = new CertificateDTO(
                    certificate.getId(),
                    certificate.getSerialNumber(),
                    certificate.isActive(),
                    certificate.getCertificateRole());

            dtoList.add(dto);
        }
        return dtoList;
    }

    /**
     * @param serialNumber: Serial number of a certificate that's being converted
     * @param certificateRole: ROOT;INTERMEDIATE;END_ENTITY, used for searching the corresponding keystore
     *
     * @return DTO Array where [0] element is Issuer DTO, and [1] element is SubjectDTO
     *
     * can be used for displaying basic issuer data in the form
     *
     * */
    public CertificateX500NameDTO[] getCertificateIssuerAndSubjectData(String serialNumber, Role certificateRole) {
        try {
            List<X509Certificate> x509list = keyStoreService.findKeyStoreCertificates(certificateRole);

            for (X509Certificate x509Certificate : x509list) {
                if(x509Certificate.getSerialNumber().toString().equals(serialNumber)) {


                    X500Name subjName = new JcaX509CertificateHolder(x509Certificate).getSubject();
                    X500Name issuerName = new JcaX509CertificateHolder(x509Certificate).getIssuer();

                    CertificateX500NameDTO[] x509dto =  converter.convertFromX500Principals(subjName, issuerName);

                    x509dto[1].setCertificateRole(certificateRole);
                    x509dto[1].setSerialNumber(serialNumber);

                    return x509dto;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }   catch (InvalidNameException e) {
            e.printStackTrace();
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns data of all CA's in the system.
     * validated :)
     * */

    public List<CertificateX500NameDTO> getCertificateCASubjectData() throws InvalidNameException, CertificateException {

        List<X509Certificate> cAJoinedList = findCACertificates();

        //ne znam zasto mi duplira root u cA; ovo je glupav quickfix
        cAJoinedList = cAJoinedList.stream()
                .distinct()
                .collect(Collectors.toList());

        if(cAJoinedList == null) {
            return null;
        }

        List<CertificateX500NameDTO> cADTOList = new ArrayList<CertificateX500NameDTO>();

        for (X509Certificate x509Certificate : cAJoinedList) {

            //if this certificate isn't valid, don't append it
            if(!validate(x509Certificate)) continue;

            String serialNumber = x509Certificate.getSerialNumber().toString();
            Certificate repositoryCertificate = certificateRepository.findCertificateBySerialNumber(serialNumber);

            //...simulation of OCSP request ...
            OCSPResponse ocspResponse = checkCertificate(serialNumber);
            if(ocspResponse == OCSPResponse.GOOD && repositoryCertificate.getCertificateRole() != Role.END_ENTITY) {
                //set ISSUER data -> chain implementation. USE THE CHAIN LUKE. THE CHAIN IS WITHIN YOU.
                //X509Certificate chain[] = converter.buildPath(x509Certificate, cAJoinedList);
                //x509dto[0].setSerialNumber(chain[1].getSerialNumber().toString());

                X500Name subjName = new JcaX509CertificateHolder(x509Certificate).getSubject();
                CertificateX500NameDTO[] x509dto = converter.convertFromX500Principals(subjName, subjName);

                x509dto[1].setSerialNumber(serialNumber);
                //Izvuci rolu sertifikata iz repository sertifikata
                x509dto[1].setCertificateRole(repositoryCertificate.getCertificateRole());
                cADTOList.add(x509dto[1]);
            }
        }

        return cADTOList;
    }


    /**
     * Metoda generiše self-signed sertifikat i čuva u odgovarajući .jks fajl (root.jks)
        @param dto: DTO primljen sa front-enda.
        Sadrzi sve podatke o kreiranju sertifikata
        U ovom slucaju, issuer i subject su ista firma
     * */
    public void generateSelfSignedCertificate(CertificateX500NameDTO dto) throws NoSuchProviderException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, ParseException {

        //keypair za subjekta i issuera je isti, jer je self-signed sertifikat
        SubjectData subject = generateSubjectData(dto);
        IssuerData issuer = generateIssuerData(dto, subject.getPrivateKey());

        // changed the last param from isCA to extensions
        X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, dto.getExtensions());


        System.out.println("\n===== Certificate issuer=====");
        System.out.println(cert.getIssuerX500Principal().getName());
        System.out.println("\n===== Certicate owner =====");
        System.out.println(cert.getSubjectX500Principal().getName());
        System.out.println("\n===== Certificate =====");

        //save the cert in the keystore
        keyStoreService.saveCertificate(cert, subject.getSerialNumber(), issuer.getPrivateKey(), Role.ROOT);

        //save the cert in the database -> used for revokation check
        this.certificateRepository.save(new Certificate(subject.getSerialNumber(), true, Role.ROOT));
    }


    public void generateCACertificate(CertificateX500NameDTO dto) throws Exception {

        List<X509Certificate> cAJoinedList = findCACertificates();

        if(cAJoinedList == null) {
            throw new Exception("There isn't any root!");
        }

        for (X509Certificate x509Certificate : cAJoinedList) {
            String serialNumber = x509Certificate.getSerialNumber().toString();

            if(dto.getSerialNumber().equalsIgnoreCase(serialNumber)) {

                //prvo ga citaj iz roota
                IssuerData issuer;

                issuer = keyStoreReader.readIssuerFromStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION,
                            serialNumber, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray(),
                            KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
                if(issuer == null) {
                    issuer = keyStoreReader.readIssuerFromStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION,
                            serialNumber, KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray(),
                            KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());
                }

                SubjectData subject = generateSubjectData(dto);

                X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, dto.getExtensions());

                //TODO: verify
                if(!validate(cert)) {
                    System.out.println("I somehow managed to invalidate a perfectly valid chain.");
                }

                System.out.println("\n===== Certificate issuer=====");
                System.out.println(cert.getIssuerX500Principal().getName());
                System.out.println("\n===== Certicate owner =====");
                System.out.println(cert.getSubjectX500Principal().getName());

                keyStoreService.saveCertificate(cert, subject.getSerialNumber(), subject.getPrivateKey(), Role.INTERMEDIATE);

                //save the cert in the database -> to be used when ocsp implementation occurs
                this.certificateRepository.save(new Certificate(subject.getSerialNumber(), true, Role.INTERMEDIATE));
                break;

            }
        }
    }

    public void generateEndEntityCertificate(CertificateX500NameDTO subjectDTO, CertificateX500NameDTO issuerDTO) throws Exception {
    	SubjectData subject = generateSubjectData(subjectDTO);

    	String keyStorePassword = "";
    	IssuerData issuer;
    	if(issuerDTO.getCertificateRole() == Role.ROOT) {
    		keyStorePassword = KeyStoreConfig.ROOT_KEYSTORE_PASSWORD;
    		issuer = this.keyStoreReader.readIssuerFromStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION, issuerDTO.getSerialNumber(), keyStorePassword.toCharArray(), keyStorePassword.toCharArray());
    	} else if(issuerDTO.getCertificateRole() == Role.INTERMEDIATE) {
    		keyStorePassword = KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD;
    		issuer = this.keyStoreReader.readIssuerFromStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION, issuerDTO.getSerialNumber(), keyStorePassword.toCharArray(), keyStorePassword.toCharArray());
    	} else {
    		//Throw exception because End Entity is somehow trying to issue
    		throw new Exception();
    	}

        X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, subjectDTO.getExtensions());

        // TODO: puca verifikacija: certificate does not verify with supplied key
//        cert.verify(subject.getPublicKey());

        System.out.println("\n===== Certificate issuer=====");
        System.out.println(cert.getIssuerX500Principal().getName());
        System.out.println("\n===== Certicate owner =====");
        System.out.println(cert.getSubjectX500Principal().getName());
        System.out.println("\n===== Certificate =====");
        System.out.println("-------------------------------------------------------");
        System.out.println(cert);
        System.out.println("-------------------------------------------------------");

        //save the cert in the keystore
        keyStoreService.saveCertificate(cert, subject.getSerialNumber(), subject.getPrivateKey(), Role.END_ENTITY);

        //save the cert in the database -> to be used when ocsp implementation occurs
        this.certificateRepository.save(new Certificate(subject.getSerialNumber(), true, Role.END_ENTITY));
        System.out.println("===================== SUCCESS =====================");
    }


    private SubjectData generateSubjectData(CertificateX500NameDTO dto) throws ParseException {

        //based on role, generate RSA key pair length
        KeyPair keyPair;

        if (dto.getCertificateRole() == Role.ROOT || dto.getCertificateRole() == Role.INTERMEDIATE) {
            keyPair = certificateGenerator.generateKeyPair(true);
        } else {
            keyPair = certificateGenerator.generateKeyPair(false);
        }

        //Datumi od kad do kad vazi sertifikat
        Calendar cal = new GregorianCalendar();
        SimpleDateFormat iso8601Formater = new SimpleDateFormat("dd-MM-yyyy");
        iso8601Formater.setTimeZone(cal.getTimeZone());

        String startDateString = iso8601Formater.format(cal.getTime());

        //based on role, set certificate expiration date
        if(dto.getCertificateRole() == Role.ROOT) {
            cal.add(Calendar.YEAR, CertificateConfig.ROOT_YEARS);
        } else if(dto.getCertificateRole() == Role.INTERMEDIATE) {
            cal.add(Calendar.YEAR, CertificateConfig.INTERMEDIATE_YEARS);
        } else {
            cal.add(Calendar.YEAR, CertificateConfig.END_ENTITY_YEARS);
        }

        String endDateString = iso8601Formater.format(cal.getTime());

        System.out.println("Start date: " + startDateString);
        System.out.println("End date: " + endDateString);

        Date startDate = iso8601Formater.parse(startDateString);
        Date endDate = iso8601Formater.parse(endDateString);

        //Serial number je vazan, za sad JAKO VELIKI random broj
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
        //UID (USER ID) je ID korisnika, treba da kupiti iz JWT-a npr, tj onaj admin koji je ulogovan
        builder.addRDN(BCStyle.UID, "123456");

        return new SubjectData(keyPair.getPublic(), builder.build(), serialNumber, startDate, endDate, keyPair.getPrivate());
    }

    /**
     * @param dto: DTO sa fronta
     * @param privateKey: privatni kljuc issuera generisan negde van ove metode i pripojen sertifikatu koji se skladisti
     * */
    private IssuerData generateIssuerData(CertificateX500NameDTO dto, PrivateKey privateKey) {

        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

        builder.addRDN(BCStyle.CN,  dto.getCN());
        builder.addRDN(BCStyle.O, dto.getO());
        builder.addRDN(BCStyle.OU, dto.getOU());
        builder.addRDN(BCStyle.C, dto.getC());
        builder.addRDN(BCStyle.E, dto.getE());
        builder.addRDN(BCStyle.L, dto.getL());
        builder.addRDN(BCStyle.ST, dto.getST());
        //TODO: id usera koji kreira zahtev, sad je hardkodovano
        builder.addRDN(BCStyle.UID, "123456");

        return new IssuerData(privateKey, builder.build());
    }


    /**
     * OCSP request validation, a client sends the certificates' serialNumber that he wants to validate
     * OCSP Request service checks the database to find the certificate.
     * @returns: OCSPResponse enum wih the certificate status
     * */

    public OCSPResponse checkCertificate(String serialNumber) {

        Certificate requestedCertificate = certificateRepository.findCertificateBySerialNumber(serialNumber);

        if(requestedCertificate == null) {
            return OCSPResponse.UNKNOWN;
        }

        else if(!requestedCertificate.isActive()) {
            return OCSPResponse.REVOKED;
        }

        return OCSPResponse.GOOD;
    }


    /** merge lista koja vraća sve cA i root sertifikate */
    public List<X509Certificate> findCACertificates() {
        try {
            List<X509Certificate> x509rootList = keyStoreService.findKeyStoreCertificates(Role.ROOT);

            //No roots, how can I issue anything without them?
            if(x509rootList.isEmpty()) {
                return null;
            }

            List<X509Certificate> x509intermediateList = keyStoreService.findKeyStoreCertificates(Role.INTERMEDIATE);

            return Stream.concat(x509rootList.stream(), x509intermediateList.stream())
                    .collect(Collectors.toList());
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * - constructs a chain
     * - verifies their signatures
     * - checks if the first chain element is valid (if it isn't expired)
     * - checks if the chain leads to root
     * */

    private boolean validate(X509Certificate certificate) {

        List<X509Certificate> cAJoined = findCACertificates();

        try {
            X509Certificate[] chain = CConverter.buildPath(certificate, cAJoined);

                try {
                    //proverava da li je istekao sertifikat
                    chain[0].checkValidity();
                } catch(CertificateExpiredException e) {
                    return false;
                } catch(CertificateNotYetValidException e)  {
                    return false;
                }


            if(converter.isSelfSigned(chain[chain.length-1])) {
                System.out.println("==========I AM GROOT");
                return true;
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

        //todo: if certificate isn't valid, recursively revoke every child
        //for now, just set the appropriate certificate to invalid
//        Certificate invalidCertificate = certificateRepository.findCertificateBySerialNumber(certificate.getSerialNumber().toString());
//        invalidCertificate.setActive(false);
//        this.certificateRepository.save(invalidCertificate);

        return false;
    }
    
    public void downloadCertificate(String serialNumber){
    	List<CertificateDTO> list = findAll();
    	for(CertificateDTO c : list) {
    		if(c.getSerialNumber().equals(serialNumber)) {
    			try {
    				if(c.getCertificateRole() == Role.ROOT) {
    					List<X509Certificate> listCert = keyStoreService.findKeyStoreCertificates(Role.ROOT);
    					for(X509Certificate cert : listCert) {
    						if(cert.getSerialNumber().toString().equals(c.getSerialNumber())) {
    							FileOutputStream os = new FileOutputStream(c.getCertificateRole() + ".cer");
    							os.write("--BEGIN CERTIFICATE--\n".getBytes("US-ASCII"));
    							os.write(Base64.getEncoder().encode(cert.getEncoded()));
    							os.write("--END CERTIFICATE--\n".getBytes("US-ASCII"));
    							os.close();
    						}
    					}
    				}else if(c.getCertificateRole() == Role.INTERMEDIATE) {
    					List<X509Certificate> listCert = keyStoreService.findKeyStoreCertificates(Role.INTERMEDIATE);
    					for(X509Certificate cert : listCert) {
    						if(cert.getSerialNumber().toString().equals(c.getSerialNumber())) {
    							FileOutputStream os = new FileOutputStream(c.getCertificateRole() + ".cer");
    							os.write("--BEGIN CERTIFICATE--\n".getBytes("US-ASCII"));
    							os.write(Base64.getEncoder().encode(cert.getEncoded()));
    							os.write("--END CERTIFICATE--\n".getBytes("US-ASCII"));
    							os.close();
    						}
    					}
    				}else {
    					List<X509Certificate> listCert = keyStoreService.findKeyStoreCertificates(Role.END_ENTITY);
    					for(X509Certificate cert : listCert) {
    						if(cert.getSerialNumber().toString().equals(c.getSerialNumber())) {
    							FileOutputStream os = new FileOutputStream(c.getCertificateRole() + ".cer");
    							os.write("--BEGIN CERTIFICATE--\n".getBytes("US-ASCII"));
    							os.write(Base64.getEncoder().encode(cert.getEncoded()));
    							os.write("--END CERTIFICATE--\n".getBytes("US-ASCII"));
    							os.close();
    						}
    					}
    				}
    			}catch(Exception e) {
    				e.printStackTrace();
    			}
    		}
    	}
    }

}
