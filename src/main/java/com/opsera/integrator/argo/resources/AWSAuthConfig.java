package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class AWSAuthConfig {

    private String clusterName;
    
    private String roleARN;
}
