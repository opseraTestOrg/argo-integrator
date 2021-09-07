package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class ArgoProjectMetadata {

    private String name;
    
    private String resourceVersion;
}
