package com.opsera.integrator.argo.resources;

import lombok.Getter;

@Getter
public enum KafkaTopics {

    OPSERA_PIPELINE_ARGO_REQUEST("opsera.pipeline.argo.request"), OPSERA_PIPELINE_REPONSE("opsera.pipeline.response"), OPSERA_PIPELINE_STATUS("opsera.pipeline.status"),
    OPSERA_PIPELINE_LOG("opsera.pipeline.log"), OPSERA_PIPELINE_CONSOLE_LOG("opsera.pipeline.console.log"), OPSERA_PIPELINE_ARGO_NOTIFICATION("opsera.pipeline.argo.notification");

    private String topicName;

    KafkaTopics(String topicName) {
        this.topicName = topicName;
    }

}
