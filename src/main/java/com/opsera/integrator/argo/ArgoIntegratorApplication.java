package com.opsera.integrator.argo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
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
