package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class ArgoApplicationDestination {

    private String namespace;

    private String server;
    
    private String name;

}
