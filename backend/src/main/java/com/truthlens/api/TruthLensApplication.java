package com.truthlens.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TruthLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(TruthLensApplication.class, args);
        System.out.println("=================================================");
        System.out.println("  TRUTHLENS AI VERIFICATION API IS NOW ONLINE    ");
        System.out.println("  Port: 8080 | Security: JWT Enabled | NLP Active");
        System.out.println("=================================================");
    }
}
