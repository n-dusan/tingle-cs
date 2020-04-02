package com.tingle.tingle.config.keystores;
import org.springframework.stereotype.Component;


@Component
public class KeyStoreConfig {

    public static final String ROOT_KEYSTORE_LOCATION = "./.jks/root.jks";
    public static final String INTERMEDIATE_KEYSTORE_LOCATION = "./.jks/intermediate.jks";
    public static final String END_ENTITY_KEYSTORE_LOCATION = "./.jks/end-entity.jks";

    public static final String ROOT_KEYSTORE_PASSWORD = "root";
    public static final String INTERMEDIATE_KEYSTORE_PASSWORD = "intermediate";
    public static final String END_ENTITY_KEYSTORE_PASSWORD = "end-entity";


}
