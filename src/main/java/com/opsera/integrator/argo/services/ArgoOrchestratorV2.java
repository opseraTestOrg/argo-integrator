package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ABORT;
import static com.opsera.integrator.argo.resources.Constants.ABORT_APPROVAL_RESPONSE;
import static com.opsera.integrator.argo.resources.Constants.AGRO_VERSION_NOT_SUPPORTED;
import static com.opsera.integrator.argo.resources.Constants.APPROVED;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_CONSOLE_FAILED;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_FAILED;
import static com.opsera.integrator.argo.resources.Constants.COMPLETED;
import static com.opsera.integrator.argo.resources.Constants.ERROR;
import static com.opsera.integrator.argo.resources.Constants.FAILED;
import static com.opsera.integrator.argo.resources.Constants.INVALID_SPEC_PROVIDED_IN_YAML;
import static com.opsera.integrator.argo.resources.Constants.NO_NEW_REVISION_FOR_APPROVAL;
import static com.opsera.integrator.argo.resources.Constants.OUT_OF_SYNC;
import static com.opsera.integrator.argo.resources.Constants.OUT_OF_SYNC_AND_STATUS_SUCCEEDED;
import static com.opsera.integrator.argo.resources.Constants.PROMOTE_APPROVAL_REPONSE;
import static com.opsera.integrator.argo.resources.Constants.PROMOTE_FULL;
import static com.opsera.integrator.argo.resources.Constants.REJECTED;
import static com.opsera.integrator.argo.resources.Constants.RUNNING;
import static com.opsera.integrator.argo.resources.Constants.SUCCEEDED;
import static com.opsera.integrator.argo.resources.Constants.SUCCESS;
import static com.opsera.integrator.argo.resources.Constants.SYNC_IN_PROGRESS;
import static com.opsera.integrator.argo.resources.Constants.SYNC_TAKING_LONG_TIME;
import static com.opsera.integrator.argo.resources.Constants.UNKNOWN_STATE_RECEIVED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.opsera.core.helper.ToolConfigurationHelper;
import com.opsera.core.helper.VaultHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.Actions;
import com.opsera.integrator.argo.resources.ApprovalGateRequest;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationSource;
import com.opsera.integrator.argo.resources.ArgoOperationState;
import com.opsera.integrator.argo.resources.ArgoSyncOperation;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.DataTransformerModel;
import com.opsera.integrator.argo.resources.Info;
import com.opsera.integrator.argo.resources.KafkaTopics;
import com.opsera.integrator.argo.resources.Kustomize;
import com.opsera.integrator.argo.resources.Node;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ResourceTree;
import com.opsera.integrator.argo.resources.RolloutActions;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.core.helper.VaultHelper;

@Component
public class ArgoOrchestratorV2 {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoOrchestratorV2.class);

    @Autowired
    private IServiceFactory serviceFactory;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private VaultHelper vaultService;

    @Autowired
    private ToolConfigurationHelper toolConfigurationHelper;

    public void syncApplication(OpseraPipelineMetadata pipelineMetadata) {
        try {
            LOGGER.debug("Starting to Sync Argo Application for the request {}", pipelineMetadata);
            pipelineMetadata.setRunCount(getRunCount(pipelineMetadata));
            ToolConfig argoToolConfig = toolConfigurationHelper.getPipelineStepConfig(pipelineMetadata.getCustomerId(), pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(), ToolConfig.class);
            ArgoToolDetails argoToolDetails = toolConfigurationHelper.getToolConfig(pipelineMetadata.getCustomerId(), argoToolConfig.getToolConfigId(), ArgoToolDetails.class);
            String argoPassword = getArgoToolPassword(argoToolDetails);
            if (argoToolConfig.isKustomizeFlag() && StringUtils.hasText(argoToolConfig.getImageUrl())) {
                setKustomizeDetails(pipelineMetadata, argoToolConfig, argoToolDetails, argoPassword);
            }
            if (argoToolConfig.isDynamicVariables() && (StringUtils.hasText(argoToolConfig.getApplicationCluster()) || StringUtils.hasText(argoToolConfig.getYamlPath()))) {
                ArgoApplicationItem appItem = serviceFactory.getArgoOrchestrator().getApplication(argoToolConfig.getToolConfigId(), argoToolDetails.getOwner(), argoToolConfig.getApplicationName());
                if (StringUtils.hasText(argoToolConfig.getApplicationCluster())) {
                    appItem.getSpec().getDestination().setServer(argoToolConfig.getApplicationCluster());
                }
                if (StringUtils.hasText(argoToolConfig.getYamlPath())) {
                    appItem.getSpec().getSource().setPath(argoToolConfig.getYamlPath());
                }
                if (!CollectionUtils.isEmpty(appItem.getStatus().getHistory())) {
                    appItem.getStatus().getHistory().clear();
                }
                serviceFactory.getArgoHelper().updateApplication(appItem, argoToolDetails.getConfiguration(), argoPassword, argoToolConfig.getApplicationName());
            }
            ArgoApplicationItem applicationItem = serviceFactory.getArgoHelper().syncApplication(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword);
            pipelineMetadata.setStatus(RUNNING);
            pipelineMetadata.setMessage(SYNC_IN_PROGRESS);
            serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_REPONSE.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
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
            serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_REPONSE.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
            LOGGER.debug("Completed sending success response to kafka {}", pipelineMetadata);
            pipelineMetadata.setMessage(message);
            serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_STATUS.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
            LOGGER.debug("Completed sending success to kafka {}", pipelineMetadata);
            Thread.sleep(60000);
            publishResponseToDataTransformer(pipelineMetadata);
            CompletableFuture.runAsync(() -> streamConsoleLogAsync(pipelineMetadata, applicationItem, argoToolDetails, argoToolConfig, argoPassword), taskExecutor);
        } else if (null != operationState.getPhase() && operationState.getPhase().equalsIgnoreCase(ERROR) || operationState.getPhase().equalsIgnoreCase(FAILED)) {
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, StringUtils.hasText(operationState.getMessage()) ? operationState.getMessage() : operationSync.getStatus()),
                    taskExecutor);
        } else {
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(pipelineMetadata, StringUtils.hasText(operationState.getMessage()) ? operationState.getMessage() : UNKNOWN_STATE_RECEIVED),
                    taskExecutor);
        }
        return "status check completed";
    }

    public Object sendErrorResponseToKafka(OpseraPipelineMetadata opseraPipelineMetadata, String message) {
        LOGGER.debug("Starting send Error response to kafka {}", message);
        opseraPipelineMetadata.setError(message);
        opseraPipelineMetadata.setStatus(FAILED);
        opseraPipelineMetadata.setMessage(ARGO_SYNC_FAILED);
        serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_REPONSE.getTopicName(), serviceFactory.gson().toJson(opseraPipelineMetadata));
        LOGGER.debug("Completed to send Error response to kafka {}", opseraPipelineMetadata);
        serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_STATUS.getTopicName(), serviceFactory.gson().toJson(opseraPipelineMetadata));
        LOGGER.debug("Completed to send Error status to kafka {}", opseraPipelineMetadata);
        opseraPipelineMetadata.setStatus(COMPLETED);
        opseraPipelineMetadata.setConsoleLog(ARGO_SYNC_CONSOLE_FAILED);
        opseraPipelineMetadata.setMessage("");
        serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_CONSOLE_LOG.getTopicName(), serviceFactory.gson().toJson(opseraPipelineMetadata));
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
                    if (StringUtils.hasText(logs)) {
                        pipelineMetadata.setMessage(String.format("Retrieved application sync logs for the pod %s successfully", podName));
                        pipelineMetadata.setPodName(podName);
                        pipelineMetadata.setConsoleLog(logs);
                        pipelineMetadata.setStatus(RUNNING);
                        serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_CONSOLE_LOG.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
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
        serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_CONSOLE_LOG.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
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
        Integer runCount = toolConfigurationHelper.getRunCount(approvalGateRequest.getPipelineId(), approvalGateRequest.getCustomerId());
        OpseraPipelineMetadata pipelineMetadata = new OpseraPipelineMetadata();
        pipelineMetadata.setCustomerId(approvalGateRequest.getCustomerId());
        pipelineMetadata.setPipelineId(approvalGateRequest.getPipelineId());
        pipelineMetadata.setStepId(approvalGateRequest.getDeployStepId());
        pipelineMetadata.setRunCount(runCount);
        try {
            ToolConfig argoToolConfig =toolConfigurationHelper.getPipelineStepConfig(pipelineMetadata.getCustomerId(), pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(), ToolConfig.class);
            ArgoToolDetails argoToolDetails = toolConfigurationHelper.getToolConfig(pipelineMetadata.getCustomerId(), argoToolConfig.getToolConfigId(), ArgoToolDetails.class);
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
            serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_STATUS.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
        }
    }

    private void processNodeDetailsForApprovalGateResponseTopic(ApprovalGateRequest approvalGateRequest, OpseraPipelineMetadata pipelineMetadata, ToolConfig argoToolConfig, String argoPassword,
            List<Node> rolloutNodeList, String applicationName) throws IOException {
        pipelineMetadata.setStepId(approvalGateRequest.getApprovalGateStepId());
        if (!CollectionUtils.isEmpty(rolloutNodeList)) {
            handleRolloutKindNodesforApproval(approvalGateRequest, pipelineMetadata, argoToolConfig, argoPassword, rolloutNodeList, applicationName);
        } else {
            approvalGateResponse(pipelineMetadata, INVALID_SPEC_PROVIDED_IN_YAML);
        }
    }

    private void handleRolloutKindNodesforApproval(ApprovalGateRequest approvalGateRequest, OpseraPipelineMetadata pipelineMetadata, ToolConfig argoToolConfig, String argoPassword,
            List<Node> rolloutNodeList, String applicationName) throws IOException {
        Node node = rolloutNodeList.get(0);
        String latestRevision = node.getInfo().get(0).getValue();
        RolloutActions rolloutActions = serviceFactory.getArgoHelper().getArgoApplicationResourceActions(applicationName, node, argoToolConfig, argoPassword, null);
        List<Actions> actions = rolloutActions.getActions();
        boolean promoteFlagAvailable = false;
        for (Actions action : actions) {
            if (action.getName().equalsIgnoreCase(PROMOTE_FULL)) {
                promoteFlagAvailable = true;
            }
            if ((action.getName().equalsIgnoreCase(PROMOTE_FULL) && approvalGateRequest.getStatus().equalsIgnoreCase(APPROVED))
                    || (action.getName().equalsIgnoreCase(ABORT) && approvalGateRequest.getStatus().equalsIgnoreCase(REJECTED))) {
                LOGGER.info("Api capabilities are available to promote or abort in ArgoCD");
                handlePromotionInUpgardedVersions(pipelineMetadata, argoToolConfig, argoPassword, node, action, applicationName, latestRevision);
            }
        }
        if (!promoteFlagAvailable) {
            LOGGER.info("Api capabilities are not available to promote or abort in this older ArgoCD. started custom handling for application {} with revision {}", applicationName, latestRevision);
            approvalGateResponse(pipelineMetadata, AGRO_VERSION_NOT_SUPPORTED);
        }
    }

    private boolean handlePromotionInUpgardedVersions(OpseraPipelineMetadata pipelineMetadata, ToolConfig argoToolConfig, String argoPassword, Node node, Actions action, String applicationName,
            String latestRevision) {
        boolean validRequest;
        if (action.isDisabled()) {
            validRequest = true;
            pipelineMetadata.setStatus(SUCCESS);
            pipelineMetadata.setMessage(String.format(NO_NEW_REVISION_FOR_APPROVAL, applicationName));
            serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_STATUS.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
        } else {
            validRequest = true;
            serviceFactory.getArgoHelper().getArgoApplicationResourceActions(applicationName, node, argoToolConfig, argoPassword, action.getName());
            pipelineMetadata.setStatus(SUCCESS);
            if (action.getName().equalsIgnoreCase(PROMOTE_FULL)) {
                pipelineMetadata.setMessage(String.format(PROMOTE_APPROVAL_REPONSE, latestRevision, applicationName));
            } else if (action.getName().equalsIgnoreCase(ABORT)) {
                pipelineMetadata.setMessage(String.format(ABORT_APPROVAL_RESPONSE, latestRevision, applicationName));
            }
            serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_STATUS.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
        }
        LOGGER.info("Argo promote/abort request successfully processed for the application {} with revision {}", latestRevision, applicationName);
        return validRequest;
    }

    private void approvalGateResponse(OpseraPipelineMetadata pipelineMetadata, String message) {
        pipelineMetadata.setStatus(SUCCESS);
        pipelineMetadata.setMessage(message);
        serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_STATUS.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
    }

    private Integer getRunCount(OpseraPipelineMetadata pipelineMetadata) {
        return toolConfigurationHelper.getRunCount(pipelineMetadata.getPipelineId(), pipelineMetadata.getCustomerId());
    }

    private String getArgoToolPassword(ArgoToolDetails argoToolDetails) {
        String argoPassword;
        if (argoToolDetails.getConfiguration().isSecretAccessTokenEnabled() && !ObjectUtils.isEmpty(argoToolDetails.getConfiguration().getSecretAccessTokenKey())) {
            argoPassword = vaultService.getSecrets(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getSecretAccessTokenKey().getVaultKey(),argoToolDetails.getVault());
        } else {
            argoPassword = vaultService.getSecrets(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey(),argoToolDetails.getVault());
        }
        return argoPassword;
    }

    private void publishResponseToDataTransformer(OpseraPipelineMetadata pipelineMetadata) {
        LOGGER.info("Started publishing sync details to Data transformer. pipelineId: {}, stepId: {}, runCount: {}", pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(),
                pipelineMetadata.getRunCount());
        try {
            pipelineMetadata.setRunCount(getRunCount(pipelineMetadata));
            ToolConfig argoToolConfig = toolConfigurationHelper.getPipelineStepConfig(pipelineMetadata.getCustomerId(), pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(), ToolConfig.class);
            ArgoToolDetails argoToolDetails = toolConfigurationHelper.getToolConfig(pipelineMetadata.getCustomerId(), argoToolConfig.getToolConfigId(), ArgoToolDetails.class);
            String argoPassword = getArgoToolPassword(argoToolDetails);
            DataTransformerModel dtModel = new DataTransformerModel();
            dtModel.setPipelineMetadata(pipelineMetadata);
            dtModel.setToolMetadata(argoToolDetails);
            dtModel.setApplicationResponse(serviceFactory.getArgoHelper().getArgoApplication(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword));
            serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_SUMMARY_LOG.getTopicName(), serviceFactory.gson().toJson(dtModel));
            LOGGER.info("Successfully published sync details to Data transformer. pipelineId: {}, stepId: {}, runCount: {}", pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(),
                    pipelineMetadata.getRunCount());
        } catch (Exception e) {
            LOGGER.error("Exception occured while publishing sync and application details to data transformer. stack: {}", Arrays.toString(e.getStackTrace()));
        }
    }

    private void setKustomizeDetails(OpseraPipelineMetadata pipelineMetadata, ToolConfig argoToolConfig, ArgoToolDetails argoToolDetails, String argoPassword) throws IOException {
        ArgoApplicationItem argoApplicationItem = serviceFactory.getArgoOrchestrator().getApplication(argoToolConfig.getToolConfigId(), pipelineMetadata.getCustomerId(),
                argoToolConfig.getApplicationName());
        ArgoApplicationSource source = serviceFactory.getArgoOrchestrator().getAppDetails(argoToolConfig.getToolConfigId(), pipelineMetadata.getCustomerId(), argoApplicationItem.getSpec());
        if ("Kustomize".equalsIgnoreCase(source.getType())) {
            List<String> images = new ArrayList<>();
            Kustomize kustomize = null;
            if (null != argoApplicationItem.getSpec().getSource().getKustomize()) {
                argoApplicationItem.getSpec().getSource().getKustomize().getImages()
                        .forEach(image -> images.add(String.format("%s=%s", image.contains("=") ? image.substring(0, image.indexOf("=")) : image, argoToolConfig.getImageUrl())));
                kustomize = argoApplicationItem.getSpec().getSource().getKustomize();
                argoApplicationItem.getSpec().getSource().getKustomize().getImages().clear();
            } else {
                source.getKustomize().getImages()
                        .forEach(image -> images.add(String.format("%s=%s", image.contains("=") ? image.substring(0, image.indexOf("=")) : image, argoToolConfig.getImageUrl())));
                kustomize = new Kustomize();
            }
            kustomize.setImages(images);
            argoApplicationItem.getSpec().getSource().setKustomize(kustomize);
            if (!CollectionUtils.isEmpty(argoApplicationItem.getStatus().getHistory())) {
                argoApplicationItem.getStatus().getHistory().clear();
            }
            serviceFactory.getArgoHelper().updateApplication(argoApplicationItem, argoToolDetails.getConfiguration(), argoPassword, argoToolConfig.getApplicationName());
        }

    }
}
