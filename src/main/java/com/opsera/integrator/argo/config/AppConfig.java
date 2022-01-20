package com.opsera.integrator.argo.config;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import com.opsera.integrator.argo.log.CorrelationIdInterceptor;

import lombok.Getter;

/**
 * All app config reside here
 */
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

    /**
     * Factory Bean Creation
     * 
     * @return
     */
    @Bean
    public ServiceLocatorFactoryBean serviceLocatorFactoryBean() {
        ServiceLocatorFactoryBean factoryBean = new ServiceLocatorFactoryBean();
        factoryBean.setServiceLocatorInterface(IServiceFactory.class);
        return factoryBean;
    }

    /**
     * Stopwatch Bean Creation
     * 
     * @return
     */
    @Bean
    public StopWatch stopWatch() {
        return new StopWatch();
    }

    /**
     * Rest Template Bean Creation
     * 
     * @return
     */
    @Bean
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(new CorrelationIdInterceptor()));
        return restTemplate;
    }

}
