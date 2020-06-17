package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoApplicationStatus {

    private ArgoOperationState operationState;

    private List<ArgoApplicationResource> resources;

    private ArgoSyncOperation sync;

}
