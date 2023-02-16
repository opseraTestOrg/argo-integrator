package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class ArgoClusterItem {

    private String server;

    private String name;
    
    private ConnectionState connectionState;
    
    private String serverVersion;
    
}
