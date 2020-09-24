package com.opsera.integrator.argo.controller;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationMetadataList;
import com.opsera.integrator.argo.resources.ArgoApplicationOperation;
import com.opsera.integrator.argo.resources.ArgoClusterList;
import com.opsera.integrator.argo.resources.CreateApplicationRequest;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller
 */
@RestController
@Api("Opsera API for the integration with Argo CD")
public class ArgoController {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoController.class);

    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * To check the service status
     * 
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("To check the service status")
    public String status() {
        return "Spinnaker integrator service running";
    }

    /**
     * To get all the argo applications for the given argo domain
     * 
     * @param argoToolId
     * @return
     */
    @GetMapping(path = "v1.0/argo/applications")
    @ApiOperation("To get all the argo applications for the given argo domain")
    public ArgoApplicationMetadataList getAllArgoApplications(@RequestParam String argoToolId,
                                                              @RequestParam String customerId) {
        Long startTime = System.currentTimeMillis();
        try {
            LOGGER.info("Received getAllArgoApplications for argoId: {}", argoToolId);
            return serviceFactory.getArgoOrchestrator().getAllApplications(argoToolId, customerId);
        } finally {
            LOGGER.info("Completed getAllArgoApplications, time taken to execute {} secs", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * To get detailed information about an argo application
     * 
     * @param argoToolId
     * @return
     */
    @GetMapping(path = "v1.0/argo/application")
    @ApiOperation("To get detailed information about an argo application")
    public ArgoApplicationItem getArgoApplication(@RequestParam String argoToolId,
                                                  @RequestParam String customerId,
                                                  @RequestParam String applicationName) {
        Long startTime = System.currentTimeMillis();
        try {
            LOGGER.info("Received getArgoApplication for argoId: {}, argoApplication: {}", argoToolId, applicationName);
            return serviceFactory.getArgoOrchestrator().getApplication(argoToolId, customerId, applicationName);
        } finally {
            LOGGER.info("Completed getArgoApplication, time taken to execute {} secs", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * To sync the argo application configured in Opsera pipeline
     * 
     * @param pipelineMetadata
     * @return
     */
    @PostMapping(path = "v1.0/argo/application/sync")
    @ApiOperation("To sync the argo application configured in Opsera pipeline")
    public ArgoApplicationOperation syncArgoApplication(@RequestBody OpseraPipelineMetadata pipelineMetadata) {
        Long startTime = System.currentTimeMillis();
        try {
            LOGGER.info("Received syncArgoApplication for pipelineMetadata : {}", pipelineMetadata);
            return serviceFactory.getArgoOrchestrator().syncApplication(pipelineMetadata);
        } finally {
            LOGGER.info("Completed syncArgoApplication, time taken to execute {} secs", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * To get all the argo clusters for the given argo domain
     *
     * @param argoToolId
     * @return
     */
    @GetMapping(path = "v1.0/argo/clusters")
    @ApiOperation("To get all the argo applications for the given argo domain")
    public ArgoClusterList getAllArgoClusters(@RequestParam String argoToolId,
                                              @RequestParam String customerId) {
        Long startTime = System.currentTimeMillis();
        try {
            LOGGER.info("Received getAllArgoClusters for argoId: {}", argoToolId);
            return serviceFactory.getArgoOrchestrator().getAllClusters(argoToolId, customerId);
        } finally {
            LOGGER.info("Completed getAllArgoClusters, time taken to execute {} secs", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * To get all the argo projects for the given argo domain
     *
     * @param argoToolId
     * @return
     */
    @GetMapping(path = "v1.0/argo/projects")
    @ApiOperation("To get all the argo projects for the given argo domain")
    public ArgoApplicationMetadataList getAllArgoProjects(@RequestParam String argoToolId,
                                                          @RequestParam String customerId) {
        Long startTime = System.currentTimeMillis();
        try {
            LOGGER.info("Received getAllArgoProjects for argoId: {}", argoToolId);
            return serviceFactory.getArgoOrchestrator().getAllProjects(argoToolId, customerId);
        } finally {
            LOGGER.info("Completed getAllArgoProjects, time taken to execute {} secs", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * To create argo application
     *
     * @param request
     * @return
     */
    @PostMapping(path = "v1.0/argo/application/create")
    @ApiOperation("To create an argo application")
    public ResponseEntity<String> createArgoApplication(@RequestBody CreateApplicationRequest request) {
        Long startTime = System.currentTimeMillis();
        try {
            LOGGER.info("Received createArgoApplication for : {}", request);
            return serviceFactory.getArgoOrchestrator().createApplication(request);
        } finally {
            LOGGER.info("Completed createArgoApplication, time taken to execute {} secs", System.currentTimeMillis() - startTime);
        }
    }

}
