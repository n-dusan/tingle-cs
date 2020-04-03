package com.tingle.tingle.service;

import com.tingle.tingle.config.CertificateConfig;
import com.tingle.tingle.config.keystores.KeyStoreConfig;
import com.tingle.tingle.config.keystores.KeyStoreReader;
import com.tingle.tingle.config.keystores.KeyStoreWriter;
import com.tingle.tingle.domain.certificates.CertificateGenerator;
import com.tingle.tingle.domain.certificates.IssuerData;
import com.tingle.tingle.domain.certificates.SubjectData;
import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.enums.Role;
import com.tingle.tingle.repository.CertificateRepository;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tingle.tingle.domain.Certificate;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
                    certificate.getCertificateRole()
            );

            dtoList.add(dto);
        }

        return dtoList;
    }

    /**
     * Metoda generiše self-signed sertifikat i čuva u odgovarajući .jks fajl (root.jks)
        @param dto: DTO primljen sa front-enda(mada u ovom slucaju i nema front enda jer su hardkodovani podaci.
        Sadrzi sve podatke o kreiranju sertifikata
        U ovom slucaju, issuer i subject su ista firma
     * */
    public void generateSelfSignedCertificate(CertificateX500NameDTO dto) throws NoSuchProviderException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, ParseException {

        //keypair za subjekta i issuera je isti, jer je self-signed sertifikat
        SubjectData subject = generateSubjectData(dto);
        IssuerData issuer = generateIssuerData(dto, subject.getPrivateKey());

        X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer);
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
        keyStoreService.saveSelfSignedCertificate(cert, dto.getAlias(), issuer.getPrivateKey());

        //save the cert in the database -> to be used when ocsp implementation occurs
        this.certificateRepository.save(new Certificate(subject.getSerialNumber(),dto.getAlias(), true, Role.ROOT));
    }
    
    public void generateCACertificate(CertificateX500NameDTO dto, String alias) throws NoSuchProviderException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, ParseException {
    	
    	SubjectData subject = generateSubjectData(dto);
    	List<CertificateDTO> list = findAll();
    	for(CertificateDTO c : list) {
    		if(c.getAlias().equals(alias)) {
    			if(c.getCertificateRole() == Role.ROOT) {
    				
    				IssuerData issuer = keyStoreReader.readIssuerFromStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION, c.getAlias(), KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray(), KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
    				X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer);
    				System.out.println("\n===== Certificate issuer=====");
    		        System.out.println(cert.getIssuerX500Principal().getName());
    		        System.out.println("\n===== Certicate owner =====");
    		        System.out.println(cert.getSubjectX500Principal().getName());
    		        System.out.println("\n===== Certificate =====");
    		        System.out.println("-------------------------------------------------------");
    		        System.out.println(cert);
    		        System.out.println("-------------------------------------------------------");

    		        //save the cert in the keystore
    		        keyStoreService.saveCACertificate(cert, dto.getAlias(), issuer.getPrivateKey());

    		        //save the cert in the database -> to be used when ocsp implementation occurs
    		        this.certificateRepository.save(new Certificate(subject.getSerialNumber(),dto.getAlias(), true, Role.INTERMEDIATE));
    			}else {
    				//kako cemo u ovom slucaju promeniti alias za novi sertifikat?
    				
    				IssuerData issuer = keyStoreReader.readIssuerFromStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION, c.getAlias(), KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray(), KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());
    				X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer);
    				System.out.println("\n===== Certificate issuer=====");
    		        System.out.println(cert.getIssuerX500Principal().getName());
    		        System.out.println("\n===== Certicate owner =====");
    		        System.out.println(cert.getSubjectX500Principal().getName());
    		        System.out.println("\n===== Certificate =====");
    		        System.out.println("-------------------------------------------------------");
    		        System.out.println(cert);
    		        System.out.println("-------------------------------------------------------");

    		        //save the cert in the keystore
    		        keyStoreService.saveCACertificate(cert, dto.getAlias(), issuer.getPrivateKey());

    		        //save the cert in the database -> to be used when ocsp implementation occurs
    		        this.certificateRepository.save(new Certificate(subject.getSerialNumber(),dto.getAlias(), true, Role.INTERMEDIATE));
    			}
    		}
    	}
    	
    }

    
    private SubjectData generateSubjectData(CertificateX500NameDTO dto) throws ParseException {

        //generate keyPair with longer key size
        //TODO: based on role, generate key length
        KeyPair keyPair = certificateGenerator.generateKeyPair(true);

        //Datumi od kad do kad vazi sertifikat
        Calendar cal = new GregorianCalendar();
        SimpleDateFormat iso8601Formater = new SimpleDateFormat("dd-MM-yyyy");
        iso8601Formater.setTimeZone(cal.getTimeZone());

        String startDateString = iso8601Formater.format(cal.getTime());

        //TODO: based on role, set expiration date
        //20 godina sam stavio da traje root :D
        cal.add(Calendar.YEAR, CertificateConfig.ROOT_YEARS);
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
        //ovo nisam nasao u X509 specifikaciji kao navedena polja, ali se mogu dodati
//        builder.addRDN(BCStyle.SURNAME, "Musk");
//        builder.addRDN(BCStyle.GIVENNAME, "Elon");
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
        //TODO: id usera koji kreira zahtev, sad je hardkodovano
        builder.addRDN(BCStyle.UID, "123456");


        return new IssuerData(privateKey, builder.build());
    }

    private KeyPair getKeyPair(){
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            keyGen.initialize(2048, random);
            return keyGen.generateKeyPair();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        return null;
    }

}
