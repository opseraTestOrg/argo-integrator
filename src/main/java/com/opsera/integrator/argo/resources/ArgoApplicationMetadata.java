package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoApplicationMetadata {

    private String name;

    private String namespace;

    private String creationTimestamp;

    private String uid;
    
    private String resourceVersion;
    
    private String repoUrl;

    private String path;

    private String branch;

    private String project;

    private String clusterName;
    
    private String clusterUrl;

    private String syncStatus;

    private String healthStatus;

    private String phase;

    private boolean autoSync;

}
