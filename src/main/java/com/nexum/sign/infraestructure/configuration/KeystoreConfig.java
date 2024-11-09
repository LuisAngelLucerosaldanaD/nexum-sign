package com.nexum.sign.infraestructure.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class KeystoreConfig {
    @Value("${keystore.file.path}")
    private String FilePath;

    @Value("${keystore.password}")
    private String Password;

    @Value("${keystore.alias}")
    private String Alias;
}
