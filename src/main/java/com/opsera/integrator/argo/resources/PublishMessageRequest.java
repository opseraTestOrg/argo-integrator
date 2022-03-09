package com.opsera.integrator.argo.resources;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(value = Include.NON_NULL)
public class PublishMessageRequest implements Serializable {

    private static final long serialVersionUID = 6477316466097974184L;

    private KafkaTopics kafkaTopicName;

    private String message;
}
