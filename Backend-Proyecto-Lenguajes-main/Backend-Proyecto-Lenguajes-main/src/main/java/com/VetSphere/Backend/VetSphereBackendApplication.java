package com.VetSphere.Backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VetSphereBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(VetSphereBackendApplication.class, args);
    }
}



