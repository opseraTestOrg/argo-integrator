package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoRepositoryItem {

    private String repo;
    
    private String type;
    
    private String name;
    
    private String username;
    
    private String sshPrivateKey;
    
    private String password;
    
    private ArgoReositoryConnectionState connectionState;
    
    private String project;
    
}
