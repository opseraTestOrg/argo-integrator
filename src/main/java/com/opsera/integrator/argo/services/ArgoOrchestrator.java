package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.FAILED;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opsera.core.exception.ServiceException;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationMetadataList;
import com.opsera.integrator.argo.resources.ArgoApplicationOperation;
import com.opsera.integrator.argo.resources.ArgoApplicationsList;
import com.opsera.integrator.argo.resources.ArgoClusterList;
import com.opsera.integrator.argo.resources.ArgoRepositoriesList;
import com.opsera.integrator.argo.resources.ArgoRepositoryItem;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.CreateApplicationRequest;
import com.opsera.integrator.argo.resources.CreateCluster;
import com.opsera.integrator.argo.resources.CreateClusterRequest;
import com.opsera.integrator.argo.resources.CreateProjectRequest;
import com.opsera.integrator.argo.resources.CreateRepositoryRequest;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.integrator.argo.resources.ToolDetails;

import io.kubernetes.client.openapi.ApiException;

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
     * @throws IOException 
     */
    public ArgoApplicationMetadataList getAllApplications(String argoToolId, String customerId) throws IOException {
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
     * @throws IOException 
     */
    public ArgoClusterList getAllClusters(String argoToolId, String customerId) throws IOException {
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
     * @throws IOException 
     */
    public ArgoApplicationMetadataList getAllProjects(String argoToolId, String customerId) throws IOException {
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
     * @throws IOException 
     */
    public ArgoApplicationItem getApplication(String argoToolId, String customerId, String applicationName) throws IOException {
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
     * @throws IOException 
     */
    public ArgoApplicationOperation syncApplication(OpseraPipelineMetadata pipelineMetadata) throws IOException {
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
     * @throws IOException 
     */
    public String createApplication(CreateApplicationRequest request) throws IOException {
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
     * @throws IOException 
     */
    public void validate(String customerId, String argoToolId) throws IOException {
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
    public String generateNewToken(String customerId, String toolId) throws ServiceException {
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
     * @throws IOException 
     */
    public void deleteApplication(String argoToolId, String customerId, String applicationName) throws IOException {
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
     * @throws IOException 
     * @throws ResourcesNotAvailable        the resources not available
     */
    public String createRepository(CreateRepositoryRequest request) throws ServiceException, IOException {
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
            ArgoRepositoryItem argoApplication = serviceFactory.getRequestBuilder().createRepositoryRequest(request, credentialToolDetails, secret);
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
     * @throws IOException 
     */
    private ArgoRepositoryItem getRepository(String toolId, String customerId, String repositoryUrl, ArgoToolDetails argoToolDetails, String argoPassword) throws IOException {
        LOGGER.debug("Starting to fetch Argo Repository {} ", repositoryUrl);
        return serviceFactory.getArgoHelper().getArgoRepository(repositoryUrl, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Gets the all argo repositories.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all argo repositories
     * @throws IOException 
     */
    public ArgoRepositoriesList getAllArgoRepositories(String argoToolId, String customerId) throws IOException {
        LOGGER.debug("Starting to fetch All Argo Repositories for toolId{} ", argoToolId);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        return serviceFactory.getArgoHelper().getArgoRepositoriesList(argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Delete repository.
     *
     * @param request the request
     * @throws IOException 
     */
    public void deleteRepository(CreateRepositoryRequest request) throws IOException {
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
     * @throws IOException 
     */
    public String createProject(CreateProjectRequest request) throws IOException {
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
     * @throws IOException 
     */
    private ArgoApplicationItem getArgoProject(ArgoToolDetails argoToolDetails, String name, String argoPassword) throws IOException {
        LOGGER.debug("Starting to fetch Argo Projet {} ", name);
        return serviceFactory.getArgoHelper().getArgoProject(name, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    /**
     * Delete project.
     *
     * @param argoToolId  the argo tool id
     * @param customerId  the customer id
     * @param projectName the project name
     * @throws IOException 
     */
    public void deleteProject(String argoToolId, String customerId, String projectName) throws IOException {
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
     * @throws IOException 
     */
    public String createCluster(CreateCluster request) throws IOException {
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
     * @throws IOException 
     */
    public String updateCluster(CreateCluster request) throws IOException {
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
     * @throws IOException 
     * @throws ApiException 
     */
    public void deleteCluster(String argoToolId, String customerId, String serverUrl) throws IOException {
        LOGGER.debug("Starting to delete the cluster {} for customerId {} and toolId {}", serverUrl, customerId, argoToolId);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolId, customerId);
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        serviceFactory.getArgoHelper().deleteArgoCluster(serverUrl, argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

    public String getArgoLog(OpseraPipelineMetadata pipelineMetadata) throws IOException {
        LOGGER.debug("Starting to get Argo Application Log for request{}", pipelineMetadata);
        ToolConfig argoToolConfig = serviceFactory.getConfigCollector().getArgoDetails(pipelineMetadata);
        ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolConfig.getToolConfigId(), pipelineMetadata.getCustomerId());
        String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        return serviceFactory.getArgoHelper().getArgoApplicationLog(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration().getToolURL(),
                argoToolDetails.getConfiguration().getUserName(), argoPassword);
    }

}
