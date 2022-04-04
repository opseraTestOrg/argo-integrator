package com.opsera.integrator.argo.config;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.opsera.core.rest.RestTemplateHelper;
import com.opsera.integrator.argo.services.ArgoHelper;
import com.opsera.integrator.argo.services.ArgoOrchestrator;
import com.opsera.integrator.argo.services.ConfigCollector;
import com.opsera.integrator.argo.services.ObjectTranslator;
import com.opsera.integrator.argo.services.RequestBuilder;
import com.opsera.integrator.argo.services.ResponseParser;
import com.opsera.integrator.argo.services.VaultService;

@Component
public interface IServiceFactory {

    public ArgoHelper getArgoHelper();

    public ArgoOrchestrator getArgoOrchestrator();

    public ResponseParser getResponseParser();

    public ConfigCollector getConfigCollector();

    public ObjectTranslator getObjectTranslator();

    public VaultService getVaultHelper();

    public Gson gson();

    public RestTemplateHelper getRestTemplate();

    public RequestBuilder getRequestBuilder();

}
