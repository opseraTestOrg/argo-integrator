package com.opsera.integrator.argo.listener;


import static com.opsera.integrator.argo.resources.Constants.OPSERA_PIPELINE_ARGO_REQUEST;
import static com.opsera.integrator.argo.resources.Constants.OPSERA_PIPELINE_ARGO_NOTIFICATION;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.opsera.integrator.argo.config.IServiceFactory;

@Component
public class ArgoActionListener {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoActionListener.class);

    @Autowired
    private IServiceFactory serviceFactory;

    @Autowired
    private TaskExecutor taskExecutor;

    @KafkaListener(topics = { OPSERA_PIPELINE_ARGO_REQUEST }, containerFactory = "kafkaListenerContainerFactory")
    public void consumeArgoDeploymentRequest(@Payload String message) {
        LOGGER.info("Message Received from Kafka topic OPSERA_PIPELINE_ARGO_REQUEST : {}", message);
        ArgoDeploymentTask runnable = new ArgoDeploymentTask(message, serviceFactory);
        taskExecutor.execute(runnable);
    }
    
    @KafkaListener(topics = { OPSERA_PIPELINE_ARGO_NOTIFICATION }, containerFactory = "kafkaListenerContainerFactory")
    public void consumeArgoNotificationResponse(@Payload String message) {
        LOGGER.info("Message Received from Kafka topic OPSERA_PIPELINE_ARGO_NOTIFICATION : {}", message);
        ArgoNotificationTask runnable = new ArgoNotificationTask(message, serviceFactory);
        taskExecutor.execute(runnable);
    }
}
