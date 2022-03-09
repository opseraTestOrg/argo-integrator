package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_CONSOLE_FAILED;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_FAILED;
import static com.opsera.integrator.argo.resources.Constants.COMPLETED;
import static com.opsera.integrator.argo.resources.Constants.FAILED;
import static com.opsera.integrator.argo.resources.Constants.RUNNING;
import static com.opsera.integrator.argo.resources.Constants.SUCCESS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.ArgoServiceException;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoOperationState;
import com.opsera.integrator.argo.resources.ArgoSyncOperation;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.Info;
import com.opsera.integrator.argo.resources.KafkaTopics;
import com.opsera.integrator.argo.resources.Node;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ResourceTree;
import com.opsera.integrator.argo.resources.ToolConfig;

@Component
public class ArgoOrchestratorV2 {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoOrchestratorV2.class);

    @Autowired
    private IServiceFactory serviceFactory;

    private TaskExecutor taskExecutor;

    public void syncApplication(OpseraPipelineMetadata pipelineMetadata) {
        try {
            LOGGER.debug("Starting to Sync Argo Application for the request {}", pipelineMetadata);
            Integer runCount = serviceFactory.getConfigCollector().getRunCount(pipelineMetadata.getPipelineId(), pipelineMetadata.getCustomerId());
            pipelineMetadata.setRunCount(runCount);
            ToolConfig argoToolConfig = serviceFactory.getConfigCollector().getArgoDetails(pipelineMetadata);
            ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolConfig.getToolConfigId(), pipelineMetadata.getCustomerId());
            String argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
            ArgoApplicationItem applicationItem = syncApp(argoToolConfig, argoToolDetails, argoPassword);
            pipelineMetadata.setStatus(RUNNING);
            pipelineMetadata.setMessage("Sync in Progress");
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_REPONSE, serviceFactory.gson().toJson(pipelineMetadata));
            ArgoApplicationItem applicationItemOperation = serviceFactory.getArgoHelper().syncApplicationOperation(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration().getToolURL(),
                    argoToolDetails.getConfiguration().getUserName(), argoPassword);
            if (null != applicationItemOperation && null != applicationItemOperation.getStatus()) {
                checkOperationStatus(pipelineMetadata, applicationItemOperation, applicationItem, argoToolDetails, argoToolConfig, argoPassword);
            } else {
                throw new ArgoServiceException("Unknown state received");
            }
        } catch (Exception ex) {
            LOGGER.error("Exception Occurred while processing sync request: {}, exception: {}", pipelineMetadata, ex);
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, ex.getMessage()));
        }
    }

    private Object checkOperationStatus(OpseraPipelineMetadata pipelineMetadata, ArgoApplicationItem applicationItemOperation, ArgoApplicationItem applicationItem, ArgoToolDetails argoToolDetails,
            ToolConfig argoToolConfig, String argoPassword) throws InterruptedException {
        ArgoSyncOperation operationSync = applicationItemOperation.getStatus().getSync();
        ArgoOperationState operationState = applicationItemOperation.getStatus().getOperationState();
        if (operationState.getPhase().equalsIgnoreCase("Running")) {
            Thread.sleep(5000);
            applicationItemOperation = serviceFactory.getArgoHelper().syncApplicationOperation(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration().getToolURL(),
                    argoToolDetails.getConfiguration().getUserName(), argoPassword);
            return checkOperationStatus(pipelineMetadata, applicationItemOperation, applicationItem, argoToolDetails, argoToolConfig, argoPassword);
        } else if (operationState.getPhase().equalsIgnoreCase("Succeeded")) {
            if (operationSync.getStatus().equalsIgnoreCase("Synced")) {
                pipelineMetadata.setStatus(SUCCESS);
                pipelineMetadata.setMessage(operationState.getMessage());
                serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_REPONSE, serviceFactory.gson().toJson(pipelineMetadata));
                LOGGER.debug("Completed sending success response to kafka {}", pipelineMetadata);
                serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_STATUS, serviceFactory.gson().toJson(pipelineMetadata));
                LOGGER.debug("Completed sending success to kafka {}", pipelineMetadata);
                streamConsoleLogAsync(pipelineMetadata, applicationItemOperation, applicationItem, argoToolDetails, argoToolConfig, argoPassword);
            } else {
                LOGGER.warn("Phase Succeeded but Status is OutOfSync");
            }
        } else if (operationState.getPhase().equalsIgnoreCase("Error")) {
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, !StringUtils.isEmpty(operationState.getMessage()) ? operationState.getMessage() : operationSync.getStatus()),
                    taskExecutor);
        } else {
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, "Unknown state received"));
        }
        return null;
    }

    private ArgoApplicationItem syncApp(ToolConfig argoToolConfig, ArgoToolDetails argoToolDetails, String argoPassword) {
        return serviceFactory.getArgoHelper().syncApplication(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration().getToolURL(), argoToolDetails.getConfiguration().getUserName(),
                argoPassword);
    }

    public Object sendErrorResponseToKafka(OpseraPipelineMetadata opseraPipelineMetadata, String message) {
        LOGGER.debug("Starting send Error response to kafka {}", message);
        opseraPipelineMetadata.setError(message);
        opseraPipelineMetadata.setStatus(FAILED);
        opseraPipelineMetadata.setMessage(ARGO_SYNC_FAILED);
        serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_REPONSE, serviceFactory.gson().toJson(opseraPipelineMetadata));
        LOGGER.debug("Completed to send Error response to kafka {}", opseraPipelineMetadata);
        serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_STATUS, serviceFactory.gson().toJson(opseraPipelineMetadata));
        LOGGER.debug("Completed to send Error status to kafka {}", opseraPipelineMetadata);
        opseraPipelineMetadata.setStatus(COMPLETED);
        opseraPipelineMetadata.setConsoleLog(ARGO_SYNC_CONSOLE_FAILED);
        opseraPipelineMetadata.setMessage("");
        serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_CONSOLE_LOG, serviceFactory.gson().toJson(opseraPipelineMetadata));
        LOGGER.debug("Completed to send Error log to kafka {}", opseraPipelineMetadata);
        return null;
    }

    private void streamConsoleLogAsync(OpseraPipelineMetadata pipelineMetadata, ArgoApplicationItem applicationItemOperation, ArgoApplicationItem applicationItem2, ArgoToolDetails argoToolDetails,
            ToolConfig argoToolConfig, String argoPassword) {
        try {
            ResourceTree resourceTree = serviceFactory.getArgoHelper().getResourceTree(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration().getToolURL(),
                    argoToolDetails.getConfiguration().getUserName(), argoPassword);
            List<String> podNames = getRunningPodList(resourceTree.getNodes(), pipelineMetadata);
            if (!CollectionUtils.isEmpty(podNames)) {
                for (String podName : podNames) {
                    String logs = serviceFactory.getArgoHelper().getArgoApplicationLog(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration().getToolURL(),
                            argoToolDetails.getConfiguration().getUserName(), argoPassword, podName, pipelineMetadata.getNamespace());
                    if (!StringUtils.isEmpty(logs)) {
                        pipelineMetadata.setMessage(String.format("Retrieved application sync logs for the pod %s successfully", podName));
                        pipelineMetadata.setPodName(podName);
                        pipelineMetadata.setConsoleLog(logs);
                        pipelineMetadata.setStatus(RUNNING);
                        serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_CONSOLE_LOG, serviceFactory.gson().toJson(pipelineMetadata));
                    }
                }
            } else {
                pipelineMetadata.setMessage("Unable to Retrieve pod logs for the sync");
            }
        } catch (Exception e) {
            LOGGER.warn("Exception occured while streaming application logs in Argo for the request {}", pipelineMetadata);
            pipelineMetadata.setMessage("Unable to Retrieve pod logs for the sync");
        }
        pipelineMetadata.setStatus(COMPLETED);
        pipelineMetadata.setConsoleLog("");
        serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_CONSOLE_LOG, serviceFactory.gson().toJson(pipelineMetadata));
    }

    private List<String> getRunningPodList(List<Node> nodes, OpseraPipelineMetadata pipelineMetadata) {
        List<String> podNames = new ArrayList<>();
        try {
            List<Node> filteredNode = nodes.stream().filter(node -> node.getKind().equalsIgnoreCase("pod")).collect(Collectors.toList());
            pipelineMetadata.setNamespace(filteredNode.get(0).getNamespace());
            filteredNode.forEach(fNode -> {
                List<Info> infos = fNode.getInfo();
                infos.forEach(info -> {
                    if (info.getValue().equalsIgnoreCase("Running")) {
                        podNames.add(fNode.getName());
                    }
                });
            });
        } catch (Exception e) {
            LOGGER.warn("Exception while extracting pod name from resource tree nodes. request: {}", pipelineMetadata);
        }
        return podNames;
    }
}
