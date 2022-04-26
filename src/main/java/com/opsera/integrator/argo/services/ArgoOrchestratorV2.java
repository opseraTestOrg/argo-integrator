package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_CONSOLE_FAILED;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_FAILED;
import static com.opsera.integrator.argo.resources.Constants.COMPLETED;
import static com.opsera.integrator.argo.resources.Constants.FAILED;
import static com.opsera.integrator.argo.resources.Constants.RUNNING;
import static com.opsera.integrator.argo.resources.Constants.SUCCESS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import com.opsera.integrator.argo.exceptions.ArgoServiceException;
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
            Integer runCount = serviceFactory.getConfigCollector().getRunCount(pipelineMetadata.getPipelineId(), pipelineMetadata.getCustomerId());
            pipelineMetadata.setRunCount(runCount);
            ToolConfig argoToolConfig = serviceFactory.getConfigCollector().getArgoDetails(pipelineMetadata);
            ArgoToolDetails argoToolDetails = serviceFactory.getConfigCollector().getArgoDetails(argoToolConfig.getToolConfigId(), pipelineMetadata.getCustomerId());
            String argoPassword;
            if (argoToolDetails.getConfiguration().isSecretAccessTokenEnabled() && !StringUtils.isEmpty(argoToolDetails.getConfiguration().getSecretAccessTokenKey())) {
                argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getSecretAccessTokenKey().getVaultKey());
            } else {
                argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
            }
            ArgoApplicationItem applicationItem = syncApp(argoToolConfig, argoToolDetails, argoPassword);
            pipelineMetadata.setStatus(RUNNING);
            pipelineMetadata.setMessage("Sync in Progress");
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_REPONSE, serviceFactory.gson().toJson(pipelineMetadata));
            ArgoApplicationItem applicationItemOperation = serviceFactory.getArgoHelper().syncApplicationOperation(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(),
                    argoPassword);
            if (null != applicationItemOperation && null != applicationItemOperation.getStatus()) {
                checkOperationStatus(pipelineMetadata, applicationItemOperation, applicationItem, argoToolDetails, argoToolConfig, argoPassword, 0);
            } else {
                throw new ArgoServiceException("Unknown state received");
            }
        } catch (Exception ex) {
            LOGGER.error("Exception Occurred while processing sync request: {}, exception: {}", pipelineMetadata, ex);
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, ex.getMessage()));
        }
    }

    private Object checkOperationStatus(OpseraPipelineMetadata pipelineMetadata, ArgoApplicationItem applicationItemOperation, ArgoApplicationItem applicationItem, ArgoToolDetails argoToolDetails,
            ToolConfig argoToolConfig, String argoPassword, long retryCount) throws InterruptedException {
        ArgoSyncOperation operationSync = applicationItemOperation.getStatus().getSync();
        ArgoOperationState operationState = applicationItemOperation.getStatus().getOperationState();
        if (operationState.getPhase().equalsIgnoreCase("Running")) {
            Thread.sleep(5000);
            applicationItemOperation = serviceFactory.getArgoHelper().syncApplicationOperation(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword);
            return checkOperationStatus(pipelineMetadata, applicationItemOperation, applicationItem, argoToolDetails, argoToolConfig, argoPassword, retryCount);
        } else if (operationState.getPhase().equalsIgnoreCase("Succeeded")) {
            String message = operationState.getMessage();
            if (operationSync.getStatus().equalsIgnoreCase("OutOfSync") && !argoToolConfig.isBlueGreenDeployment()) {
                if (10 > retryCount) {
                    Thread.sleep(30000);
                    retryCount = retryCount + 1;
                    applicationItemOperation = serviceFactory.getArgoHelper().syncApplicationOperation(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword);
                    return checkOperationStatus(pipelineMetadata, applicationItemOperation, applicationItem, argoToolDetails, argoToolConfig, argoPassword, retryCount);
                }
                message = "successfully synced (all tasks run) but the current status in argo tool is OutOfSync for more than 5 mins and it might take sometime than usual to reflect in the tool";
            }
            pipelineMetadata.setStatus(SUCCESS);
            pipelineMetadata.setMessage(operationState.getMessage());
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_REPONSE, serviceFactory.gson().toJson(pipelineMetadata));
            LOGGER.debug("Completed sending success response to kafka {}", pipelineMetadata);
            pipelineMetadata.setMessage(message);
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_STATUS, serviceFactory.gson().toJson(pipelineMetadata));
            LOGGER.debug("Completed sending success to kafka {}", pipelineMetadata);
            Thread.sleep(60000);
            CompletableFuture.runAsync(() -> streamConsoleLogAsync(pipelineMetadata, applicationItem, argoToolDetails, argoToolConfig, argoPassword), taskExecutor);
        } else if (operationState.getPhase().equalsIgnoreCase("Error") || operationState.getPhase().equalsIgnoreCase("Failed")) {
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, !StringUtils.isEmpty(operationState.getMessage()) ? operationState.getMessage() : operationSync.getStatus()),
                    taskExecutor);
        } else {
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, !StringUtils.isEmpty(operationState.getMessage()) ? operationState.getMessage() : "Unknown state received"),
                    taskExecutor);
        }
        CompletableFuture.runAsync(() -> publishResponseToDataTransformer(pipelineMetadata, argoToolDetails, argoToolConfig, argoPassword), taskExecutor);
        return "Check Status Completed";
    }

    private ArgoApplicationItem syncApp(ToolConfig argoToolConfig, ArgoToolDetails argoToolDetails, String argoPassword) {
        return serviceFactory.getArgoHelper().syncApplication(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword);
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

    private void streamConsoleLogAsync(OpseraPipelineMetadata pipelineMetadata, ArgoApplicationItem applicationItem, ArgoToolDetails argoToolDetails,
            ToolConfig argoToolConfig, String argoPassword) {
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
            List<Node> filteredNode = nodes.stream().filter(node -> node.getKind().equalsIgnoreCase("pod")).collect(Collectors.toList());
            pipelineMetadata.setNamespace(filteredNode.get(0).getNamespace());
            filteredNode.forEach(fNode -> {
                List<Info> infos = fNode.getInfo();
                infos.forEach(info -> {
                    podNames.add(fNode.getName());
                });
            });
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
            if (argoToolDetails.getConfiguration().isSecretAccessTokenEnabled() && !StringUtils.isEmpty(argoToolDetails.getConfiguration().getSecretAccessTokenKey())) {
                argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getSecretAccessTokenKey().getVaultKey());
            } else {
                argoPassword = serviceFactory.getVaultHelper().getArgoPassword(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey());
            }
            ResourceTree resourceTree = serviceFactory.getArgoHelper().getResourceTree(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword);
            List<Node> rolloutNodeList = resourceTree.getNodes().stream().filter(node -> node.getKind().equalsIgnoreCase("Rollout")).collect(Collectors.toList());
            processNodeDetailsForApprovalGateResponseTopic(approvalGateRequest, pipelineMetadata, argoToolDetails.getConfiguration(), argoPassword, rolloutNodeList, argoToolConfig.getApplicationName());
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
                if ((action.getName().equalsIgnoreCase("promote-full") && approvalGateRequest.getStatus().equalsIgnoreCase("Approved"))
                        || (action.getName().equalsIgnoreCase("abort") && approvalGateRequest.getStatus().equalsIgnoreCase("Rejected"))) {
                    validRequest = processValidRolloutRequest(pipelineMetadata, argoToolConfig, argoPassword, node, action, applicationName);
                }
            }
            if (!validRequest) {
                argoApprovalGateInvalidYaml(pipelineMetadata);
            }
        } else {
            argoApprovalGateInvalidYaml(pipelineMetadata);
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
            if (action.getName().equalsIgnoreCase("promote-full")) {
                pipelineMetadata.setMessage(String.format("The new deployment revision for %s promoted successfully", applicationName));
            } else if (action.getName().equalsIgnoreCase("abort")) {
                pipelineMetadata.setMessage(String.format("The new deployment revision for %s Aborted", applicationName));
            }
            serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_STATUS, serviceFactory.gson().toJson(pipelineMetadata));
        }
        return validRequest;
    }

    private void argoApprovalGateInvalidYaml(OpseraPipelineMetadata pipelineMetadata) {
        pipelineMetadata.setStatus(SUCCESS);
        pipelineMetadata.setMessage("Invalid spec provided in yaml for Argo blue green deployment");
        serviceFactory.getKafkaHelper().postNotificationToKafkaService(KafkaTopics.OPSERA_PIPELINE_STATUS, serviceFactory.gson().toJson(pipelineMetadata));
    }
    
    private void publishResponseToDataTransformer(OpseraPipelineMetadata pipelineMetadata, ArgoToolDetails argoToolDetails, ToolConfig argoToolConfig, String argoPassword) {
        LOGGER.info("Started publishing sync details to Data transformer. pipelineId: {}, stepId: {}, runCount: {}", pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(),
                pipelineMetadata.getRunCount());
        try {
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
