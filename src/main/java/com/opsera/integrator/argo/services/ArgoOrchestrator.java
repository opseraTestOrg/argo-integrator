package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ARGO_VAULT_KEY_TEMPLATE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationMetadataList;
import com.opsera.integrator.argo.resources.ArgoApplicationOperation;
import com.opsera.integrator.argo.resources.ArgoApplicationsList;
import com.opsera.integrator.argo.resources.ArgoToolConfig;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;

/**
 * Class that orchestrates different classes within the service to provide the
 * needed functionality
 */
@Component
public class ArgoOrchestrator {

    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * get all argo applications
     * 
     * @param argoToolId
     * @return
     */
    public ArgoApplicationMetadataList getAllApplications(String argoToolId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId);
        String argoVaultKey = String.format(ARGO_VAULT_KEY_TEMPLATE, argoToolId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoVaultKey);
        ArgoApplicationsList argoApplicationsList = serviceFactory.getArgoHelper().getAllArgoApplications(
                argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
        return serviceFactory.getObjectTranslator().translateToArgoApplicationMetadataList(argoApplicationsList);
    }

    /**
     * get argo application details
     * 
     * @param argoToolId
     * @param applicationName
     * @return
     */
    public ArgoApplicationItem getApplication(String argoToolId, String applicationName) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId);
        String argoVaultKey = String.format(ARGO_VAULT_KEY_TEMPLATE, argoToolId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoVaultKey);
        return serviceFactory.getArgoHelper().getArgoApplication(applicationName,
                argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * syncs an argo application
     * 
     * @param pipelineMetadata
     * @return
     */
    public ArgoApplicationOperation syncApplication(OpseraPipelineMetadata pipelineMetadata) {
        ArgoToolConfig argoToolConfig = serviceFactory.getConfigCollector().getArgoDetails(pipelineMetadata);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(pipelineMetadata.getCustomerId(), argoToolConfig.getVaultSecretKey());
        ArgoApplicationItem applicationItem = serviceFactory.getArgoHelper().syncApplication(
                argoToolConfig.getApplicationName(), argoToolConfig.getToolURL(),
                argoToolConfig.getUserName(), argoPassword);
        return applicationItem.getOperation();
    }
}
