package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoApplicationSpec {

    private String project;

    private ArgoApplicationSource source;

    private ArgoApplicationDestination destination;

}
