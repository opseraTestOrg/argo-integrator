package com.opsera.integrator.argo.resources;

import java.util.List;

import lombok.Data;

@Data
public class CreateApplicationRequest {

    private String toolId;

    private String customerId;

    private String applicationName;

    private String projectName;

    private String cluster;

    private String branchName;

    private String namespace;

    private String gitUrl;

    private String gitPath;

    private boolean autoSync;
    
    private String type;
    
    private boolean recursive;
    
    private List<String> valueFiles;
    
    private String values;
    
    private String namePrefix;
    
    private String nameSuffix;

}
