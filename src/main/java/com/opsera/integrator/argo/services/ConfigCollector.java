package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.GET_TOOL_DETAILS;
import static com.opsera.integrator.argo.resources.Constants.PIPELINE_TABLE_ENDPOINT;
import static com.opsera.integrator.argo.resources.Constants.QUERY_PARM_CUSTOMERID;
import static com.opsera.integrator.argo.resources.Constants.QUERY_PARM_TOOLID;
import static com.opsera.integrator.argo.resources.Constants.TOOL_REGISTRY_ENDPOINT;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.opsera.integrator.argo.config.AppConfig;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.ResourcesNotAvailable;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ToolDetails;

/**
 * Class to interact and fetch the configurations.
 */
@Component
public class ConfigCollector {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(ConfigCollector.class);

    /** The service factory. */
    @Autowired
    private IServiceFactory serviceFactory;

    /** The app config. */
    @Autowired
    private AppConfig appConfig;

    /**
     * Get the argo config defined for the given pipeline/step.
     *
     * @param opseraPipelineMetadata the opsera pipeline metadata
     * @return the argo details
     */
    public ToolConfig getArgoDetails(OpseraPipelineMetadata opseraPipelineMetadata) {
        RestTemplate restTemplate = serviceFactory.getRestTemplate();
        String toolsConfigURL = appConfig.getPipelineConfigBaseUrl() + PIPELINE_TABLE_ENDPOINT;
        String response = restTemplate.postForObject(toolsConfigURL, opseraPipelineMetadata, String.class);
        return serviceFactory.getResponseParser().extractArgoToolConfig(response);
    }

    /**
     * Get the argo config.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the argo details
     */
    public ArgoToolDetails getArgoDetails(String argoToolId, String customerId) {
        RestTemplate restTemplate = serviceFactory.getRestTemplate();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(appConfig.getPipelineConfigBaseUrl() + TOOL_REGISTRY_ENDPOINT).queryParam(QUERY_PARM_TOOLID, argoToolId)
                .queryParam(QUERY_PARM_CUSTOMERID, customerId);
        String response = restTemplate.getForObject(uriBuilder.toUriString(), String.class);
        return serviceFactory.getResponseParser().extractArgoToolDetails(response);
    }

    /**
     * To call the tool details to get it from customer service.
     *
     * @param customerId the customer id
     * @param toolId     the tool id
     * @return the tools details
     * @throws ResourcesNotAvailable the resources not available
     */
    public ToolDetails getToolsDetails(String customerId, String toolId) throws ResourcesNotAvailable {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(appConfig.getCustomerBaseUrl()).path(String.format(GET_TOOL_DETAILS, customerId, toolId));
        try {
            ResponseEntity<ToolDetails> responseEntity = serviceFactory.getRestTemplate().exchange(uriBuilder.toUriString(), HttpMethod.GET, getRequestEntity(null), ToolDetails.class);
            Optional<ToolDetails> response = Optional.ofNullable(responseEntity.getBody());
            if (response.isPresent()) {
                return response.get();
            } else {
                LOGGER.warn("No Tools Details available for tool Id {}", toolId);
                throw new ResourcesNotAvailable(String.format("No Tools Details available for tool Id %s", toolId));
            }
        } catch (Exception ex) {
            LOGGER.warn("Exception while retrieving tools Details", ex);
            throw new ResourcesNotAvailable(String.format("No Tools Details available for tool Id %s", toolId));
        }
    }

    /**
     * This method used to construct http header.
     *
     * @param obj the obj
     * @return the request entity
     */
    private HttpEntity<Object> getRequestEntity(Object obj) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return null != obj ? new HttpEntity<>(obj, requestHeaders) : new HttpEntity<>(requestHeaders);
    }

    /**
     * Gets the tool details.
     *
     * @param toolConfigId the tool config id
     * @param customerId   the customer id
     * @return the tool details
     */
    public ToolDetails getToolDetails(String toolConfigId, String customerId) {
        RestTemplate restTemplate = serviceFactory.getRestTemplate();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(appConfig.getPipelineConfigBaseUrl()).path(TOOL_REGISTRY_ENDPOINT).queryParam(QUERY_PARM_TOOLID, toolConfigId)
                .queryParam(QUERY_PARM_CUSTOMERID, customerId);
        return restTemplate.getForObject(uriBuilder.toUriString(), ToolDetails.class);
    }
}
