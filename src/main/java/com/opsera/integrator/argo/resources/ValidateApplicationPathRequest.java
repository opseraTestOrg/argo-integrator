package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateApplicationPathRequest {

    private String customerId;
    private String argoToolId;
    private String path;
    private String name;
    private String repoUrl;
    private String branch;
    private String tag;
}
