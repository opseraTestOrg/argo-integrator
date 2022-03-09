package com.opsera.integrator.argo.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.Action;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;

public class ArgoDeploymentTask implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoDeploymentTask.class);

    private String message;

    private IServiceFactory serviceFactory;

    public ArgoDeploymentTask(String message, IServiceFactory serviceFactory) {
        this.message = message;
        this.serviceFactory = serviceFactory;
    }

    @Override
    public void run() {
        try {
            OpseraPipelineMetadata pipelineMetadata = serviceFactory.getObjectMapper().readValue(message, OpseraPipelineMetadata.class);
            long threadId = Thread.currentThread().getId();
            String threadName = Thread.currentThread().getName();
            LOGGER.info("Starting Invoke Argo Executor @ threadId: {} , threadName: {}", threadId, threadName);
            switch (Action.valueOf(pipelineMetadata.getAction().toUpperCase())) {
            case START:
                serviceFactory.getArgoOrchestratorV2().syncApplication(pipelineMetadata);
                break;
            case DELETE:
                break;
            default:
                break;
            }
        } catch (Exception ex) {
            LOGGER.error("Error while processing Kafka request for Argo application sync", ex);
        } finally {
            LOGGER.info("Application Sync request Submitted");
        }
    }
}
