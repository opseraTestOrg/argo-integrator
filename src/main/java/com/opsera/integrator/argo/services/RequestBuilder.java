package com.opsera.integrator.argo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.opsera.integrator.argo.resources.ArgoApplicationDestination;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationMetadata;
import com.opsera.integrator.argo.resources.ArgoApplicationSource;
import com.opsera.integrator.argo.resources.ArgoApplicationSpec;
import com.opsera.integrator.argo.resources.ArgoProjectMetadata;
import com.opsera.integrator.argo.resources.ArgoRepositoryItem;
import com.opsera.integrator.argo.resources.CreateApplicationRequest;
import com.opsera.integrator.argo.resources.CreateProjectRequest;
import com.opsera.integrator.argo.resources.CreateRepositoryRequest;
import com.opsera.integrator.argo.resources.Project;
import com.opsera.integrator.argo.resources.ToolConfig;

/**
 * The Class RequestBuilder.
 */
@Service
public class RequestBuilder {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(RequestBuilder.class);

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
    public ArgoRepositoryItem createRepositoryRequest(CreateRepositoryRequest request, ToolConfig toolConfig, String secret) {
        LOGGER.debug("Starting to create Argo Repository Request {}", request);
        ArgoRepositoryItem argoRepositoryItem = new ArgoRepositoryItem();
        argoRepositoryItem.setName(request.getRepositoryName());
        argoRepositoryItem.setType(request.getRepositoryType());
        if (toolConfig.isTwoFactorAuthentication()) {
            argoRepositoryItem.setRepo(request.getSshUrl());
            argoRepositoryItem.setSshPrivateKey(secret);
        } else {
            argoRepositoryItem.setRepo(request.getHttpsUrl());
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

}
