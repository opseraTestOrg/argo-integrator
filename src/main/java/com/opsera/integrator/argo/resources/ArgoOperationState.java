package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoOperationState {

    private ArgoApplicationOperation operation;

    private String phase;

    private String message;

    private String startedAt;

    private String finishedAt;

}
