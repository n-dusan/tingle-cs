package com.tingle.tingle.config;

import org.springframework.stereotype.Component;

@Component
public class CertificateConfig {

    //amount of time certificates last
    public static final Integer ROOT_YEARS = 20;
    public static final Integer INTERMEDIATE_YEARS = 10;
    public static final Integer END_ENTITY_YEARS = 1;

    //root info, always the same
    public static final String ROOT_CN = "*.tingle.org";
    public static final String ROOT_O = "Tingle Cyber Security Public-key infrastructure";
    public static final String ROOT_OU = "IT";
    public static final String ROOT_L = "Novi Sad";
    public static final String ROOT_ST = "Vojvodina";
    public static final String ROOT_C = "Serbia";
    public static final String ROOT_MAIL = "tingle@tingle.org";
    
    public static final String INTERMEDIATE_CN = "*.tingle.org";
    public static final String INTERMEDIATE_O = "UNS-FTN";
    public static final String INTERMEDIATE_OU = "Katedra za informatiku";
    public static final String INTERMEDIATE_L = "Novi Sad";
    public static final String INTERMEDIATE_ST = "Vojvodina";
    public static final String INTERMEDIATE_C = "Serbia";
    public static final String INTERMEDIATE_MAIL = "service@gmail.com";
   
    

}
