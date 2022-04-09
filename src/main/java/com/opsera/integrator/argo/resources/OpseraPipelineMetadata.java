package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class OpseraPipelineMetadata {

    private String jobId;
    private String pipelineId;
    private String customerId;
    private String stepId;
    private String action;
    private Integer runCount;
    private String consoleLog;
    private String message;
    private String error;
    private String status;
    private String podName;
    private String namespace;

}
