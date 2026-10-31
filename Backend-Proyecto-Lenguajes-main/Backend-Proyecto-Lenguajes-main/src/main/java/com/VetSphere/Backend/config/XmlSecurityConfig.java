package com.VetSphere.Backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XmlSecurityConfig {
    @PostConstruct
    public void protegerProcesamientoXml() {
        System.setProperty("javax.xml.accessExternalDTD", "");
        System.setProperty("javax.xml.accessExternalSchema", "");
        System.setProperty("jdk.xml.entityExpansionLimit", "64");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "4096");
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "1024");
    }
}

