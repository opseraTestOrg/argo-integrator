package com.opsera.integrator.argo.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoApplicationStatus {

    private ArgoOperationState operationState;

    private List<ArgoApplicationResource> resources;

    private ArgoSyncOperation sync;

    private Health health;
    
    private List<History> history;
    
    private ArgoApplicationSource source;
    
    private Summary summary;
    
    private String phase;

}
