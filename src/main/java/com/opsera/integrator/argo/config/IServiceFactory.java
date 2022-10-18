package com.opsera.integrator.argo.config;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.opsera.integrator.argo.services.ArgoHelper;
import com.opsera.integrator.argo.services.ArgoOrchestrator;
import com.opsera.integrator.argo.services.ArgoOrchestratorV2;
import com.opsera.integrator.argo.services.AwsServiceHelper;
import com.opsera.integrator.argo.services.ConfigCollector;
import com.opsera.integrator.argo.services.KafkaHelper;
import com.opsera.integrator.argo.services.ObjectTranslator;
import com.opsera.integrator.argo.services.RequestBuilder;
import com.opsera.integrator.argo.services.ResponseParser;

@Component
public interface IServiceFactory {

    public ArgoHelper getArgoHelper();

    public ArgoOrchestrator getArgoOrchestrator();

    public ResponseParser getResponseParser();

    public ConfigCollector getConfigCollector();

    public ObjectTranslator getObjectTranslator();

    public Gson gson();

    public StopWatch stopWatch();

    public RequestBuilder getRequestBuilder();
    
    public ObjectMapper getObjectMapper();
    
    public ArgoOrchestratorV2 getArgoOrchestratorV2();
    
    public KafkaHelper getKafkaHelper();
    
    public AwsServiceHelper getAwsServiceHelper();

}
