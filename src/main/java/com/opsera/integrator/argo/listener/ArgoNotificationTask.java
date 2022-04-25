package com.opsera.integrator.argo.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ApprovalGateRequest;

public class ArgoNotificationTask implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoNotificationTask.class);

    private String message;

    private IServiceFactory serviceFactory;

    public ArgoNotificationTask(String message, IServiceFactory serviceFactory) {
        this.message = message;
        this.serviceFactory = serviceFactory;
    }

    @Override
    public void run() {
        try {
            ApprovalGateRequest approvalGateRequest = serviceFactory.getObjectMapper().readValue(message, ApprovalGateRequest.class);
            long threadId = Thread.currentThread().getId();
            String threadName = Thread.currentThread().getName();
            LOGGER.info("Starting Invoke Argo Executor @ threadId: {} , threadName: {}", threadId, threadName);
            serviceFactory.getArgoOrchestratorV2().promoteOrAbortRolloutDeployment(approvalGateRequest);
        } catch (Exception ex) {
            LOGGER.error("Error while processing Kafka request for Argo Promot / abort", ex);
        } finally {
            LOGGER.info("Blue Green deployment pormote/abort completed successfully");
        }
    }
}
