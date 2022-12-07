package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class CreateApplicationRequest {

    private String toolId;

    private String customerId;

    private String applicationName;

    private String projectName;

    private String cluster;

    private String branchName;

    private String namespace;

    private String gitUrl;

    private String gitPath;

    private boolean autoSync;

}
