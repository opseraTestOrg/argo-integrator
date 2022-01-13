package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.AWS;
import static com.opsera.integrator.argo.resources.Constants.AZURE;
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
import com.opsera.integrator.argo.resources.AwsClusterDetails;
import com.opsera.integrator.argo.resources.AzureClusterDetails;
import com.opsera.integrator.argo.resources.CreateApplicationRequest;
import com.opsera.integrator.argo.resources.CreateCluster;
import com.opsera.integrator.argo.resources.CreateClusterRequest;
import com.opsera.integrator.argo.resources.CreateProjectRequest;
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
        LOGGER.debug("Starting to fetch All Argo Applications for toolId {} and customerId {}", argoToolId, customerId);
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
        LOGGER.debug("Starting to fetch All Argo Clusters for toolId {} and customerId {}", argoToolId, customerId);
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
        LOGGER.debug("Starting to fetch All Argo Projects for toolId {} and customerId {}", argoToolId, customerId);
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
        LOGGER.debug("Starting to fetch Argo Application for toolId {} and customerId {} and applicationName {}", argoToolId, customerId, applicationName);
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
        LOGGER.debug("Starting to Sync Argo Application for request{}", pipelineMetadata);
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
        ArgoApplicationMetadataList applicationMetadataList = getAllApplications(request.getToolId(), request.getCustomerId());
        boolean isApplicationExists = applicationMetadataList.getApplicationList().stream().anyMatch(applicationMetadata -> applicationMetadata.getName().equals(request.getApplicationName()));
        if (isApplicationExists) {
            return serviceFactory.getArgoHelper().updateApplication(argoApplication, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword,
                    request.getApplicationName());
        } else {
            return serviceFactory.getArgoHelper().createApplication(argoApplication, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
        }
    }

    /**
     * get all argo applications.
     *
     * @param customerId the customer id
     * @param argoToolId the argo tool id
     */
    public void validate(String customerId, String argoToolId) {
        LOGGER.debug("To validate the credentials for customerId {} and toolId {}", customerId, argoToolId);
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
     * @throws ResourcesNotAvailable        the resources not available
     * @throws UnsupportedEncodingException the unsupported encoding exception
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
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    private ArgoRepositoryItem getRepository(String toolId, String customerId, String repositoryUrl, ArgoToolDetails argoToolDetails, String argoPassword) throws UnsupportedEncodingException {
        LOGGER.debug("Starting to fetch Argo Repository {} ", repositoryUrl);
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
        LOGGER.debug("Starting to fetch All Argo Repositories for toolId{} ", argoToolId);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        return serviceFactory.getArgoHelper().getArgoRepositoriesList(argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Delete repository.
     *
     * @param request the request
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public void deleteRepository(CreateRepositoryRequest request) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to delete the repository request {}", request);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(request.getToolId(), request.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ToolDetails credentialToolDetails = serviceFactory.getConfigCollector().getToolDetails(request.getGitToolId(), request.getCustomerId());
        String repositoryUrl = "";
        if (null != credentialToolDetails) {
            ToolConfig toolConfig = credentialToolDetails.getConfiguration();
            if (toolConfig.isTwoFactorAuthentication()) {
                repositoryUrl = request.getSshUrl();
            } else {
                repositoryUrl = request.getHttpsUrl();
            }
        }
        serviceFactory.getArgoHelper().deleteArgoRepository(repositoryUrl, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
        LOGGER.debug("To Completed to delete the repository request {}", request);
    }

    /**
     * Creates the project.
     *
     * @param request the request
     * @return the response entity
     */
    public ResponseEntity<String> createProject(CreateProjectRequest request) {
        LOGGER.debug("To Starting to create/update the project {} ", request);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(request.getToolId(), request.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        ArgoApplicationMetadataList applicationMetadataList = getAllProjects(request.getToolId(), request.getCustomerId());
        serviceFactory.getRequestBuilder().createProjectRequest(request);
        boolean isProjectExists = applicationMetadataList.getApplicationList().stream()
                .anyMatch(applicationMetadata -> applicationMetadata.getName().equals(request.getProject().getMetadata().getName()));
        if (isProjectExists) {
            ArgoApplicationItem projectItem = getArgoProject(argoToolDetails, request.getProject().getMetadata().getName(), argoPassword);
            CreateProjectRequest updateProjectRequest = serviceFactory.getRequestBuilder().updateProjectRequest(projectItem, request);
            return serviceFactory.getArgoHelper().updateProject(updateProjectRequest, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
        } else {
            return serviceFactory.getArgoHelper().createProject(request, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
        }
    }

    /**
     * Gets the argo project.
     *
     * @param argoToolDetails the argo tool details
     * @param name            the name
     * @param argoPassword    the argo password
     * @return the argo project
     */
    private ArgoApplicationItem getArgoProject(ArgoToolDetails argoToolDetails, String name, String argoPassword) {
        LOGGER.debug("Starting to fetch Argo Projet {} ", name);
        return serviceFactory.getArgoHelper().getArgoProject(name, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Delete project.
     *
     * @param argoToolId  the argo tool id
     * @param customerId  the customer id
     * @param projectName the project name
     */
    public void deleteProject(String argoToolId, String customerId, String projectName) {
        LOGGER.debug("To Starting to delete the projectName {} and customerId {} and toolId {}", projectName, customerId, argoToolId);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        serviceFactory.getArgoHelper().deleteArgoProject(projectName, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
        LOGGER.debug("To Completed to delete the projectName {} and customerId {} and toolId {}", projectName, customerId, argoToolId);
    }

    /**
     * Creates the cluster.
     *
     * @param request the request
     * @return the response entity
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public ResponseEntity<String> createCluster(CreateCluster request) throws UnsupportedEncodingException {
        LOGGER.debug("Starting to create the cluster {} ", request);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(request.getArgoToolId(), request.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        CreateClusterRequest clusterItem = serviceFactory.getRequestBuilder().createClusterRequest(request);
        return serviceFactory.getArgoHelper().createCluster(clusterItem, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Update cluster.
     *
     * @param request the request
     * @return the response entity
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public ResponseEntity<String> updateCluster(CreateCluster request) throws UnsupportedEncodingException {
        LOGGER.debug("Starting to update the cluster {} ", request);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(request.getArgoToolId(), request.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        CreateClusterRequest clusterItem = serviceFactory.getRequestBuilder().createClusterRequest(request);
        return serviceFactory.getArgoHelper().updateCluster(clusterItem, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Delete cluster.
     *
     * @param request the request
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public void deleteCluster(CreateCluster request) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to delete the cluster request {}", request);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(request.getArgoToolId(), request.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        String serverUrl = "";
        if (AWS.equalsIgnoreCase(request.getPlatform().toUpperCase())) {
            AwsClusterDetails clusterDetails = serviceFactory.getConfigCollector().getAWSEKSClusterDetails(request.getPlatformToolId(), request.getCustomerId(), request.getClusterName());
            serverUrl = clusterDetails.getCluster().getEndpoint();
        } else if (AZURE.equalsIgnoreCase(request.getPlatform().toUpperCase())) {
            AzureClusterDetails azureClusterDetails = serviceFactory.getConfigCollector().getAKSClusterDetails(request);
            serverUrl = azureClusterDetails.getServer();
        }
        serviceFactory.getArgoHelper().deleteArgoCluster(serverUrl, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
        LOGGER.debug("Completed to delete the cluster request {}", request);
    }

}
