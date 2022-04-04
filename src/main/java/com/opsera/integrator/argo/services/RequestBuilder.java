package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ASTERISK;
import static com.opsera.integrator.argo.resources.Constants.AWS;
import static com.opsera.integrator.argo.resources.Constants.AZURE;
import static com.opsera.integrator.argo.resources.Constants.NAMESPACE_OPSERA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static com.opsera.integrator.argo.resources.Constants.AZURE_DEVOPS_TOOL_IDENTIFIER;
import static com.opsera.integrator.argo.resources.Constants.OPSERA_USER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ArgoApplicationDestination;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationMetadata;
import com.opsera.integrator.argo.resources.ArgoApplicationSource;
import com.opsera.integrator.argo.resources.ArgoApplicationSpec;
import com.opsera.integrator.argo.resources.ArgoClusterConfig;
import com.opsera.integrator.argo.resources.ArgoProjectClusterResourceWhiteList;
import com.opsera.integrator.argo.resources.ArgoProjectMetadata;
import com.opsera.integrator.argo.resources.ArgoProjectNamespaceResourceBlacklist;
import com.opsera.integrator.argo.resources.ArgoProjectNamespaceResourceWhitelist;
import com.opsera.integrator.argo.resources.ArgoRepositoryItem;
import com.opsera.integrator.argo.resources.AwsClusterDetails;
import com.opsera.integrator.argo.resources.AzureClusterDetails;
import com.opsera.integrator.argo.resources.CreateApplicationRequest;
import com.opsera.integrator.argo.resources.CreateCluster;
import com.opsera.integrator.argo.resources.CreateClusterRequest;
import com.opsera.integrator.argo.resources.CreateProjectRequest;
import com.opsera.integrator.argo.resources.CreateRepositoryRequest;
import com.opsera.integrator.argo.resources.Project;
import com.opsera.integrator.argo.resources.TLSClientConfig;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.integrator.argo.resources.ToolDetails;

/**
 * The Class RequestBuilder.
 */
@Service
public class RequestBuilder {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(RequestBuilder.class);

    /** The service factory. */
    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * Creates the application request.
     *
     * @param request the request
     * @return the argo application item
     */
    public ArgoApplicationItem createApplicationRequest(CreateApplicationRequest request) {
        LOGGER.debug("Starting to create Argo Application Request {}", request);
        ArgoApplicationItem argoApplication = new ArgoApplicationItem();

        ArgoApplicationMetadata metadata = new ArgoApplicationMetadata();
        metadata.setName(request.getApplicationName());

        ArgoApplicationSpec spec = new ArgoApplicationSpec();
        ArgoApplicationSource source = new ArgoApplicationSource();
        ArgoApplicationDestination destination = new ArgoApplicationDestination();
        source.setRepoURL(request.getGitUrl());
        source.setPath(request.getGitPath());
        source.setTargetRevision(request.getBranchName());
        destination.setNamespace(request.getNamespace());
        destination.setServer(request.getCluster());
        spec.setSource(source);
        spec.setDestination(destination);

        argoApplication.setMetadata(metadata);
        argoApplication.setSpec(spec);
        return argoApplication;
    }

    /**
     * Creates the repository request.
     *
     * @param request    the request
     * @param toolConfig the tool config
     * @param secret     the secret
     * @return the argo repository item
     */
    public ArgoRepositoryItem createRepositoryRequest(CreateRepositoryRequest request, ToolDetails toolDetails, String secret) {
        LOGGER.debug("Starting to create Argo Repository Request {}", request);
        ArgoRepositoryItem argoRepositoryItem = new ArgoRepositoryItem();
        argoRepositoryItem.setName(request.getRepositoryName());
        argoRepositoryItem.setType(request.getRepositoryType());
        ToolConfig toolConfig = toolDetails.getConfiguration();
        if (toolConfig.isTwoFactorAuthentication()) {
            argoRepositoryItem.setRepo(request.getSshUrl());
            argoRepositoryItem.setSshPrivateKey(secret);
        } else {
            argoRepositoryItem.setRepo(request.getHttpsUrl());
            if (AZURE_DEVOPS_TOOL_IDENTIFIER.equalsIgnoreCase(toolDetails.getToolIdentifier()) && StringUtils.isEmpty(toolConfig.getAccountUsername()))
                argoRepositoryItem.setUsername(OPSERA_USER);
            else
                argoRepositoryItem.setUsername(toolConfig.getAccountUsername());
            argoRepositoryItem.setPassword(secret);
        }
        return argoRepositoryItem;
    }

    /**
     * Update project request.
     *
     * @param projectItem the project item
     * @param request     the request
     * @return the creates the project request
     */
    public CreateProjectRequest updateProjectRequest(ArgoApplicationItem projectItem, CreateProjectRequest request) {
        LOGGER.debug("Starting to update Argo Project Request {}", request);
        CreateProjectRequest createProjectRequest = new CreateProjectRequest();
        Project project = new Project();
        ArgoProjectMetadata metaData = new ArgoProjectMetadata();
        metaData.setName(projectItem.getMetadata().getName());
        metaData.setResourceVersion(projectItem.getMetadata().getResourceVersion());
        project.setMetadata(metaData);
        project.setSpec(request.getProject().getSpec());
        createProjectRequest.setProject(project);
        return createProjectRequest;
    }

    /**
     * Creates the cluster request.
     *
     * @param request the request
     * @return the creates the cluster request
     * @throws IOException 
     */
    public CreateClusterRequest createClusterRequest(CreateCluster request) throws IOException {
        LOGGER.debug("Starting to create Argo cluster Request {}", request);
        AwsClusterDetails awsClusterDetails;
        AzureClusterDetails azureClusterDetails;
        CreateClusterRequest createClusterRequest = new CreateClusterRequest();
        TLSClientConfig tlsClientConfig = new TLSClientConfig();
        ArgoClusterConfig argoClusterConfig = new ArgoClusterConfig();
        if (AWS.equalsIgnoreCase(request.getPlatform().toUpperCase())) {
            awsClusterDetails = serviceFactory.getConfigCollector().getAWSEKSClusterDetails(request.getPlatformToolId(), request.getCustomerId(), request.getClusterName());
            createClusterRequest.setServer(awsClusterDetails.getCluster().getEndpoint());
            createClusterRequest.setName(awsClusterDetails.getCluster().getName());
            tlsClientConfig.setCaData(awsClusterDetails.getCluster().getCertificateAuthority().getData());
            argoClusterConfig.setTlsClientConfig(tlsClientConfig);
            String eksToken = serviceFactory.getConfigCollector().getAWSEKSClusterToken(request);
            String bearerToken = serviceFactory.getConfigCollector().getBearerToken(createClusterRequest.getServer(), eksToken, request.getArgoToolId(), NAMESPACE_OPSERA);
            argoClusterConfig.setBearerToken(bearerToken);
            createClusterRequest.setConfig(argoClusterConfig);
        } else if (AZURE.equalsIgnoreCase(request.getPlatform().toUpperCase())) {
            azureClusterDetails = serviceFactory.getConfigCollector().getAKSClusterDetails(request);
            createClusterRequest.setServer(azureClusterDetails.getServer());
            createClusterRequest.setName(azureClusterDetails.getName());
            tlsClientConfig.setCaData(azureClusterDetails.getCaData());
            tlsClientConfig.setCertData(azureClusterDetails.getCertData());
            tlsClientConfig.setKeyData(azureClusterDetails.getKeyData());
            argoClusterConfig.setTlsClientConfig(tlsClientConfig);
            argoClusterConfig.setBearerToken(azureClusterDetails.getBearerToken());
            createClusterRequest.setConfig(argoClusterConfig);
        }
        return createClusterRequest;
    }

    /**
     * Creates the project request.
     *
     * @param request the request
     */
    public void createProjectRequest(CreateProjectRequest request) {
        LOGGER.debug("Starting to Build Request for Argo Project {}", request);
        List<ArgoProjectClusterResourceWhiteList> clusterResourceWhitelist = setClusterResourceWhiteList(request);
        request.getProject().getSpec().setClusterResourceWhitelist(clusterResourceWhitelist);
        List<ArgoProjectNamespaceResourceBlacklist> namespaceResourceBlacklist = setNamespaceResourceBlacklist(request);
        request.getProject().getSpec().setNamespaceResourceBlacklist(namespaceResourceBlacklist);
        List<ArgoProjectNamespaceResourceWhitelist> namespaceResourceWhitelist = setNamespaceResourceWhitelist(request);
        request.getProject().getSpec().setNamespaceResourceWhitelist(namespaceResourceWhitelist);
        LOGGER.debug("Completed to Build Request for Argo Project {}", request);
    }

    /**
     * Sets the cluster resource white list.
     *
     * @param request the request
     * @return the list
     */
    private List<ArgoProjectClusterResourceWhiteList> setClusterResourceWhiteList(CreateProjectRequest request) {
        LOGGER.debug("Starting to set Cluster Resource WhiteList for Argo Project {}", request);
        List<ArgoProjectClusterResourceWhiteList> clusterResourceWhitelist = new ArrayList<>();
        ArgoProjectClusterResourceWhiteList clusterResourceWhite;
        if (request.getProject().getSpec().getClusterResourceWhitelist() != null) {
            for (ArgoProjectClusterResourceWhiteList clusterResource : request.getProject().getSpec().getClusterResourceWhitelist()) {
                clusterResourceWhite = new ArgoProjectClusterResourceWhiteList();
                if (clusterResource.getGroup().isEmpty()) {
                    clusterResourceWhite.setGroup(ASTERISK);
                } else {
                    clusterResourceWhite.setGroup(clusterResource.getGroup());
                }
                if (clusterResource.getKind().isEmpty()) {
                    clusterResourceWhite.setKind(ASTERISK);
                } else {
                    clusterResourceWhite.setKind(clusterResource.getKind());
                }
                clusterResourceWhitelist.add(clusterResourceWhite);
            }
        } else {
            clusterResourceWhite = new ArgoProjectClusterResourceWhiteList();
            clusterResourceWhite.setGroup(ASTERISK);
            clusterResourceWhite.setKind(ASTERISK);
            clusterResourceWhitelist.add(clusterResourceWhite);
        }
        return clusterResourceWhitelist;
    }

    /**
     * Sets the namespace resource blacklist.
     *
     * @param request the request
     * @return the list
     */
    private List<ArgoProjectNamespaceResourceBlacklist> setNamespaceResourceBlacklist(CreateProjectRequest request) {
        LOGGER.debug("Starting to set Namespace Resource Blacklist for Argo Project {}", request);
        List<ArgoProjectNamespaceResourceBlacklist> namespaceResourceBlacklist = new ArrayList<>();
        ArgoProjectNamespaceResourceBlacklist namespaceResourceBlack;
        if (request.getProject().getSpec().getNamespaceResourceBlacklist() != null) {
            for (ArgoProjectNamespaceResourceBlacklist namespaceResource : request.getProject().getSpec().getNamespaceResourceBlacklist()) {
                namespaceResourceBlack = new ArgoProjectNamespaceResourceBlacklist();
                if (namespaceResource.getGroup().isEmpty()) {
                    namespaceResourceBlack.setGroup(ASTERISK);
                } else {
                    namespaceResourceBlack.setGroup(namespaceResource.getGroup());
                }
                if (namespaceResource.getKind().isEmpty()) {
                    namespaceResourceBlack.setKind(ASTERISK);
                } else {
                    namespaceResourceBlack.setKind(namespaceResource.getKind());
                }
                namespaceResourceBlacklist.add(namespaceResourceBlack);
            }
        } else {
            namespaceResourceBlack = new ArgoProjectNamespaceResourceBlacklist();
            namespaceResourceBlack.setGroup(ASTERISK);
            namespaceResourceBlack.setKind(ASTERISK);
            namespaceResourceBlacklist.add(namespaceResourceBlack);
        }
        return namespaceResourceBlacklist;
    }

    /**
     * Sets the namespace resource whitelist.
     *
     * @param request the request
     * @return the list
     */
    private List<ArgoProjectNamespaceResourceWhitelist> setNamespaceResourceWhitelist(CreateProjectRequest request) {
        LOGGER.debug("Starting to set Namespace Resource Whitelist for Argo Project {}", request);
        List<ArgoProjectNamespaceResourceWhitelist> namespaceResourceWhitelist = new ArrayList<>();
        ArgoProjectNamespaceResourceWhitelist namespaceResourceWhite;
        if (request.getProject().getSpec().getNamespaceResourceWhitelist() != null) {
            for (ArgoProjectNamespaceResourceWhitelist projectNamespaceResource : request.getProject().getSpec().getNamespaceResourceWhitelist()) {
                namespaceResourceWhite = new ArgoProjectNamespaceResourceWhitelist();
                if (projectNamespaceResource.getGroup().isEmpty()) {
                    namespaceResourceWhite.setGroup(ASTERISK);
                } else {
                    namespaceResourceWhite.setGroup(projectNamespaceResource.getGroup());
                }
                if (projectNamespaceResource.getKind().isEmpty()) {
                    namespaceResourceWhite.setKind(ASTERISK);
                } else {
                    namespaceResourceWhite.setKind(projectNamespaceResource.getKind());
                }
                namespaceResourceWhitelist.add(namespaceResourceWhite);
            }
        } else {
            namespaceResourceWhite = new ArgoProjectNamespaceResourceWhitelist();
            namespaceResourceWhite.setGroup(ASTERISK);
            namespaceResourceWhite.setKind(ASTERISK);
            namespaceResourceWhitelist.add(namespaceResourceWhite);
        }
        return namespaceResourceWhitelist;
    }
}
