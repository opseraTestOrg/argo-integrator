package com.opsera.integrator.argo.listener;

import static com.opsera.core.enums.KafkaResponseTopics.OPSERA_PIPELINE_CONSOLE_LOG;
import static com.opsera.core.enums.KafkaResponseTopics.OPSERA_PIPELINE_STATUS;
import static com.opsera.integrator.argo.resources.Constants.COMPLETED;
import static com.opsera.integrator.argo.resources.Constants.FAILED;
import static com.opsera.integrator.argo.resources.Constants.REGIX_REPLACE_ANSI_COLOR;
import static com.opsera.integrator.argo.resources.Constants.RUNNING;
import static com.opsera.integrator.argo.resources.Constants.SUCCESS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.kubernetes.helper.listener.KubernetesLogListener;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KubernetesPodConsoleLogListener implements KubernetesLogListener {

    public static final Logger LOGGER = LoggerFactory.getLogger(KubernetesPodConsoleLogListener.class);

    private OpseraPipelineMetadata pipelineMetadata;
    private ToolConfig argoToolConfig;
    private IServiceFactory serviceFactory;

    @Override
    public void onLogData(String message) {
        try {
            OpseraPipelineMetadata consoleLogMetadata = new OpseraPipelineMetadata();
            consoleLogMetadata.setPipelineId(pipelineMetadata.getPipelineId());
            consoleLogMetadata.setStepId(pipelineMetadata.getStepId());
            consoleLogMetadata.setCustomerId(pipelineMetadata.getCustomerId());
            consoleLogMetadata.setConsoleLog(constructProperMessage(message));
            consoleLogMetadata.setRunCount(pipelineMetadata.getRunCount());
            consoleLogMetadata.setStatus(RUNNING);
            serviceFactory.getKafkaHelper().postNotificationToKafka(OPSERA_PIPELINE_CONSOLE_LOG.getTopicName(), serviceFactory.gson().toJson(consoleLogMetadata));
            LOGGER.info("consoleLogStream: Successfully published kafka message {}", consoleLogMetadata);
        } catch (Exception e) {
            LOGGER.error("consoleLogStream: Exception occured while sending console log streams to kafka for pipeline : {}, step : {}", pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(),
                    e);
        }
    }

    @Override
    public void processCompleted(String message, int exitCode, String completeLogs) {
        try {
            pipelineMetadata.setStatus(exitCode == 0 ? SUCCESS : FAILED);
            pipelineMetadata.setMessage(constructProperMessage(message));
            pipelineMetadata.setConsoleLog(constructProperMessage(message));
            if (SUCCESS.equalsIgnoreCase(pipelineMetadata.getStatus())) {
                serviceFactory.getArgoOrchestratorV3().syncApplication(pipelineMetadata, argoToolConfig);
            } else {
                serviceFactory.getKafkaHelper().postNotificationToKafka(OPSERA_PIPELINE_STATUS.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
                LOGGER.info("consoleLogStream: Successfully published kafka message {}", pipelineMetadata);
              pipelineMetadata.setStatus(COMPLETED);
                serviceFactory.getKafkaHelper().postNotificationToKafka(OPSERA_PIPELINE_CONSOLE_LOG.getTopicName(), serviceFactory.gson().toJson(pipelineMetadata));
            }
            LOGGER.info("processCompletion: Successfully published log topic kafka message {} ", pipelineMetadata);
        } catch (Exception e) {
            LOGGER.error("processCompletion: Exception occured while sending final status to kafka for pipeline : {}, step : {}", pipelineMetadata.getPipelineId(), pipelineMetadata.getStepId(), e);
        }
    }

    private String constructProperMessage(String message) {
        return message.replaceAll(REGIX_REPLACE_ANSI_COLOR, "");
    }

}
