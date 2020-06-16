package com.opsera.integrator.argo.services;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.opsera.integrator.argo.resources.Constants.ARGO_VAULT_KEY_TEMPLATE;

/**
 * Class that orchestrates different classes within the service
 * to provide the needed functionality
 */
@Component
public class ArgoOrchestrator {

    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * get all argo applications
     * @param argoToolId
     * @return
     */
    public ArgoApplicationMetadataList getAllApplications(String argoToolId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId);
        String argoVaultKey = String.format(ARGO_VAULT_KEY_TEMPLATE, argoToolId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoVaultKey);
        ArgoApplicationsList argoApplicationsList = serviceFactory.getArgoHelper().getAllArgoApplications(
                argoToolDetails.getConfiguration().getUserName(), argoPassword);
        return serviceFactory.getObjectTranslator().translateToArgoApplicationMetadataList(argoApplicationsList);
    }

    /**
     * get argo application details
     * @param argoToolId
     * @param applicationName
     * @return
     */
    public ArgoApplicationItem getApplication(String argoToolId, String applicationName) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId);
        String argoVaultKey = String.format(ARGO_VAULT_KEY_TEMPLATE, argoToolId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoVaultKey);
        return serviceFactory.getArgoHelper().getArgoApplication(
                applicationName, argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * syncs an argo application
     * @param pipelineMetadata
     * @return
     */
    public ArgoApplicationOperation syncApplication(OpseraPipelineMetadata pipelineMetadata) {
        ArgoToolConfig argoToolConfig = serviceFactory.getConfigCollector().getArgoDetails(pipelineMetadata);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(pipelineMetadata.getCustomerId(), argoToolConfig.getVaultSecretKey());
        ArgoApplicationItem applicationItem = serviceFactory.getArgoHelper().syncApplication(
                argoToolConfig.getApplicationName(), argoToolConfig.getUserName(), argoPassword);
        return applicationItem.getOperation();
    }
}
