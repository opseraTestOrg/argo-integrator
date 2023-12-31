package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoReositoryConnectionState {

    private String status;
    
    private String message;
    
    private String attemptedAt;
    
}
