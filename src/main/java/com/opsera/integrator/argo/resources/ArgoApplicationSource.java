package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArgoApplicationSource {

    private String repoURL;

    private String targetRevision;

    private String path;

    private String type;

    private Kustomize kustomize;

}
