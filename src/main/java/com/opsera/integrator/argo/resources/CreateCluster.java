package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class CreateCluster {

    private String platformToolId;

    private String customerId;

    private String argoToolId;

    private String clusterName;

    private String platform;

    private String resourceGroup;
    
}
