package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class CreateClusterRequest {

    private String toolId;

    private String customerId;
    
    private String server;

    private String name;
    
    private String serverVersion;
    
    private ArgoClusterConfig config;
}
