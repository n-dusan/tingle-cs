package com.tingle.tingle.service;

import com.tingle.tingle.config.CertificateConfig;
import com.tingle.tingle.config.keystores.KeyStoreConfig;
import com.tingle.tingle.config.keystores.KeyStoreReader;
import com.tingle.tingle.domain.certificates.CertificateGenerator;
import com.tingle.tingle.domain.certificates.IssuerData;
import com.tingle.tingle.domain.certificates.SubjectData;
import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.enums.OCSPResponse;
import com.tingle.tingle.domain.enums.Role;
import com.tingle.tingle.repository.CertificateRepository;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tingle.tingle.domain.Certificate;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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


    public List<CertificateDTO> findAll() {
        List<Certificate> certificates = certificateRepository.findAll();
        List<CertificateDTO> dtoList = new ArrayList<CertificateDTO>();

        for (Certificate certificate : certificates) {
            CertificateDTO dto = new CertificateDTO(
                    certificate.getId(),
                    certificate.getSerialNumber(),
                    certificate.getAlias(),
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

                    CertificateX500NameDTO[] x509dto =  convertFromX500Principals(subjName, issuerName);

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
     * Ana, zanimacete ova metoda koja vraca listu svih CA sertifikata, koji ti mogu upasti u onaj tvoj select.
     * Trebalo bi da sadrzi sve neophodne podatke o mogucim issuerima.
     *
     * TODO: validirati lanac sertifikata, da bi izdavanje novog sertifikata imalo smisla
     * */

    public List<CertificateX500NameDTO> getCertificateCASubjectData() throws FileNotFoundException, InvalidNameException, CertificateEncodingException {

        List<X509Certificate> x509rootList = keyStoreService.findKeyStoreCertificates(Role.ROOT);
        List<X509Certificate> x509intermediateList = keyStoreService.findKeyStoreCertificates(Role.INTERMEDIATE);

        List<X509Certificate> cAJoinedList = Stream.concat(x509rootList.stream(), x509intermediateList.stream())
                .collect(Collectors.toList());

        List<CertificateX500NameDTO> cADTOList = new ArrayList<CertificateX500NameDTO>();

        for (X509Certificate x509Certificate : cAJoinedList) {
            String serialNumber = x509Certificate.getSerialNumber().toString();

            //...simulation of OCSP request ...
            OCSPResponse ocspResponse = checkCertificate(serialNumber);

            if(ocspResponse == OCSPResponse.GOOD) {

                X500Name subjName = new JcaX509CertificateHolder(x509Certificate).getSubject();
                Certificate repositoryCertificate = certificateRepository.findCertificateBySerialNumber(serialNumber);
                //get subject data in better format with a converter
                CertificateX500NameDTO[] x509dto = convertFromX500Principals(subjName, subjName);
                x509dto[1].setSerialNumber(serialNumber);
                //Izvuci rolu sertifikata iz repository sertifikata
                x509dto[1].setCertificateRole(repositoryCertificate.getCertificateRole());
                x509dto[1].setAlias(repositoryCertificate.getAlias());

                //append the certificate to the valid cA list
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

        X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, true);
        //verify the self signed cert
        cert.verify(subject.getPublicKey());

        System.out.println("\n===== Certificate issuer=====");
        System.out.println(cert.getIssuerX500Principal().getName());
        System.out.println("\n===== Certicate owner =====");
        System.out.println(cert.getSubjectX500Principal().getName());
        System.out.println("\n===== Certificate =====");
        System.out.println("-------------------------------------------------------");
        System.out.println(cert);
        System.out.println("-------------------------------------------------------");

        //save the cert in the keystore
        keyStoreService.saveCertificate(cert, dto.getAlias(), issuer.getPrivateKey(), Role.ROOT);

        //save the cert in the database -> to be used when ocsp implementation occurs
        this.certificateRepository.save(new Certificate(subject.getSerialNumber(),dto.getAlias(), true, Role.ROOT));
    }
    
    public void generateCACertificate(CertificateX500NameDTO dto, String alias) throws NoSuchProviderException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, ParseException {
    	
    	SubjectData subject = generateSubjectData(dto);
    	List<CertificateDTO> list = findAll();
    	for(CertificateDTO c : list) {
    		if(c.getAlias().equals(alias)) {
    			if(c.getCertificateRole() == Role.ROOT) {
    				
    				IssuerData issuer = keyStoreReader.readIssuerFromStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION,
                            c.getAlias(), KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray(),
                            KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());

    				X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, true);
    				System.out.println("\n===== Certificate issuer=====");
    		        System.out.println(cert.getIssuerX500Principal().getName());
    		        System.out.println("\n===== Certicate owner =====");
    		        System.out.println(cert.getSubjectX500Principal().getName());
    		        System.out.println("\n===== Certificate =====");
    		        System.out.println("-------------------------------------------------------");
    		        System.out.println(cert);
    		        System.out.println("-------------------------------------------------------");

    		        
    		        keyStoreService.saveCertificate(cert, dto.getAlias(), subject.getPrivateKey(), Role.INTERMEDIATE);
    		        this.certificateRepository.save(new Certificate(subject.getSerialNumber(), dto.getAlias(), true, Role.INTERMEDIATE));
    			} else {
    				//kako cemo u ovom slucaju promeniti alias za novi sertifikat?
    				//pseudo kod za olju:
                    //dobices serijski broj sertifikata issuera sa front-enda, onda pozoves getCertificateIssuerAndSubjectData i
                    //odatle izvuces subjekta tog sertifikata koji ce biti issuer za ovaj sertifikat koji sad pravis
                    //edit: malo jasniji koraci:
                    // - pozoves getCertificateIssuerAndSubjectData nad serijskim brojem issuera, dobijes issuera i subjekta
                    //  - pozoves generateIssuerData i prosledis dto subjekta u njega, a privatan kljuc
    				IssuerData issuer = keyStoreReader.readIssuerFromStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION,
                            c.getAlias(),
                            KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray(),
                            KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());

    				X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, true);
    				System.out.println("\n===== Certificate issuer=====");
    		        System.out.println(cert.getIssuerX500Principal().getName());
    		        System.out.println("\n===== Certicate owner =====");
    		        System.out.println(cert.getSubjectX500Principal().getName());
    		        System.out.println("\n===== Certificate =====");
    		        System.out.println("-------------------------------------------------------");
    		        System.out.println(cert);
    		        System.out.println("-------------------------------------------------------");

    		        keyStoreService.saveCertificate(cert, dto.getAlias(), issuer.getPrivateKey(), Role.INTERMEDIATE);
    		        this.certificateRepository.save(new Certificate(subject.getSerialNumber(),dto.getAlias(), true, Role.INTERMEDIATE));
    			}
    		}
    	}
    	
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


        //klasa X500NameBuilder pravi X500Name objekat koji predstavlja podatke o vlasniku
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
     *  dodajte jos polja za issuera
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
     * don't even look at it, black-box.
     * */
    private CertificateX500NameDTO[] convertFromX500Principals(X500Name subjectDN, X500Name issuerDN) throws InvalidNameException {

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

    /**
     * OCSP request validation, a client sends the certificates' serialNumber that he wants to validate
     * OCSP Request service checks the database to find the certificate.
     * @return: OCSPResponse enum wih the certificate status
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


}
