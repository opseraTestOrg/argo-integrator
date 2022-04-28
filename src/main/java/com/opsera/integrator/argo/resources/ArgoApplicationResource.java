package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoApplicationResource {

    private String group;

    private String version;

    private String kind;

    private String namespace;

    private String name;

    private String status;

    private String message;

    private String hookPhase;

    private String syncPhase;
    
    private Health health;

}
