package com.opsera.integrator.argo.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import com.opsera.core.aspects.TrackExecutionTime;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.InvalidRequestException;
import com.opsera.integrator.argo.exceptions.ResourcesNotAvailable;
import com.opsera.integrator.argo.resources.ApprovalGateRequest;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationMetadataList;
import com.opsera.integrator.argo.resources.ArgoApplicationOperation;
import com.opsera.integrator.argo.resources.ArgoClusterList;
import com.opsera.integrator.argo.resources.ArgoRepositoriesList;
import com.opsera.integrator.argo.resources.CreateApplicationRequest;
import com.opsera.integrator.argo.resources.CreateCluster;
import com.opsera.integrator.argo.resources.CreateProjectRequest;
import com.opsera.integrator.argo.resources.CreateRepositoryRequest;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ProjectList;
import com.opsera.integrator.argo.resources.RepoRefs;
import com.opsera.integrator.argo.resources.Response;
import com.opsera.integrator.argo.resources.ValidateApplicationPathRequest;
import com.opsera.integrator.argo.resources.ValidationResponse;
import com.opsera.integrator.argo.resources.Project;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Controller.
 */
@RestController
@Api("Opsera API for the integration with Argo CD")
public class ArgoController {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoController.class);

    /** The service factory. */
    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * To check the service status.
     *
     * @return the string
     */
    @GetMapping("/status")
    @ApiOperation("To check the service status")
    public String status() {
        return "Spinnaker integrator service running";
    }

    /**
     * To get all the argo applications for the given argo domain.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all argo applications
     */
    @GetMapping(path = "v1.0/argo/applications")
    @ApiOperation("To get all the argo applications for the given argo domain")
    @TrackExecutionTime
    public ArgoApplicationMetadataList getAllArgoApplications(@RequestParam String argoToolId, @RequestParam String customerId) {
        LOGGER.info("Received getAllArgoApplications for argoId: {}", argoToolId);
        return serviceFactory.getArgoOrchestrator().getAllApplications(argoToolId, customerId);
    }

    /**
     * To get detailed information about an argo application.
     *
     * @param argoToolId      the argo tool id
     * @param customerId      the customer id
     * @param applicationName the application name
     * @return the argo application
     */
    @GetMapping(path = "v1.0/argo/application")
    @ApiOperation("To get detailed information about an argo application")
    @TrackExecutionTime
    public ArgoApplicationItem getArgoApplication(@RequestParam String argoToolId, @RequestParam String customerId, @RequestParam String applicationName) {
        LOGGER.info("Received getArgoApplication for argoId: {}, argoApplication: {}", argoToolId, applicationName);
        return serviceFactory.getArgoOrchestrator().getApplication(argoToolId, customerId, applicationName);
    }

    /**
     * To sync the argo application configured in Opsera pipeline.
     *
     * @param pipelineMetadata the pipeline metadata
     * @return the argo application operation
     */
    @PostMapping(path = "v1.0/argo/application/sync")
    @ApiOperation("To sync the argo application configured in Opsera pipeline")
    @TrackExecutionTime
    public ArgoApplicationOperation syncArgoApplication(@RequestBody OpseraPipelineMetadata pipelineMetadata) {
        LOGGER.info("Received syncArgoApplication for pipelineMetadata : {}", pipelineMetadata);
        return serviceFactory.getArgoOrchestrator().syncApplication(pipelineMetadata);
    }

    /**
     * To sync the argo application configured in Opsera pipeline.
     *
     * @param pipelineMetadata the pipeline metadata
     * @return the argo application operation
     */
    @PostMapping(path = "v2.0/argo/application/sync")
    @ApiOperation("To sync the argo application configured in Opsera pipeline")
    @TrackExecutionTime
    public void syncArgoApplicationV2(@RequestBody OpseraPipelineMetadata pipelineMetadata) {
        Long startTime = System.currentTimeMillis();
        try {
            LOGGER.info("Received syncArgoApplication for pipelineMetadata : {}", pipelineMetadata);
            serviceFactory.getArgoOrchestratorV2().syncApplication(pipelineMetadata);
        } finally {
            LOGGER.info("Completed syncArgoApplication, time taken to execute {} secs", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * To get all the argo clusters for the given argo domain.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all argo clusters
     */
    @GetMapping(path = "v1.0/argo/clusters")
    @ApiOperation("To get all the argo applications for the given argo domain")
    @TrackExecutionTime
    public ArgoClusterList getAllArgoClusters(@RequestParam String argoToolId, @RequestParam String customerId) {
        LOGGER.info("Received getAllArgoClusters for argoId: {}", argoToolId);
        return serviceFactory.getArgoOrchestrator().getAllClusters(argoToolId, customerId);
    }

    /**
     * To get all the argo projects for the given argo domain.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all argo projects
     */
    @GetMapping(path = "v1.0/argo/projects")
    @ApiOperation("To get all the argo projects for the given argo domain")
    @TrackExecutionTime
    public ProjectList getAllArgoProjects(@RequestParam String argoToolId, @RequestParam String customerId) {
        LOGGER.info("Received getAllArgoProjects for argoId: {}", argoToolId);
        return serviceFactory.getArgoOrchestrator().getAllProjects(argoToolId, customerId);
    }
    
    /**
     * Gets the argo project dtls.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @param projectName the project name
     * @return the argo project dtls
     */
    @GetMapping(path = "v1.0/argo/projects/{projectName}")
    @ApiOperation("To get specific project details")
    @TrackExecutionTime
    public Project getArgoProjectDtls(@RequestParam String argoToolId, @RequestParam String customerId, @PathVariable String projectName) {
        LOGGER.info("Received getArgoProjectDtls for argoId: {}, project: {}", argoToolId, projectName);
        return serviceFactory.getArgoOrchestrator().getProjectDtls(argoToolId, customerId, projectName);
    }

    /**
     * To create argo application.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping(path = "v1.0/argo/application/create")
    @ApiOperation("To create an argo application")
    @TrackExecutionTime
    public ResponseEntity<String> createArgoApplication(@RequestBody CreateApplicationRequest request) {
        LOGGER.info("Received createArgoApplication for : {}", request);
        return serviceFactory.getArgoOrchestrator().createApplication(request);
    }

    /**
     * To validate the tools input.
     *
     * @param customerId the customer id
     * @param toolId     the tool id
     * @return the response entity
     */
    @GetMapping("/validate")
    @ApiOperation("To Validate the user given details")
    @TrackExecutionTime
    public ResponseEntity<ValidationResponse> validate(@RequestParam(value = "customerId") String customerId, @RequestParam(value = "toolId") String toolId) {
        StopWatch stopwatch = serviceFactory.stopWatch();
        stopwatch.start();
        try {
            LOGGER.info("Starting to validate user details customerId {} and toolId {}", customerId, toolId);
            serviceFactory.getArgoOrchestrator().validate(customerId, toolId);
            LOGGER.info("Ending to validate user details customerId {} and toolId {}", customerId, toolId);
            return new ResponseEntity<>(ValidationResponse.builder().status(HttpStatus.OK.toString()).message("Connection was successful").build(), HttpStatus.OK);
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                String message = "Given UserName/Password is invalid";
                return new ResponseEntity<>(ValidationResponse.builder().status(HttpStatus.UNAUTHORIZED.toString()).message(message).build(), HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(ValidationResponse.builder().status(e.getStatusCode().toString()).message(e.getStatusText()).build(), e.getStatusCode());
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("URI is not absolute")) {
                String message = "Given Url is invalid";
                return new ResponseEntity<>(ValidationResponse.builder().status(HttpStatus.BAD_REQUEST.toString()).message(message).build(), HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(ValidationResponse.builder().status(HttpStatus.BAD_REQUEST.toString()).message(e.getMessage()).build(), HttpStatus.BAD_REQUEST);
        } catch (RestClientException e) {
            if (e.getCause().toString().contains("java.net.UnknownHostException:")) {
                String message = "Given domain " + e.getCause().toString().substring("java.net.UnknownHostException: ".length(), e.getCause().toString().length()) + " is invalid";
                return new ResponseEntity<>(ValidationResponse.builder().status(HttpStatus.BAD_REQUEST.toString()).message(message).build(), HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(ValidationResponse.builder().status(HttpStatus.BAD_REQUEST.toString()).message(e.getCause().toString()).build(), HttpStatus.BAD_REQUEST);
        } finally {
            stopwatch.stop();
            LOGGER.info("Completed to validate the tool connection in {} secs time to execute", stopwatch.getTotalTimeSeconds());
        }
    }

    /**
     * Endpoint for generate new token.
     *
     * @param customerId the customer id
     * @param toolId     the tool id
     * @return the response entity
     * @throws ResourcesNotAvailable the resources not available
     * @throws InterruptedException the interrupted exception
     */
    @GetMapping(path = "v1.0/generateNewToken")
    @ApiOperation("Gets argocd password ")
    @TrackExecutionTime
    public ResponseEntity<String> generateNewToken(@RequestParam String customerId, @RequestParam String toolId) throws ResourcesNotAvailable, InterruptedException {
        LOGGER.info("Starting the generateNewToken a argocd");
        LOGGER.info("Received generateNewToken a argocd request customerId {} and url {}", customerId, toolId);
        String token = serviceFactory.getArgoOrchestrator().generateNewToken(customerId, toolId);
        LOGGER.info("Successfully generate new token for customerId {} and toolId {}", customerId, toolId);
        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    /**
     * To delete the argo application.
     *
     * @param argoToolId      the argo tool id
     * @param customerId      the customer id
     * @param applicationName the application name
     * @return the response entity
     */
    @DeleteMapping(path = "v1.0/argo/application")
    @ApiOperation("To delete application")
    @TrackExecutionTime
    public ResponseEntity<String> deleteArgoApplication(@RequestParam String argoToolId, @RequestParam String customerId, @RequestParam String applicationName) {
        LOGGER.info("Received getArgoApplication for argoId: {}, argoApplication: {}", argoToolId, applicationName);
        serviceFactory.getArgoOrchestrator().deleteApplication(argoToolId, customerId, applicationName);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    /**
     * Creates the argo repository.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping(path = "v1.0/argo/repository/create")
    @ApiOperation("To create an argo repository")
    @TrackExecutionTime
    public ResponseEntity<String> createArgoRepository(@RequestBody CreateRepositoryRequest request) {

        LOGGER.info("Received createArgoRepository for : {}", request);
        return serviceFactory.getArgoOrchestrator().createRepository(request);

    }

    /**
     * Gets the all argo repositories.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all argo repositories
     */
    @GetMapping(path = "v1.0/argo/repositories")
    @ApiOperation("To get all the argo repositories for the given argo domain")
    @TrackExecutionTime
    public ArgoRepositoriesList getAllArgoRepositories(@RequestParam String argoToolId, @RequestParam String customerId) {
        LOGGER.info("Received getAllArgoRepositories for argoId: {}", argoToolId);
        return serviceFactory.getArgoOrchestrator().getAllArgoRepositories(argoToolId, customerId);
    }

    /**
     * Delete argo repository.
     *
     * @param request the request
     * @return the response entity
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @PostMapping(path = "v1.0/argo/repository/delete")
    @ApiOperation("To delete the repository")
    @TrackExecutionTime
    public ResponseEntity<String> deleteArgoRepository(@RequestBody CreateRepositoryRequest request) throws IOException {
        LOGGER.info("Received deleteArgoRepository request {}", request);
        serviceFactory.getArgoOrchestrator().deleteRepository(request);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    /**
     * Creates the argo project.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping(path = "v1.0/argo/project/create")
    @ApiOperation("To create an argo project")
    @TrackExecutionTime
    public ResponseEntity<String> createArgoProject(@RequestBody CreateProjectRequest request) {
        LOGGER.info("Received createArgoProject for : {}", request);
        return serviceFactory.getArgoOrchestrator().createProject(request);
    }

    /**
     * Delete argo project.
     *
     * @param argoToolId  the argo tool id
     * @param customerId  the customer id
     * @param projectName the project name
     * @return the response entity
     */
    @DeleteMapping(path = "v1.0/argo/projects/{projectName}")
    @ApiOperation("To delete project")
    @TrackExecutionTime
    public ResponseEntity<String> deleteArgoProject(@RequestParam String argoToolId, @RequestParam String customerId, @PathVariable String projectName) {
        LOGGER.info("Received deleteArgoProject for argoId: {}, projectName: {}", argoToolId, projectName);
        serviceFactory.getArgoOrchestrator().deleteProject(argoToolId, customerId, projectName);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    /**
     * Creates the argo cluster.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping(path = "v1.0/argo/clusters")
    @ApiOperation("To create an argo cluster")
    @TrackExecutionTime
    public ResponseEntity<String> createArgoCluster(@RequestBody CreateCluster request) {
        LOGGER.info("Received createArgoCluster request {}", request);
        return serviceFactory.getArgoOrchestrator().createCluster(request);
    }

    /**
     * Update argo cluster.
     *
     * @param request the request
     * @return the response entity
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @PutMapping(path = "v1.0/argo/clusters")
    @ApiOperation("To update an argo cluster")
    @TrackExecutionTime
    public ResponseEntity<String> updateArgoCluster(@RequestBody CreateCluster request) throws IOException {
        LOGGER.info("Received updateArgoCluster request {}", request);
        return serviceFactory.getArgoOrchestrator().updateCluster(request);
    }

    /**
     * Delete argo cluster.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @param server     the server
     * @return the response entity
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InvalidRequestException      the invalid request exception
     */
    @DeleteMapping(path = "v1.0/argo/clusters")
    @ApiOperation("To delete an argo cluster")
    @TrackExecutionTime
    public ResponseEntity<String> deleteArgoCluster(@RequestParam String argoToolId, @RequestParam String customerId, @RequestParam String server) throws IOException {
        LOGGER.info("Received deleteArgoCluster request for argoToolId: {}, customerId: {}, server: {}", argoToolId, customerId, server);
        serviceFactory.getArgoOrchestrator().deleteCluster(argoToolId, customerId, server);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    /**
     * Creates the namespace.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping(path = "v1.0/argo/namespace/create")
    @ApiOperation("To create an argo project")
    @TrackExecutionTime
    public ResponseEntity<Response> createNamespace(@RequestBody CreateCluster request) {
        LOGGER.info("Received createNamespace for : {}", request);
        Response response = serviceFactory.getArgoOrchestrator().createNamespace(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Approval or reject promotion.
     *
     * @param request the request
     * @return the string
     */
    @PostMapping(path = "v1.0/argo/application/approvalgate")
    @ApiOperation("To sync the argo application configured in Opsera pipeline")
    @TrackExecutionTime
    public String approvalOrRejectPromotion(@RequestBody ApprovalGateRequest request) {
        LOGGER.info("Received approvalOrRejectPromotion for pipelineMetadata : {}", request);
        serviceFactory.getArgoOrchestratorV2().promoteOrAbortRolloutDeployment(request);
        return "Request Submitted";
    }
    
    /**
     * Gets the repo branches and tags.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @param repoUrl the repo url
     * @return the repo branches and tags
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    @GetMapping(path = "v1.0/argo/repositories/refs")
    @ApiOperation("To retrieve branches and tags of repositories")
    @TrackExecutionTime
    public RepoRefs getRepoBranchesAndTags(@RequestParam String argoToolId, @RequestParam String customerId, @RequestParam String repoUrl) throws UnsupportedEncodingException {
        LOGGER.info("Received getRepoBranches request for argoTool: {}", argoToolId);
        return serviceFactory.getArgoOrchestrator().getRepoBranchesAndTagsList(argoToolId, customerId, repoUrl);
    }
    
    /**
     * Validate application path.
     *
     * @param request the request
     * @return the response
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    @PostMapping(path = "v1.0/argo/application/validatepath")
    @ApiOperation("To check application directory path correctness")
    @TrackExecutionTime
    public Response validateApplicationPath(@RequestBody ValidateApplicationPathRequest request) throws UnsupportedEncodingException {
        LOGGER.info("Received validate applicaton path request : {}", request);
        serviceFactory.getArgoOrchestrator().validateAppPath(request);
        return Response.builder().message("Application path is valid").status("Success").build();
    }
}
