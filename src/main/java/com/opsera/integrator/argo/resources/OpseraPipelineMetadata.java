package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class OpseraPipelineMetadata {

    private String pipelineId;

    private String jobId;

    private String customerId;

    private String stepId;

    private String action;

}
