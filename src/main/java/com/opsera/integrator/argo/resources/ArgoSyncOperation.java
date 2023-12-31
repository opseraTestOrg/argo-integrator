package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoSyncOperation {

    private String revision;

    private String status;
    
    private ArgoApplicationSpec comparedTo;

}
