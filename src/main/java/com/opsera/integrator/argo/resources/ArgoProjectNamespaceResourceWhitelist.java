package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class ArgoProjectNamespaceResourceWhitelist {

    private String group;

    private String kind;
}
