package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class CreateRepositoryRequest {

    private String toolId;

    private String customerId;

    private String repositoryName;

    private String repositoryType;

    private String sshUrl;

    private String httpsUrl;

    private String gitToolId;
    
    private String projectName;
    
    private String repoUrl;

}
