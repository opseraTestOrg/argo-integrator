package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.PIPELINE_TABLE_ENDPOINT;
import static com.opsera.integrator.argo.resources.Constants.QUERY_PARM_CUSTOMERID;
import static com.opsera.integrator.argo.resources.Constants.QUERY_PARM_TOOLID;
import static com.opsera.integrator.argo.resources.Constants.TOOL_REGISTRY_ENDPOINT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.opsera.integrator.argo.config.AppConfig;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ArgoToolConfig;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;

/**
 * Class to interact and fetch the configurations
 */
@Component
public class ConfigCollector {

    public static final Logger LOGGER = LoggerFactory.getLogger(ConfigCollector.class);

    @Autowired
    private IServiceFactory serviceFactory;

    @Autowired
    private AppConfig appConfig;

    /**
     * Get the argo config defined for the given pipeline/step
     * 
     * @param opseraPipelineMetadata
     * @return
     */
    public ArgoToolConfig getArgoDetails(OpseraPipelineMetadata opseraPipelineMetadata) {
        RestTemplate restTemplate = serviceFactory.getRestTemplate();
        String toolsConfigURL = appConfig.getPipelineConfigBaseUrl() + PIPELINE_TABLE_ENDPOINT;
        String response = restTemplate.postForObject(toolsConfigURL, opseraPipelineMetadata, String.class);
        ArgoToolConfig argoToolConfig = serviceFactory.getResponseParser().extractArgoToolConfig(response);
        return  argoToolConfig;
    }

    /**
     * Get the argo config
     * 
     * @param argoToolId
     * @return
     */
    public ArgoToolDetails getArgoDetails(String argoToolId, String customerId) {
        RestTemplate restTemplate = serviceFactory.getRestTemplate();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(appConfig.getPipelineConfigBaseUrl() + TOOL_REGISTRY_ENDPOINT)
                .queryParam(QUERY_PARM_TOOLID, argoToolId).queryParam(QUERY_PARM_CUSTOMERID, customerId);
        String response = restTemplate.getForObject(uriBuilder.toUriString(), String.class);
        ArgoToolDetails argoToolDetails = serviceFactory.getResponseParser().extractArgoToolDetails(response);
        return  argoToolDetails;
    }
}
