package com.opsera.integrator.argo.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import com.opsera.core.aspects.TrackExecutionTime;
import com.opsera.core.exception.ServiceException;
import com.opsera.integrator.argo.config.IServiceFactory;
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
import com.opsera.integrator.argo.resources.ValidationResponse;

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
     * @throws IOException
     */
    @GetMapping(path = "v1.0/argo/applications")
    @ApiOperation("To get all the argo applications for the given argo domain")
    @TrackExecutionTime
    public ArgoApplicationMetadataList getAllArgoApplications(@RequestParam String argoToolId, @RequestParam String customerId) throws IOException {
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
     * @throws IOException
     */
    @GetMapping(path = "v1.0/argo/application")
    @ApiOperation("To get detailed information about an argo application")
    @TrackExecutionTime
    public ArgoApplicationItem getArgoApplication(@RequestParam String argoToolId, @RequestParam String customerId, @RequestParam String applicationName) throws IOException {
        LOGGER.info("Received getArgoApplication for argoId: {}, argoApplication: {}", argoToolId, applicationName);
        return serviceFactory.getArgoOrchestrator().getApplication(argoToolId, customerId, applicationName);
    }

    /**
     * To sync the argo application configured in Opsera pipeline.
     *
     * @param pipelineMetadata the pipeline metadata
     * @return the argo application operation
     * @throws IOException
     */
    @PostMapping(path = "v1.0/argo/application/sync")
    @ApiOperation("To sync the argo application configured in Opsera pipeline")
    @TrackExecutionTime
    public ArgoApplicationOperation syncArgoApplication(@RequestBody OpseraPipelineMetadata pipelineMetadata) throws IOException {
        LOGGER.info("Received syncArgoApplication for pipelineMetadata : {}", pipelineMetadata);
        return serviceFactory.getArgoOrchestrator().syncApplication(pipelineMetadata);
    }

    /**
     * To get all the argo clusters for the given argo domain.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all argo clusters
     * @throws IOException
     */
    @GetMapping(path = "v1.0/argo/clusters")
    @ApiOperation("To get all the argo applications for the given argo domain")
    @TrackExecutionTime
    public ArgoClusterList getAllArgoClusters(@RequestParam String argoToolId, @RequestParam String customerId) throws IOException {
        LOGGER.info("Received getAllArgoClusters for argoId: {}", argoToolId);
        return serviceFactory.getArgoOrchestrator().getAllClusters(argoToolId, customerId);
    }

    /**
     * To get all the argo projects for the given argo domain.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all argo projects
     * @throws IOException
     */
    @GetMapping(path = "v1.0/argo/projects")
    @ApiOperation("To get all the argo projects for the given argo domain")
    @TrackExecutionTime
    public ArgoApplicationMetadataList getAllArgoProjects(@RequestParam String argoToolId, @RequestParam String customerId) throws IOException {
        LOGGER.info("Received getAllArgoProjects for argoId: {}", argoToolId);
        return serviceFactory.getArgoOrchestrator().getAllProjects(argoToolId, customerId);
    }

    /**
     * To create argo application.
     *
     * @param request the request
     * @return the response entity
     * @throws IOException
     */
    @PostMapping(path = "v1.0/argo/application/create")
    @ApiOperation("To create an argo application")
    @TrackExecutionTime
    public ResponseEntity<String> createArgoApplication(@RequestBody CreateApplicationRequest request) throws IOException {
        LOGGER.info("Received createArgoApplication for : {}", request);
        return new ResponseEntity<String>(serviceFactory.getArgoOrchestrator().createApplication(request), HttpStatus.OK);
    }

    /**
     * To validate the tools input.
     *
     * @param customerId the customer id
     * @param toolId     the tool id
     * @return the response entity
     * @throws IOException
     */
    @GetMapping("/validate")
    @ApiOperation("To Validate the user given details")
    @TrackExecutionTime
    public ResponseEntity<ValidationResponse> validate(@RequestParam(value = "customerId") String customerId, @RequestParam(value = "toolId") String toolId) throws IOException {
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
        }
    }

    /**
     * Endpoint for generate new token.
     *
     * @param customerId the customer id
     * @param toolId     the tool id
     * @return the response entity
     * @throws ResourcesNotAvailable the resources not available
     */
    @GetMapping(path = "v1.0/generateNewToken")
    @ApiOperation("Gets argocd password ")
    @TrackExecutionTime
    public ResponseEntity<String> generateNewToken(@RequestParam String customerId, @RequestParam String toolId) {
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
     * @throws IOException
     */
    @DeleteMapping(path = "v1.0/argo/application")
    @ApiOperation("To delete application")
    @TrackExecutionTime
    public ResponseEntity<String> deleteArgoApplication(@RequestParam String argoToolId, @RequestParam String customerId, @RequestParam String applicationName) throws IOException {
        LOGGER.info("Received getArgoApplication for argoId: {}, argoApplication: {}", argoToolId, applicationName);
        serviceFactory.getArgoOrchestrator().deleteApplication(argoToolId, customerId, applicationName);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    /**
     * Creates the argo repository.
     *
     * @param request the request
     * @return the response entity
     * @throws IOException
     * @throws ServiceException
     * @throws ResourcesNotAvailable the resources not available
     */
    @PostMapping(path = "v1.0/argo/repository/create")
    @ApiOperation("To create an argo repository")
    @TrackExecutionTime
    public ResponseEntity<String> createArgoRepository(@RequestBody CreateRepositoryRequest request) throws ServiceException, IOException {
        LOGGER.info("Received createArgoRepository for : {}", request);
        return new ResponseEntity<String>(serviceFactory.getArgoOrchestrator().createRepository(request), HttpStatus.OK);
    }

    /**
     * Gets the all argo repositories.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the all argo repositories
     * @throws IOException
     */
    @GetMapping(path = "v1.0/argo/repositories")
    @ApiOperation("To get all the argo repositories for the given argo domain")
    @TrackExecutionTime
    public ResponseEntity<ArgoRepositoriesList> getAllArgoRepositories(@RequestParam String argoToolId, @RequestParam String customerId) throws IOException {
        LOGGER.info("Received getAllArgoRepositories for argoId: {}", argoToolId);
        return new ResponseEntity<ArgoRepositoriesList>(serviceFactory.getArgoOrchestrator().getAllArgoRepositories(argoToolId, customerId), HttpStatus.OK);
    }

    /**
     * Delete argo repository.
     *
     * @param request the request
     * @return the response entity
     * @throws IOException
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
     * @throws IOException
     * @throws ResourcesNotAvailable the resources not available
     */
    @PostMapping(path = "v1.0/argo/project/create")
    @ApiOperation("To create an argo project")
    @TrackExecutionTime
    public ResponseEntity<String> createArgoProject(@RequestBody CreateProjectRequest request) throws IOException {
        LOGGER.info("Received createArgoProject for : {}", request);
        return new ResponseEntity<String>(serviceFactory.getArgoOrchestrator().createProject(request), HttpStatus.OK);
    }

    /**
     * Delete argo project.
     *
     * @param argoToolId  the argo tool id
     * @param customerId  the customer id
     * @param projectName the project name
     * @return the response entity
     * @throws IOException
     */
    @DeleteMapping(path = "v1.0/argo/project")
    @ApiOperation("To delete project")
    @TrackExecutionTime
    public ResponseEntity<String> deleteArgoProject(@RequestParam String argoToolId, @RequestParam String customerId, @RequestParam String projectName) throws IOException {
        LOGGER.info("Received deleteArgoProject for argoId: {}, peojectName: {}", argoToolId, projectName);
        serviceFactory.getArgoOrchestrator().deleteProject(argoToolId, customerId, projectName);
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    /**
     * Creates the argo cluster.
     *
     * @param request the request
     * @return the response entity
     * @throws IOException
     * @throws ResourcesNotAvailable the resources not available
     */
    @PostMapping(path = "v1.0/argo/clusters")
    @ApiOperation("To create an argo cluster")
    @TrackExecutionTime
    public ResponseEntity<String> createArgoCluster(@RequestBody CreateCluster request) throws IOException {
        LOGGER.info("Received createArgoCluster request {}", request);
        return new ResponseEntity<String>(serviceFactory.getArgoOrchestrator().createCluster(request), HttpStatus.OK);
    }

    /**
     * Update argo cluster.
     *
     * @param request the request
     * @return the response entity
     * @throws IOException
     * @throws ResourcesNotAvailable the resources not available
     */
    @PutMapping(path = "v1.0/argo/clusters")
    @ApiOperation("To update an argo cluster")
    @TrackExecutionTime
    public ResponseEntity<String> updateArgoCluster(@RequestBody CreateCluster request) throws IOException {
        LOGGER.info("Received updateArgoCluster request {}", request);
        return new ResponseEntity<String>(serviceFactory.getArgoOrchestrator().updateCluster(request), HttpStatus.OK);
    }

    /**
     * Delete argo cluster.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @param server     the server
     * @return the response entity
     * @throws IOException
     * @throws InvalidRequestException the invalid request exception
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
     * Gets the argo log.
     *
     * @param pipelineMetadata the pipeline metadata
     * @return the argo log
     * @throws IOException
     */
    @PostMapping(path = "v1.0/argo/application/log")
    @ApiOperation("to get argo application sync log")
    @TrackExecutionTime
    public ResponseEntity<String> getArgoLog(@RequestBody OpseraPipelineMetadata pipelineMetadata) throws IOException {
        Long startTime = System.currentTimeMillis();
        try {
            LOGGER.info("Received getArgoLog for pipelineMetadata : {}", pipelineMetadata);
            String response = serviceFactory.getArgoOrchestrator().getArgoLog(pipelineMetadata);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } finally {
            LOGGER.info("Completed getArgoLog, time taken to execute {} secs", System.currentTimeMillis() - startTime);
        }
    }
}
