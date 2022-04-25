package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class ApprovalGateRequest {

    private String pipelineId;
    private String deployStepId;
    private String approvalGateStepId;
    private String deployToolIdentifier;
    private String status;
    private String customerId;
    private String ownerId;

}
