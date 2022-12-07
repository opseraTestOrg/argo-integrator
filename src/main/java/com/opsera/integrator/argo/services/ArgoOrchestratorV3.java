package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ARGO_CLI_IMAGE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_CONSOLE_FAILED;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_FAILED;
import static com.opsera.integrator.argo.resources.Constants.COMPLETED;
import static com.opsera.integrator.argo.resources.Constants.CUSTOMER_CLUSTER_INFO_MISSING;
import static com.opsera.integrator.argo.resources.Constants.FAILED;
import static com.opsera.integrator.argo.resources.Constants.GIT_BRANCH;
import static com.opsera.integrator.argo.resources.Constants.GIT_FILE_PATH;
import static com.opsera.integrator.argo.resources.Constants.GIT_TOKEN;
import static com.opsera.integrator.argo.resources.Constants.GIT_URL;
import static com.opsera.integrator.argo.resources.Constants.HTTP;
import static com.opsera.integrator.argo.resources.Constants.HTTPS;
import static com.opsera.integrator.argo.resources.Constants.IMAGE_REFERENCE;
import static com.opsera.integrator.argo.resources.Constants.IMAGE_URL;
import static com.opsera.integrator.argo.resources.Constants.RUNNING;
import static com.opsera.integrator.argo.resources.Constants.VAULT_CLUSTER_TOKEN;
import static com.opsera.integrator.argo.resources.Constants.VAULT_CLUSTER_URL;
import static com.opsera.integrator.argo.resources.Constants.SYNC_IN_PROGRESS;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.opsera.core.helper.ToolConfigurationHelper;
import com.opsera.core.helper.VaultHelper;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.ResourcesNotAvailable;
import com.opsera.integrator.argo.listener.KubernetesPodConsoleLogListener;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.DataTransformerModel;
import com.opsera.integrator.argo.resources.KafkaTopics;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.kubernetes.helper.KubernetesPodHandler;
import com.opsera.kubernetes.helper.exception.KubernetesHelperException;

@Component
public class ArgoOrchestratorV3 {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoOrchestratorV3.class);

    @Autowired
    private IServiceFactory serviceFactory;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private VaultHelper vaultService;

    @Autowired
    private ToolConfigurationHelper toolConfigurationHelper;

    /**
     * To execute the azure related pipeline
     * 
     * @param request
     * @return
     */
    public void executePipeline(OpseraPipelineMetadata opseraPipelineMetadata, ToolConfig argoToolConfig) {
        LOGGER.info("Starting processing request : {} ", opseraPipelineMetadata);
        try {
            // Get k8 cluster details from customer vault
            Integer runCount = toolConfigurationHelper.getRunCount(opseraPipelineMetadata.getPipelineId(), opseraPipelineMetadata.getCustomerId());
            opseraPipelineMetadata.setRunCount(runCount);
            String parentId = toolConfigurationHelper.getParentId(opseraPipelineMetadata.getCustomerId());
            Map<String, String> vaultData = vaultService.getSecrets(parentId, Arrays.asList(VAULT_CLUSTER_URL, VAULT_CLUSTER_TOKEN), null);
            String url = vaultData.get(VAULT_CLUSTER_URL);
            String token = vaultData.get(VAULT_CLUSTER_TOKEN);
            if (!StringUtils.hasText(url) || !StringUtils.hasText(token))
                throw new ResourcesNotAvailable(CUSTOMER_CLUSTER_INFO_MISSING);
            LOGGER.info("Successfully fetched the customer cluster information");
            Map<String, String> secrets = new LinkedHashMap<>();
            Map<String, String> envVar = new LinkedHashMap<>();
            List<String> commands = new ArrayList<>();
            commands.add("/bin/sh");
            commands.add("-exc");
            StringBuilder command = new StringBuilder();
            getEnvironmentVariables(opseraPipelineMetadata, argoToolConfig, secrets, envVar);
            getCommands(opseraPipelineMetadata, argoToolConfig, secrets, command, envVar);
            commands.add(command.toString());
            LOGGER.info("Starting to create kubernetes pod on the customer data plane");
            KubernetesPodHandler handler = new KubernetesPodHandler(url, token, opseraPipelineMetadata.getPipelineId(), opseraPipelineMetadata.getStepId(), opseraPipelineMetadata.getRunCount());
            handler.createJob(ARGO_CLI_IMAGE, commands, envVar, secrets);
            CompletableFuture.runAsync(() -> streamConsoleLogAsync(opseraPipelineMetadata, argoToolConfig, handler), taskExecutor);
            LOGGER.info("Pod created and async call to stream console logs to Kafka topic has been Initiated");
            opseraPipelineMetadata.setStatus(RUNNING);
            opseraPipelineMetadata.setMessage(SYNC_IN_PROGRESS);
            serviceFactory.getKafkaHelper().postNotificationToKafka(KafkaTopics.OPSERA_PIPELINE_REPONSE.getTopicName(), serviceFactory.gson().toJson(opseraPipelineMetadata));
        } catch (Exception ex) {
            LOGGER.error("Error while triggering job, Exception : ", ex);
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(opseraPipelineMetadata, ex.getMessage()));
        }
    }

    /**
     * Streaming the azure cli console logs
     * 
     * @param request
     * @param handler
     * @param config
     */
    private void streamConsoleLogAsync(OpseraPipelineMetadata request, ToolConfig argoToolConfig, KubernetesPodHandler handler) {
        KubernetesPodConsoleLogListener listener = new KubernetesPodConsoleLogListener(request, argoToolConfig, serviceFactory);
        try {
            handler.streamJobLogs(listener, 20);
        } catch (KubernetesHelperException ex) {
            LOGGER.error("Error while streaming console Logs, Exception : ", ex);
            CompletableFuture.runAsync(() -> sendErrorResponseToKafka(request, ex.getMessage()));
        }
    }

    private void getCommands(OpseraPipelineMetadata request, ToolConfig argoToolConfig, Map<String, String> secrets, StringBuilder command, Map<String, String> envVar) throws IOException {
        LOGGER.info("Starting to construct the commands");
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("kustomize.sh");
        String kustomizeCommand = IOUtils.toString(inputStream, Charset.defaultCharset());
        command.append(kustomizeCommand).append(System.lineSeparator());

    }

    public void getEnvironmentVariables(OpseraPipelineMetadata opseraPipelineMetadata, ToolConfig argoToolConfig, Map<String, String> secrets, Map<String, String> envVar)
            throws UnsupportedEncodingException {
        LOGGER.info("starting to set commands, setting environment variables");
        String gitUrl = getGitUrl(argoToolConfig, opseraPipelineMetadata, secrets);
        secrets.put(GIT_URL, gitUrl);
        secrets.put(GIT_BRANCH, argoToolConfig.getDefaultBranch());
        String gitBaseFolder = getGitCheckoutFolder(gitUrl);
        envVar.put(GIT_FILE_PATH, String.format("%s/%s", gitBaseFolder, StringUtils.hasText(argoToolConfig.getGitFilePath()) ? argoToolConfig.getGitFilePath() : ""));
        envVar.put(IMAGE_REFERENCE, argoToolConfig.getImageReference());
        envVar.put(IMAGE_URL, argoToolConfig.getImageUrl());
        LOGGER.info("Successfully set commands, setting environment variables");

    }

    /**
     * To construct the git checkout url
     * 
     * @param stepConfiguration
     * @param opseraPipelineMetadata
     * @return
     * @throws UnsupportedEncodingException
     */
    public String getGitUrl(ToolConfig argoToolConfig, OpseraPipelineMetadata opseraPipelineMetadata, Map<String, String> secrets) throws UnsupportedEncodingException {
        ArgoToolDetails gitToolDetails = toolConfigurationHelper.getToolConfig(opseraPipelineMetadata.getCustomerId(), argoToolConfig.getGitToolId(), ArgoToolDetails.class);
        String credentialSecret = gitToolDetails.getConfiguration().isTwoFactorAuthentication() ? gitToolDetails.getConfiguration().getSecretAccessTokenKey().getVaultKey()
                : gitToolDetails.getConfiguration().getAccountPassword().getVaultKey();
        String secret = vaultService.getSecrets(gitToolDetails.getOwner(), credentialSecret, gitToolDetails.getVault());
        secrets.put(GIT_TOKEN, secret);
        String url = argoToolConfig.getGitUrl();
        if (url.contains("@"))
            url = HTTPS + url.substring(url.indexOf("@") + 1);
        if (url.contains(HTTPS))
            url = url.replace(HTTPS, HTTPS + URLEncoder.encode(gitToolDetails.getConfiguration().getAccountUsername(), StandardCharsets.UTF_8.toString()) + ":"
                    + URLEncoder.encode(secret, StandardCharsets.UTF_8.toString()) + "@");
        else
            url = url.replace(HTTP, HTTP + URLEncoder.encode(gitToolDetails.getConfiguration().getAccountUsername(), StandardCharsets.UTF_8.toString()) + ":"
                    + URLEncoder.encode(secret, StandardCharsets.UTF_8.toString()) + "@");
        return url;

    }

    public void syncApplication(OpseraPipelineMetadata pipelineMetadata, ToolConfig argoToolConfig) throws InterruptedException {
        ArgoToolDetails argoToolDetails = toolConfigurationHelper.getToolConfig(pipelineMetadata.getCustomerId(), argoToolConfig.getToolConfigId(), ArgoToolDetails.class);
        String argoPassword = getArgoToolPassword(argoToolDetails);
        ArgoApplicationItem applicationItem = serviceFactory.getArgoHelper().syncApplication(argoToolConfig.getApplicationName(), argoToolDetails.getConfiguration(), argoPassword);
        Thread.sleep(10000);
        serviceFactory.getArgoOrchestratorV2().checkOperationStatus(pipelineMetadata, applicationItem, argoToolDetails, argoToolConfig, argoPassword, 0);

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

    private String getArgoToolPassword(ArgoToolDetails argoToolDetails) {
        String argoPassword;
        if (argoToolDetails.getConfiguration().isSecretAccessTokenEnabled() && !ObjectUtils.isEmpty(argoToolDetails.getConfiguration().getSecretAccessTokenKey())) {
            argoPassword = vaultService.getSecrets(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getSecretAccessTokenKey().getVaultKey(), argoToolDetails.getVault());
        } else {
            argoPassword = vaultService.getSecrets(argoToolDetails.getOwner(), argoToolDetails.getConfiguration().getAccountPassword().getVaultKey(), argoToolDetails.getVault());
        }
        return argoPassword;
    }

    private void publishResponseToDataTransformer(OpseraPipelineMetadata pipelineMetadata) {
        LOGGER.info("Started publishing sync details to Data transformer. pipelineId: {}, stepId: {}, runCount: {}", pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(),
                pipelineMetadata.getRunCount());
        try {
            ToolConfig argoToolConfig = toolConfigurationHelper.getPipelineStepConfig(pipelineMetadata.getCustomerId(), pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(),
                    ToolConfig.class);
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

    /**
     * Gets the git checkout folder.
     * 
     * @param gitURL the git URL
     * @return the git checkout folder
     */

    public String getGitCheckoutFolder(String gitURL) {
        if (gitURL.contains(".git")) {
            return gitURL.substring(gitURL.lastIndexOf('/') + 1, gitURL.indexOf(".git"));
        } else {
            return gitURL.substring(gitURL.lastIndexOf('/') + 1);
        }

    }

}
