package com.opsera.integrator.argo.config;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.opsera.integrator.argo.services.ArgoHelper;
import com.opsera.integrator.argo.services.ArgoOrchestrator;
import com.opsera.integrator.argo.services.ConfigCollector;
import com.opsera.integrator.argo.services.ObjectTranslator;
import com.opsera.integrator.argo.services.ResponseParser;
import com.opsera.integrator.argo.services.VaultHelper;

@Component
public interface IServiceFactory {

    public ArgoHelper getArgoHelper();

    public ArgoOrchestrator getArgoOrchestrator();

    public ResponseParser getResponseParser();

    public ConfigCollector getConfigCollector();

    public ObjectTranslator getObjectTranslator();

    public VaultHelper getVaultHelper();

    public Gson gson();

    public StopWatch stopWatch();

    public RestTemplate getRestTemplate();

}
