package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class ArgoApplicationSource {

    private String repoURL;

    private String path;

}
