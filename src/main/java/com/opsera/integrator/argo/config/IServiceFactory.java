package com.opsera.integrator.argo.config;

import com.google.gson.Gson;
import com.opsera.integrator.argo.services.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

@Component
public interface IServiceFactory {

    ArgoHelper getArgoHelper();

    ArgoOrchestrator getArgoOrchestrator();

    ResponseParser getResponseParser();

    ConfigCollector getConfigCollector();

    ObjectTranslator getObjectTranslator();

    VaultHelper getVaultHelper();

    Gson gson();

    StopWatch stopWatch();

    RestTemplate getRestTemplate();
}
