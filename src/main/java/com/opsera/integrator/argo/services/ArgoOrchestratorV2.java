package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ABORT;
import static com.opsera.integrator.argo.resources.Constants.AGRO_VERSION_NOT_SUPPORTED;
import static com.opsera.integrator.argo.resources.Constants.APPROVED;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_CONSOLE_FAILED;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_FAILED;
import static com.opsera.integrator.argo.resources.Constants.COMPLETED;
import static com.opsera.integrator.argo.resources.Constants.ERROR;
import static com.opsera.integrator.argo.resources.Constants.FAILED;
import static com.opsera.integrator.argo.resources.Constants.INVALID_SPEC_PROVIDED_IN_YAML;
import static com.opsera.integrator.argo.resources.Constants.OUT_OF_SYNC;
import static com.opsera.integrator.argo.resources.Constants.OUT_OF_SYNC_AND_STATUS_SUCCEEDED;
import static com.opsera.integrator.argo.resources.Constants.PROMOTE_FULL;
import static com.opsera.integrator.argo.resources.Constants.REJECTED;
import static com.opsera.integrator.argo.resources.Constants.RUNNING;
import static com.opsera.integrator.argo.resources.Constants.SUCCEEDED;
import static com.opsera.integrator.argo.resources.Constants.SUCCESS;
import static com.opsera.integrator.argo.resources.Constants.SYNC_IN_PROGRESS;
import static com.opsera.integrator.argo.resources.Constants.SYNC_TAKING_LONG_TIME;
import static com.opsera.integrator.argo.resources.Constants.UNKNOWN_STATE_RECEIVED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import com.opsera.integrator.argo.resources.Actions;
import com.opsera.integrator.argo.resources.ApprovalGateRequest;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoOperationState;
import com.opsera.integrator.argo.resources.ArgoSyncOperation;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.DataTransformerModel;
import com.opsera.integrator.argo.resources.Info;
import com.opsera.integrator.argo.resources.KafkaTopics;
import com.opsera.integrator.argo.resources.Node;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ResourceTree;
import com.opsera.integrator.argo.resources.RolloutActions;
import com.opsera.integrator.argo.resources.ToolConfig;

@Component
public class ArgoOrchestratorV2 {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoOrchestratorV2.class);

    @Autowired
    private IServiceFactory serviceFactory;

    @Autowired
    private TaskExecutor taskExecutor;

    public void syncApplication(OpseraPipelineMetadata pipelineMetadata) {
        try {
            LOGGER.debug("Starting to Sync Argo Application for the request {}", pipelineMetadata);
            pipelineMetadata.setRunCount(getRunCount(pipelineMetadata));
            ToolConfig argoToolConfig = serviceFactory.getConfigCollector().getArgoDetails(pipelineMetadata);
            ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolConfig.getToolConfigId(), pipelineMetadata.getCustomerId());
            String argoPassword = getArgoToolPassword(argoToolDetails);
            ArgoApplicationItem applicationItem = serviceFactory.getArgoHelper().syncApplication(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword);
            pipelineMetadata.setStatus(RUNNING);
            pipelineMetadata.setMessage(SYNC_IN_PROGRESS);
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_REPONSE, serviceFactory.gson().toJson(pipelineMetadata));
            Thread.sleep(10000);
            checkOperationStatus(pipelineMetadata, applicationItem, argoToolDetails, argoToolConfig, argoPassword, 0);
        } catch (Exception ex) {
            LOGGER.error("Exception Occurred while processing sync request: {}, exception: {}", pipelineMetadata, ex);
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, ex.getMessage()));
            Thread.currentThread().interrupt();
        }
    }

    private Object checkOperationStatus(OpseraPipelineMetadata pipelineMetadata, ArgoApplicationItem applicationItem, ArgoToolDetails argoToolDetails, ToolConfig argoToolConfig, String argoPassword,
            long retryCount) throws InterruptedException {
        ArgoApplicationItem applicationItemOperation = serviceFactory.getArgoHelper().syncApplicationOperation(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword);
        ArgoSyncOperation operationSync = applicationItemOperation.getStatus().getSync();
        ArgoOperationState operationState = applicationItemOperation.getStatus().getOperationState();
        if (null != operationState.getPhase() && operationState.getPhase().equalsIgnoreCase(RUNNING)) {
            if (20 > retryCount) {
                Thread.sleep(30000);
                retryCount = retryCount + 1;
                return checkOperationStatus(pipelineMetadata, applicationItem, argoToolDetails, argoToolConfig, argoPassword, retryCount);
            }
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, SYNC_TAKING_LONG_TIME), taskExecutor);
        } else if (null != operationState.getPhase() && operationState.getPhase().equalsIgnoreCase(SUCCEEDED)) {
            String message = operationState.getMessage();
            if (operationSync.getStatus().equalsIgnoreCase(OUT_OF_SYNC) && !argoToolConfig.isBlueGreenDeployment()) {
                if (10 > retryCount) {
                    Thread.sleep(30000);
                    retryCount = retryCount + 1;
                    return checkOperationStatus(pipelineMetadata, applicationItem, argoToolDetails, argoToolConfig, argoPassword, retryCount);
                }
                message = OUT_OF_SYNC_AND_STATUS_SUCCEEDED;
            }
            pipelineMetadata.setStatus(SUCCESS);
            pipelineMetadata.setMessage(operationState.getMessage());
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_REPONSE, serviceFactory.gson().toJson(pipelineMetadata));
            LOGGER.debug("Completed sending success response to kafka {}", pipelineMetadata);
            pipelineMetadata.setMessage(message);
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_STATUS, serviceFactory.gson().toJson(pipelineMetadata));
            LOGGER.debug("Completed sending success to kafka {}", pipelineMetadata);
            Thread.sleep(60000);
            publishResponseToDataTransformer(pipelineMetadata);
            CompletableFuture.runAsync(() -> streamConsoleLogAsync(pipelineMetadata, applicationItem, argoToolDetails, argoToolConfig, argoPassword), taskExecutor);
        } else if (null != operationState.getPhase() && operationState.getPhase().equalsIgnoreCase(ERROR) || operationState.getPhase().equalsIgnoreCase(FAILED)) {
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, !StringUtils.isEmpty(operationState.getMessage()) ? operationState.getMessage() : operationSync.getStatus()),
                    taskExecutor);
        } else {
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, !StringUtils.isEmpty(operationState.getMessage()) ? operationState.getMessage() : UNKNOWN_STATE_RECEIVED),
                    taskExecutor);
        }
        return "status check completed";
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
        publishResponseToDataTransformer(opseraPipelineMetadata);
        LOGGER.debug("Completed to send Error log to kafka {}", opseraPipelineMetadata);
        return null;
    }

    private void streamConsoleLogAsync(OpseraPipelineMetadata pipelineMetadata, ArgoApplicationItem applicationItem, ArgoToolDetails argoToolDetails, ToolConfig argoToolConfig, String argoPassword) {
        try {
            ResourceTree resourceTree = serviceFactory.getArgoHelper().getResourceTree(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword);
            List<String> podNames = getRunningPodList(resourceTree.getNodes(), pipelineMetadata);
            Set<String> uniquePodNames = new HashSet<>(podNames);
            if (!CollectionUtils.isEmpty(uniquePodNames)) {
                for (String podName : uniquePodNames) {
                    String logs = serviceFactory.getArgoHelper().getArgoApplicationLog(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword, podName,
                            pipelineMetadata.getNamespace());
                    if (!StringUtils.isEmpty(logs)) {
                        pipelineMetadata.setMessage(String.format("Retrieved application sync logs for the pod %s successfully", podName));
                        pipelineMetadata.setPodName(podName);
                        pipelineMetadata.setConsoleLog(logs);
                        pipelineMetadata.setStatus(RUNNING);
                        serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_CONSOLE_LOG, serviceFactory.gson().toJson(pipelineMetadata));
                        Thread.sleep(5000);
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
            String imageTagLatest = null;
            Optional<Node> firstNode = nodes.stream().filter(node -> node.getKind().equalsIgnoreCase("pod")).findFirst();
            if (firstNode.isPresent()) {
                imageTagLatest = firstNode.get().getImages().get(0);
                final String tempLatestTag = imageTagLatest;
                List<Node> filteredNode = nodes.stream().filter(node -> node.getKind().equalsIgnoreCase("pod") && node.getImages().contains(tempLatestTag)).collect(Collectors.toList());
                pipelineMetadata.setNamespace(filteredNode.get(0).getNamespace());
                filteredNode.forEach(fNode -> {
                    List<Info> infos = fNode.getInfo();
                    infos.forEach(info -> {
                        podNames.add(fNode.getName());
                    });
                });
            }
        } catch (Exception e) {
            LOGGER.warn("Exception while extracting pod name from resource tree nodes. request: {}", pipelineMetadata);
        }
        return podNames;
    }

    public void promoteOrAbortRolloutDeployment(ApprovalGateRequest approvalGateRequest) {
        LOGGER.debug("Starting to promote/abort Argo application depoyment based on approval gate response {}", approvalGateRequest);
        Integer runCount = serviceFactory.getConfigCollector().getRunCount(approvalGateRequest.getPipelineId(), approvalGateRequest.getCustomerId());
        OpseraPipelineMetadata pipelineMetadata = new OpseraPipelineMetadata();
        pipelineMetadata.setCustomerId(approvalGateRequest.getCustomerId());
        pipelineMetadata.setPipelineId(approvalGateRequest.getPipelineId());
        pipelineMetadata.setStepId(approvalGateRequest.getDeployStepId());
        pipelineMetadata.setRunCount(runCount);
        try {
            ToolConfig argoToolConfig = serviceFactory.getConfigCollector().getArgoDetails(pipelineMetadata);
            ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolConfig.getToolConfigId(), pipelineMetadata.getCustomerId());
            String argoPassword;
            argoPassword = getArgoToolPassword(argoToolDetails);
            ResourceTree resourceTree = serviceFactory.getArgoHelper().getResourceTree(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword);
            List<Node> rolloutNodeList = resourceTree.getNodes().stream().filter(node -> node.getKind().equalsIgnoreCase("Rollout")).collect(Collectors.toList());
            processNodeDetailsForApprovalGateResponseTopic(approvalGateRequest, pipelineMetadata, argoToolDetails.getConfiguration(), argoPassword, rolloutNodeList,
                    argoToolConfig.getApplicationName());
        } catch (Exception e) {
            LOGGER.error("argo blue green approval flow error. message: {}", Arrays.toString(e.getStackTrace()));
            pipelineMetadata.setStepId(approvalGateRequest.getDeployStepId());
            pipelineMetadata.setError(Arrays.toString(e.getStackTrace()));
            pipelineMetadata.setStatus(SUCCESS);
            pipelineMetadata.setMessage("Failed to retrieve promotion status for B/G deployment. Please check application status in argo tool for more details.");
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_STATUS, serviceFactory.gson().toJson(pipelineMetadata));
        }
    }

    private void processNodeDetailsForApprovalGateResponseTopic(ApprovalGateRequest approvalGateRequest, OpseraPipelineMetadata pipelineMetadata, ToolConfig argoToolConfig, String argoPassword,
            List<Node> rolloutNodeList, String applicationName) {
        pipelineMetadata.setStepId(approvalGateRequest.getApprovalGateStepId());
        if (!CollectionUtils.isEmpty(rolloutNodeList)) {
            Node node = rolloutNodeList.get(0);
            RolloutActions rolloutActions = serviceFactory.getArgoHelper().getArgoApplicationResourceActions(applicationName, node, argoToolConfig, argoPassword, null);
            boolean validRequest = false;
            List<Actions> actions = rolloutActions.getActions();
            for (Actions action : actions) {
                if ((action.getName().equalsIgnoreCase(PROMOTE_FULL) && approvalGateRequest.getStatus().equalsIgnoreCase(APPROVED))
                        || (action.getName().equalsIgnoreCase(ABORT) && approvalGateRequest.getStatus().equalsIgnoreCase(REJECTED))) {
                    validRequest = processValidRolloutRequest(pipelineMetadata, argoToolConfig, argoPassword, node, action, applicationName);
                }
            }
            if (!validRequest) {
                argoApprovalGateInvalidYaml(pipelineMetadata, AGRO_VERSION_NOT_SUPPORTED);
            }
        } else {
            argoApprovalGateInvalidYaml(pipelineMetadata, INVALID_SPEC_PROVIDED_IN_YAML);
        }
    }

    private boolean processValidRolloutRequest(OpseraPipelineMetadata pipelineMetadata, ToolConfig argoToolConfig, String argoPassword, Node node, Actions action, String applicationName) {
        boolean validRequest;
        if (action.isDisabled()) {
            validRequest = true;
            pipelineMetadata.setStatus(SUCCESS);
            pipelineMetadata.setMessage(String.format("Argo blue green deployment was successful. No new revision found in the application %s to promote/abort.", applicationName));
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_STATUS, serviceFactory.gson().toJson(pipelineMetadata));
        } else {
            validRequest = true;
            serviceFactory.getArgoHelper().getArgoApplicationResourceActions(applicationName, node, argoToolConfig, argoPassword, action.getName());
            pipelineMetadata.setStatus(SUCCESS);
            if (action.getName().equalsIgnoreCase(PROMOTE_FULL)) {
                pipelineMetadata.setMessage(String.format("The new deployment revision for %s promoted successfully", applicationName));
            } else if (action.getName().equalsIgnoreCase(ABORT)) {
                pipelineMetadata.setMessage(String.format("The new deployment revision for %s Aborted", applicationName));
            }
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_STATUS, serviceFactory.gson().toJson(pipelineMetadata));
        }
        return validRequest;
    }

    private void argoApprovalGateInvalidYaml(OpseraPipelineMetadata pipelineMetadata, String message) {
        pipelineMetadata.setStatus(SUCCESS);
        pipelineMetadata.setMessage(message);
        serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_STATUS, serviceFactory.gson().toJson(pipelineMetadata));
    }

    private Integer getRunCount(OpseraPipelineMetadata pipelineMetadata) {
        return serviceFactory.getConfigCollector().getRunCount(pipelineMetadata.getPipelineId(), pipelineMetadata.getCustomerId());
    }

    private String getArgoToolPassword(ArgoToolDetails argoToolDetails) {
        String argoPassword;
        if (argoToolDetails.getConfiguration().isSecretAccessTokenEnabled() && !StringUtils.isEmpty(argoToolDetails.getConfiguration().getSecretAccessTokenKey())) {
            argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getSecretAccessTokenKey().getVaultKey());
        } else {
            argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
        }
        return argoPassword;
    }

    private void publishResponseToDataTransformer(OpseraPipelineMetadata pipelineMetadata) {
        LOGGER.info("Started publishing sync details to Data transformer. pipelineId: {}, stepId: {}, runCount: {}", pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(),
                pipelineMetadata.getRunCount());
        try {
            pipelineMetadata.setRunCount(getRunCount(pipelineMetadata));
            ToolConfig argoToolConfig = serviceFactory.getConfigCollector().getArgoDetails(pipelineMetadata);
            ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolConfig.getToolConfigId(), pipelineMetadata.getCustomerId());
            String argoPassword = getArgoToolPassword(argoToolDetails);
            DataTransformerModel dtModel = new DataTransformerModel();
            dtModel.setPipelineMetadata(pipelineMetadata);
            dtModel.setToolMetadata(argoToolDetails);
            dtModel.setApplicationResponse(serviceFactory.getArgoHelper().getArgoApplication(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword));
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_SUMMARY_LOG, serviceFactory.gson().toJson(dtModel));
            LOGGER.info("Successfully published sync details to Data transformer. pipelineId: {}, stepId: {}, runCount: {}", pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(),
                    pipelineMetadata.getRunCount());
        } catch (Exception e) {
            LOGGER.error("Exception occured while publishing sync and application details to data transformer. stack: {}", Arrays.toString(e.getStackTrace()));
        }
    }
}
