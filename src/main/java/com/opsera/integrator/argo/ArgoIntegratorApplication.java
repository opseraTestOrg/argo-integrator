package com.opsera.integrator.argo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.opsera.core"})
public class ArgoIntegratorApplication {

    /**
     * Spring boot application
     * 
     * @param args
     */
    public static void main(String[] args) {

        SpringApplication.run(ArgoIntegratorApplication.class, args);

    }

}
