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

import java.io.FileNotFoundException;
import java.math.BigInteger;
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

            List<X509Certificate> cAJoined = findCACertificates();

            for (X509Certificate x509Certificate : x509list) {
                if(x509Certificate.getSerialNumber().toString().equals(serialNumber)) {

                    try {
                        X509Certificate[] chain = buildPath(x509Certificate, cAJoined);

                        for (X509Certificate certificate : chain) {
                            try {
                                certificate.checkValidity();

                            } catch(CertificateExpiredException e) {
                                e.printStackTrace();
                            } catch(CertificateNotYetValidException e)  {
                                e.printStackTrace();
                            }
                            System.out.println("=========HAVE I FOUND MY CHAIN?" + certificate.getSubjectX500Principal().getName());
                            String sn = certificate.getSerialNumber().toString();

                            //Certificate cer = certificateRepository.findCertificateBySerialNumber(sn);
                            if(isSelfSigned(certificate)) {
                                System.out.println("==========I AM ROOT");
                            }
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
     * TODO: validate them
     * */

    public List<CertificateX500NameDTO> getCertificateCASubjectData() throws FileNotFoundException, InvalidNameException, CertificateEncodingException {


        List<X509Certificate> cAJoinedList = findCACertificates();

        List<CertificateX500NameDTO> cADTOList = new ArrayList<CertificateX500NameDTO>();

        for (X509Certificate x509Certificate : cAJoinedList) {
            String serialNumber = x509Certificate.getSerialNumber().toString();

            //...simulation of OCSP request ...
            OCSPResponse ocspResponse = checkCertificate(serialNumber);

            if(ocspResponse == OCSPResponse.GOOD) {
                X500Name subjName = new JcaX509CertificateHolder(x509Certificate).getSubject();
                Certificate repositoryCertificate = certificateRepository.findCertificateBySerialNumber(serialNumber);
                //get subject data in better format with a converter
                CertificateX500NameDTO[] x509dto = converter.convertFromX500Principals(subjName, subjName);
                x509dto[1].setSerialNumber(serialNumber);
                //Izvuci rolu sertifikata iz repository sertifikata
                x509dto[1].setCertificateRole(repositoryCertificate.getCertificateRole());

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
        keyStoreService.saveCertificate(cert, subject.getSerialNumber(), issuer.getPrivateKey(), Role.ROOT);

        //save the cert in the database -> to be used when ocsp implementation occurs
        this.certificateRepository.save(new Certificate(subject.getSerialNumber(), true, Role.ROOT));
    }


    /** TODO: validate certificate chain, validate extensions*/
    public void generateCACertificate(CertificateX500NameDTO dto) throws ParseException, Exception {

        List<X509Certificate> cAJoinedList = findCACertificates();

        if(cAJoinedList == null) {
            throw new Exception("There isn't any root!");
        }

        for (X509Certificate x509Certificate : cAJoinedList) {
            String serialNumber = x509Certificate.getSerialNumber().toString();

            if(dto.getSerialNumber().equalsIgnoreCase(serialNumber)) {

                //prvo ga citaj iz roota
                IssuerData issuer = keyStoreReader.readIssuerFromStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION,
                        serialNumber, KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray(),
                            KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
                //ako ga nema u rootu, onda je u cA
                if(issuer == null) {
                    issuer = keyStoreReader.readIssuerFromStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION,
                            serialNumber, KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION.toCharArray(),
                            KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());
                }

                SubjectData subject = generateSubjectData(dto);

                X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, true);

                //TODO: verify

                System.out.println("\n===== Certificate issuer=====");
                System.out.println(cert.getIssuerX500Principal().getName());
                System.out.println("\n===== Certicate owner =====");
                System.out.println(cert.getSubjectX500Principal().getName());
                System.out.println("\n===== Certificate =====");
                System.out.println("-------------------------------------------------------");
                System.out.println(cert);
                System.out.println("-------------------------------------------------------");

                keyStoreService.saveCertificate(cert, subject.getSerialNumber(), subject.getPrivateKey(), Role.INTERMEDIATE);

                //save the cert in the database -> to be used when ocsp implementation occurs
                this.certificateRepository.save(new Certificate(subject.getSerialNumber(), true, Role.INTERMEDIATE));

            }
        }
    }
    
//    public void generateCACertificate(CertificateX500NameDTO dto, String alias) throws ParseException {
//
//    	SubjectData subject = generateSubjectData(dto);
//    	List<CertificateDTO> list = findAll();
//    	for(CertificateDTO c : list) {
//    		if(c.getAlias().equals(alias)) {
//    			if(c.getCertificateRole() == Role.ROOT) {
//
//    				IssuerData issuer = keyStoreReader.readIssuerFromStore(KeyStoreConfig.ROOT_KEYSTORE_LOCATION,
//                            c.getAlias(), KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray(),
//                            KeyStoreConfig.ROOT_KEYSTORE_PASSWORD.toCharArray());
//
//    				X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, true);
//    				System.out.println("\n===== Certificate issuer=====");
//    		        System.out.println(cert.getIssuerX500Principal().getName());
//    		        System.out.println("\n===== Certicate owner =====");
//    		        System.out.println(cert.getSubjectX500Principal().getName());
//    		        System.out.println("\n===== Certificate =====");
//    		        System.out.println("-------------------------------------------------------");
//    		        System.out.println(cert);
//    		        System.out.println("-------------------------------------------------------");
//
//
//    		        keyStoreService.saveCertificate(cert, dto.getAlias(), subject.getPrivateKey(), Role.INTERMEDIATE);
//    		        this.certificateRepository.save(new Certificate(subject.getSerialNumber(), dto.getAlias(), true, Role.INTERMEDIATE));
//    			} else {
//    				//kako cemo u ovom slucaju promeniti alias za novi sertifikat?
//    				//pseudo kod za olju:
//                    //dobices serijski broj sertifikata issuera sa front-enda, onda pozoves getCertificateIssuerAndSubjectData i
//                    //odatle izvuces subjekta tog sertifikata koji ce biti issuer za ovaj sertifikat koji sad pravis
//                    //edit: malo jasniji koraci:
//                    // - pozoves getCertificateIssuerAndSubjectData nad serijskim brojem issuera, dobijes issuera i subjekta
//                    //  - pozoves generateIssuerData i prosledis dto subjekta u njega, a privatan kljuc
//    				IssuerData issuer = keyStoreReader.readIssuerFromStore(KeyStoreConfig.INTERMEDIATE_KEYSTORE_LOCATION,
//                            c.getAlias(),
//                            KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray(),
//                            KeyStoreConfig.INTERMEDIATE_KEYSTORE_PASSWORD.toCharArray());
//
//    				X509Certificate cert = certificateGenerator.generateCertificate(subject, issuer, true);
//    				System.out.println("\n===== Certificate issuer=====");
//    		        System.out.println(cert.getIssuerX500Principal().getName());
//    		        System.out.println("\n===== Certicate owner =====");
//    		        System.out.println(cert.getSubjectX500Principal().getName());
//    		        System.out.println("\n===== Certificate =====");
//    		        System.out.println("-------------------------------------------------------");
//    		        System.out.println(cert);
//    		        System.out.println("-------------------------------------------------------");
//
//    		        keyStoreService.saveCertificate(cert, dto.getAlias(), issuer.getPrivateKey(), Role.INTERMEDIATE);
//    		        this.certificateRepository.save(new Certificate(subject.getSerialNumber(),dto.getAlias(), true, Role.INTERMEDIATE));
//    			}
//    		}
//    	}
//
//    }

    
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


//    public void validate(X509Certificate[] chain) throws CertificateException {
//        byte[] chain = chain
//        CertificateFactory cf = CertificateFactory.getInstance("X.509");
//        List<Certificate> certx = new ArrayList<>(chain.length);
//        for (byte[] c : chain)
//            certx.add(cf.generateCertificate(new ByteArrayInputStream(c)));
//        CertPath path = cf.generateCertPath(certx);
//        CertPathValidator validator = CertPathValidator.getInstance("PKIX");
//        KeyStore keystore = KeyStore.getInstance("JKS");
//        try (InputStream is = Files.newInputStream(Paths.get("cacerts.jks"))) {
//            keystore.load(is, "changeit".toCharArray());
//        }
//        Collection<? extends CRL> crls;
//        try (InputStream is = Files.newInputStream(Paths.get("crls.p7c"))) {
//            crls = cf.generateCRLs(is);
//        }
//        PKIXParameters params = new PKIXParameters(keystore);
//        CertStore store = CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls));
//        /* If necessary, specify the certificate policy or other requirements
//         * with the appropriate params.setXXX() method. */
//        params.addCertStore(store);
//        /* Validate will throw an exception on invalid chains. */
//        PKIXCertPathValidatorResult r = (PKIXCertPathValidatorResult) validator.validate(path, params);
//    }

}
