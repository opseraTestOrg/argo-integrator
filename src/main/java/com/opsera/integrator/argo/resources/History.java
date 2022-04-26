package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class History {
    
    private String id;
    private String revision;
    private String deployedAt;
    private String deployStartedAt;

}
