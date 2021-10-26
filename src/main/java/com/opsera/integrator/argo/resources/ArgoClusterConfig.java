package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class ArgoClusterConfig {

    private String username;
    
    private String password;
    
    private String bearerToken;
    
    private AWSAuthConfig awsAuthConfig;
    
    private TLSClientConfig tlsClientConfig;
}
