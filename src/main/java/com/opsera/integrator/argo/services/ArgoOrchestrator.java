package com.opsera.integrator.argo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.ResourcesNotAvailable;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationMetadataList;
import com.opsera.integrator.argo.resources.ArgoApplicationOperation;
import com.opsera.integrator.argo.resources.ArgoApplicationsList;
import com.opsera.integrator.argo.resources.ArgoClusterList;
import com.opsera.integrator.argo.resources.ArgoToolConfig;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.CreateApplicationRequest;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ToolDetails;

/**
 * Class that orchestrates different classes within the service to provide the
 * needed functionality
 */
@Component
public class ArgoOrchestrator {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoOrchestrator.class);

    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * get all argo applications
     * 
     * @param argoToolId
     * @return
     */
    public ArgoApplicationMetadataList getAllApplications(String argoToolId, String customerId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ArgoApplicationsList argoApplicationsList = serviceFactory.getArgoHelper().getAllArgoApplications(argoToolDetails.getConfiguration().getToolURL(),
                argoToolDetails.getConfiguration().getUserName(), argoPassword);
        return serviceFactory.getObjectTranslator().translateToArgoApplicationMetadataList(argoApplicationsList);
    }

    /**
     * get all argo clusters
     *
     * @param argoToolId
     * @return
     */
    public ArgoClusterList getAllClusters(String argoToolId, String customerId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        return serviceFactory.getArgoHelper().getAllArgoClusters(argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * get all argo clusters
     *
     * @param argoToolId
     * @return
     */
    public ArgoApplicationMetadataList getAllProjects(String argoToolId, String customerId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ArgoApplicationsList argoApplicationsList = serviceFactory.getArgoHelper().getAllArgoProjects(argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(),
                argoPassword);
        return serviceFactory.getObjectTranslator().translateToArgoApplicationMetadataList(argoApplicationsList);
    }

    /**
     * get argo application details
     * 
     * @param argoToolId
     * @param applicationName
     * @return
     */
    public ArgoApplicationItem getApplication(String argoToolId, String customerId, String applicationName) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        return serviceFactory.getArgoHelper().getArgoApplication(applicationName, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * syncs an argo application
     * 
     * @param pipelineMetadata
     * @return
     */
    public ArgoApplicationOperation syncApplication(OpseraPipelineMetadata pipelineMetadata) {
        ArgoToolConfig argoToolConfig = serviceFactory.getConfigCollector().getArgoDetails(pipelineMetadata);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolConfig.getToolConfigId(), pipelineMetadata.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ArgoApplicationItem applicationItem = serviceFactory.getArgoHelper().syncApplication(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration().getToolURL(),
                argoToolDetails.getConfiguration().getUserName(), argoPassword);
        return applicationItem.getOperation();
    }

    /**
     * syncs an argo application
     *
     * @param request
     * @return
     */
    public ResponseEntity<String> createApplication(CreateApplicationRequest request) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(request.getToolId(), request.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ArgoApplicationItem argoApplication = serviceFactory.getRequestBuilder().createApplicationRequest(request);
        return serviceFactory.getArgoHelper().createApplication(argoApplication, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * get all argo applications
     * 
     * @param argoToolId
     * @return
     */
    public void validate(String customerId, String argoToolId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        serviceFactory.getArgoHelper().getAllArgoApplications(argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Method used to generate the token
     * 
     * @param customerId
     * @param toolId
     * @return
     * @throws ResourcesNotAvailable
     */
    public String generateNewToken(String customerId, String toolId) throws ResourcesNotAvailable {
        LOGGER.debug("To generate the new token for user {} and toolId {}", customerId, toolId);
        ToolDetails details = serviceFactory.getConfigCollector().getToolsDetails(customerId, toolId);
        return details.getPassword();
    }

}
