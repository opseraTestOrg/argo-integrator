package com.opsera.integrator.argo.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class SyncResult {

    private List<ArgoApplicationResource> resources;

    private String revision;

    private ArgoApplicationSource source;

}
