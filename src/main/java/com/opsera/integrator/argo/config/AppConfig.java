package com.opsera.integrator.argo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

@Configuration
@Getter
public class AppConfig {

    @Value("${pipeline.config.baseurl}")
    private String pipelineConfigBaseUrl;

    @Value("${argo.defaultUrl}")
    private String argoDefaultUrl;

    @Value("${vault.config.baseurl}")
    private String vaultBaseUrl;

    @Value("${customer.config.baseurl}")
    private String customerBaseUrl;

    @Value("${azure.service.baseurl}")
    private String azureServiceBaseUrl;

    @Value("${aws.service.baseurl}")
    private String awsServiceBaseUrl;

    @Bean
    public ServiceLocatorFactoryBean serviceLocatorFactoryBean() {
        ServiceLocatorFactoryBean factoryBean = new ServiceLocatorFactoryBean();
        factoryBean.setServiceLocatorInterface(IServiceFactory.class);
        return factoryBean;
    }

    @Bean
    @Scope(value = "prototype")
    public StopWatch stopWatch() {
        return new StopWatch();
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

}
