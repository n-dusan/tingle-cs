package com.tingle.tingle.service;

import com.tingle.tingle.config.CertificateConfig;
import com.tingle.tingle.domain.certificates.CertificateGenerator;
import com.tingle.tingle.domain.certificates.IssuerData;
import com.tingle.tingle.domain.certificates.SubjectData;
import com.tingle.tingle.domain.dto.CertificateDTO;
import com.tingle.tingle.domain.dto.CertificateX500NameDTO;
import com.tingle.tingle.domain.enums.Role;
import com.tingle.tingle.repository.CertificateRepository;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
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
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;


@Service
public class CertificateService {

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private CertificateGenerator certificateGenerator;

    @Autowired
    private KeyStoreService keyStoreService;


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

                    String x500SubjectName = x509Certificate.getSubjectX500Principal().getName();
                    String x500IssuerName = x509Certificate.getIssuerX500Principal().getName();
                    CertificateX500NameDTO[] x509dto =  convertFromX500Principals(x500IssuerName, x500SubjectName);

                    x509dto[1].setCertificateRole(certificateRole);
                    x509dto[1].setSerialNumber(serialNumber);

                    return x509dto;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }   catch (InvalidNameException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Ana, zanimacete ova metoda koja vraca listu svih CA sertifikata, koji ti mogu upasti u onaj tvoj select.
     * Trebalo bi da sadrzi sve neophodne podatke o mogucim issuerima.
     * Napravi end-point na kontroleru pa ga uvezi sa svojim frontom.
     * TODO: validirati lanac sertifikata, da bi izdavanje novog sertifikata imalo smisla
     * */

    public List<CertificateX500NameDTO> getCertificateCASubjectData() throws FileNotFoundException, InvalidNameException {

        List<X509Certificate> x509rootList = keyStoreService.findKeyStoreCertificates(Role.ROOT);
        List<X509Certificate> x509intermediateList = keyStoreService.findKeyStoreCertificates(Role.INTERMEDIATE);

        List<X509Certificate> cAJoinedList = Stream.concat(x509rootList.stream(), x509intermediateList.stream())
                .collect(Collectors.toList());

        List<CertificateX500NameDTO> cADTOList = new ArrayList<CertificateX500NameDTO>();

        for (X509Certificate x509Certificate : cAJoinedList) {

            String x500SubjectName = x509Certificate.getSubjectX500Principal().getName();
            CertificateX500NameDTO[] x509dto =  convertFromX500Principals(x500SubjectName, x500SubjectName);
            String serialNumber = x509Certificate.getSerialNumber().toString();
            x509dto[1].setSerialNumber(serialNumber);
            Certificate repositoryCertificate = certificateRepository.findCertificateBySerialNumber(serialNumber);

            //Izvuci rolu sertifikata, nazalost iz repozitorijuma
            x509dto[1].setCertificateRole(repositoryCertificate.getCertificateRole());

            cADTOList.add(x509dto[1]);
        }

        return cADTOList;
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
        keyStoreService.saveSelfSignedCertificate(cert, dto.getAlias(), issuer.getPrivateKey());

        //save the cert in the database -> to be used when ocsp implementation occurs
        this.certificateRepository.save(new Certificate(subject.getSerialNumber(),dto.getAlias(), true, Role.ROOT));
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


    /**
     * don't even look at it, black-box.
     * */
    private CertificateX500NameDTO[] convertFromX500Principals(String subjectDN, String issuerDN) throws InvalidNameException {

        CertificateX500NameDTO[] issuerAndSubjectDTO = new CertificateX500NameDTO[2];

        String[] x500principals = { issuerDN, subjectDN };

        for(int i = 0; i < x500principals.length; i++) {

            issuerAndSubjectDTO[i] = new CertificateX500NameDTO();

            LdapName ln = new LdapName(x500principals[i]);

            for (Rdn rdn : ln.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    issuerAndSubjectDTO[i].setCN(rdn.getValue().toString());
                }
                if (rdn.getType().equalsIgnoreCase("C")) {
                    issuerAndSubjectDTO[i].setC(rdn.getValue().toString());
                }
                if (rdn.getType().equalsIgnoreCase("OU")) {
                    issuerAndSubjectDTO[i].setOU(rdn.getValue().toString());
                }
                if (rdn.getType().equalsIgnoreCase("O")) {
                    issuerAndSubjectDTO[i].setO(rdn.getValue().toString());
                }
                if (rdn.getType().equalsIgnoreCase("ST")) {
                    issuerAndSubjectDTO[i].setST(rdn.getValue().toString());
                }
                if (rdn.getType().equalsIgnoreCase("L")) {
                    issuerAndSubjectDTO[i].setL(rdn.getValue().toString());
                }
                if (rdn.getType().equalsIgnoreCase("E")) {
                    issuerAndSubjectDTO[i].setE(rdn.getValue().toString());
                }
            }
        }

        return issuerAndSubjectDTO;
    }


}
