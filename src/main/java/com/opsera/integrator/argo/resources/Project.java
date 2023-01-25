package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Project {
    
    private String name;
    
    private String description;

    private ArgoProjectMetadata metadata;

    private ArgoProjectSpec spec;
}
