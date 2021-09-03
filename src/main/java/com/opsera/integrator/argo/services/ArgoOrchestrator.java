package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.FAILED;

import java.io.UnsupportedEncodingException;

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
import com.opsera.integrator.argo.resources.ArgoRepositoriesList;
import com.opsera.integrator.argo.resources.ArgoRepositoryItem;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.CreateApplicationRequest;
import com.opsera.integrator.argo.resources.CreateRepositoryRequest;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.integrator.argo.resources.ToolDetails;

/**
 * Class that orchestrates different classes within the service to provide the
 * needed functionality.
 */
@Component
public class ArgoOrchestrator {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoOrchestrator.class);

    /** The service factory. */
    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * get all argo applications.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all applications
     */
    public ArgoApplicationMetadataList getAllApplications(String argoToolId, String customerId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ArgoApplicationsList argoApplicationsList = serviceFactory.getArgoHelper().getAllArgoApplications(argoToolDetails.getConfiguration().getToolURL(),
                argoToolDetails.getConfiguration().getUserName(), argoPassword);
        return serviceFactory.getObjectTranslator().translateToArgoApplicationMetadataList(argoApplicationsList);
    }

    /**
     * get all argo clusters.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all clusters
     */
    public ArgoClusterList getAllClusters(String argoToolId, String customerId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        return serviceFactory.getArgoHelper().getAllArgoClusters(argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * get all argo clusters.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all projects
     */
    public ArgoApplicationMetadataList getAllProjects(String argoToolId, String customerId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ArgoApplicationsList argoApplicationsList = serviceFactory.getArgoHelper().getAllArgoProjects(argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(),
                argoPassword);
        return serviceFactory.getObjectTranslator().translateToArgoApplicationMetadataList(argoApplicationsList);
    }

    /**
     * get argo application details.
     *
     * @param argoToolId      the argo tool id
     * @param customerId      the customer id
     * @param applicationName the application name
     * @return the application
     */
    public ArgoApplicationItem getApplication(String argoToolId, String customerId, String applicationName) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        return serviceFactory.getArgoHelper().getArgoApplication(applicationName, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * syncs an argo application.
     *
     * @param pipelineMetadata the pipeline metadata
     * @return the argo application operation
     */
    public ArgoApplicationOperation syncApplication(OpseraPipelineMetadata pipelineMetadata) {
        ToolConfig argoToolConfig = serviceFactory.getConfigCollector().getArgoDetails(pipelineMetadata);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolConfig.getToolConfigId(), pipelineMetadata.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ArgoApplicationItem applicationItem = serviceFactory.getArgoHelper().syncApplication(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration().getToolURL(),
                argoToolDetails.getConfiguration().getUserName(), argoPassword);
        return applicationItem.getOperation();
    }

    /**
     * syncs an argo application.
     *
     * @param request the request
     * @return the response entity
     */
    public ResponseEntity<String> createApplication(CreateApplicationRequest request) {
        LOGGER.debug("To Starting to create/update the application {} ", request);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(request.getToolId(), request.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ArgoApplicationItem argoApplication = serviceFactory.getRequestBuilder().createApplicationRequest(request);
        ArgoApplicationItem applicationItem = getApplication(request.getToolId(), request.getCustomerId(), request.getApplicationName());
        if (null == applicationItem) {
            return serviceFactory.getArgoHelper().createApplication(argoApplication, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
        } else {
            return serviceFactory.getArgoHelper().updateApplication(argoApplication, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword,
                    request.getApplicationName());

        }
    }

    /**
     * get all argo applications.
     *
     * @param customerId the customer id
     * @param argoToolId the argo tool id
     */
    public void validate(String customerId, String argoToolId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        serviceFactory.getArgoHelper().getAllArgoApplications(argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Method used to generate the token.
     *
     * @param customerId the customer id
     * @param toolId     the tool id
     * @return the string
     * @throws ResourcesNotAvailable the resources not available
     */
    public String generateNewToken(String customerId, String toolId) throws ResourcesNotAvailable {
        LOGGER.debug("To generate the new token for user {} and toolId {}", customerId, toolId);
        ToolDetails details = serviceFactory.getConfigCollector().getToolsDetails(customerId, toolId);
        return details.getPassword();
    }

    /**
     * delete the argocd application.
     *
     * @param argoToolId      the argo tool id
     * @param customerId      the customer id
     * @param applicationName the application name
     */
    public void deleteApplication(String argoToolId, String customerId, String applicationName) {
        LOGGER.debug("To Starting to delete the application {} and customerId {} and toolId {}", applicationName, customerId, argoToolId);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        serviceFactory.getArgoHelper().deleteArgoApplication(applicationName, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
        LOGGER.debug("To Completed to delete the application {} and customerId {} and toolId {}", applicationName, customerId, argoToolId);
    }

    /**
     * Creates the repository.
     *
     * @param request the request
     * @return the response entity
     * @throws ResourcesNotAvailable the resources not available
     * @throws UnsupportedEncodingException 
     */
    public ResponseEntity<String> createRepository(CreateRepositoryRequest request) throws ResourcesNotAvailable, UnsupportedEncodingException {
        LOGGER.debug("To Starting to create/update the repository {} ", request);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(request.getToolId(), request.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ToolDetails credentialToolDetails = serviceFactory.getConfigCollector().getToolDetails(request.getGitToolId(), request.getCustomerId());
        if (null != credentialToolDetails) {
            String repositoryUrl = "";
            ToolConfig toolConfig = credentialToolDetails.getConfiguration();
            String credentialSecret = null;
            if (toolConfig.isTwoFactorAuthentication()) {
                credentialSecret = toolConfig.getSecretPrivateKey().getVaultKey();
                repositoryUrl = request.getSshUrl();
            } else {
                credentialSecret = toolConfig.getAccountPassword().getVaultKey();
                repositoryUrl = request.getHttpsUrl();
            }
            String secret = serviceFactory.getVaultHelper().getSecret(credentialToolDetails.getOwner(), credentialSecret, credentialToolDetails.getVault());
            ArgoRepositoryItem argoApplication = serviceFactory.getRequestBuilder().createRepositoryRequest(request, toolConfig, secret);
            ArgoRepositoryItem applicationItem = getRepository(request.getToolId(), request.getCustomerId(), repositoryUrl, argoToolDetails, argoPassword);
            if (null != applicationItem && applicationItem.getConnectionState().getStatus().equalsIgnoreCase(FAILED)) {
                return serviceFactory.getArgoHelper().createRepository(argoApplication, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(),
                        argoPassword);
            } else {
                return serviceFactory.getArgoHelper().updateRepository(argoApplication, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(),
                        argoPassword);

            }
        }
        return null;
    }

    /**
     * Gets the repository.
     *
     * @param toolId          the tool id
     * @param customerId      the customer id
     * @param repositoryUrl   the repository url
     * @param argoToolDetails the argo tool details     
     * @param argoPassword    the argo password
     * @return the repository
     * @throws UnsupportedEncodingException 
     */
    private ArgoRepositoryItem getRepository(String toolId, String customerId, String repositoryUrl, ArgoToolDetails argoToolDetails, String argoPassword) throws UnsupportedEncodingException {
        return serviceFactory.getArgoHelper().getArgoRepository(repositoryUrl, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Gets the all argo repositories.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all argo repositories
     */
    public ArgoRepositoriesList getAllArgoRepositories(String argoToolId, String customerId) {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        return serviceFactory.getArgoHelper().getArgoRepositoriesList(argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Delete repository.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @param repoUrl    the repo url
     * @throws UnsupportedEncodingException 
     */
    public void deleteRepository(String argoToolId, String customerId, String repoUrl) throws UnsupportedEncodingException {
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        serviceFactory.getArgoHelper().deleteArgoRepository(repoUrl, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }
}
