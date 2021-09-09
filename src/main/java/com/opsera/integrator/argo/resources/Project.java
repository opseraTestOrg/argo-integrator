package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class Project {

    private ArgoProjectMetadata metadata;

    private ArgoProjectSpec spec;
}
