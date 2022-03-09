package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.SUCCESS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.opsera.integrator.argo.exceptions.MessageProcessingException;
import com.opsera.integrator.argo.resources.KafkaTopics;

@Component
public class KafkaHelper {

    public static final Logger LOGGER = LoggerFactory.getLogger(KafkaHelper.class);

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Method to publish to Kafka topic via Kafka Integrator Service
     * 
     * @param request
     * @return
     */
    public String postNotificationToKafkaService(KafkaTopics topic, String message) {
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic.getTopicName(), message);

        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
                LOGGER.info("Sent message=[ {} ] to topic=[ {} ] with offset=[ {} ]", message, topic.name(), result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                LOGGER.error("Unable to send message=[] due to exception: {}", message, ex);
                throw new MessageProcessingException("Exception while publishing message via kafka");
            }
        });
        return SUCCESS;
    }

    public void stopListeners() {
        LOGGER.info("Stopping the Kafka Listeners");
        kafkaListenerEndpointRegistry.stop();
    }
}
