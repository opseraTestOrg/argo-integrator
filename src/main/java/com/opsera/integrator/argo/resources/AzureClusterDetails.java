package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class AzureClusterDetails {
    
    private String server;
    
    private String name;

    private String caData;
    
    private String certData;
    
    private String bearerToken;
}
