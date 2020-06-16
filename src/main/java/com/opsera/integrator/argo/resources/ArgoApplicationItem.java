package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoApplicationItem {

    private ArgoApplicationMetadata metadata;

    private ArgoApplicationSpec spec;

    private ArgoApplicationStatus status;

    private ArgoApplicationOperation operation;

}
