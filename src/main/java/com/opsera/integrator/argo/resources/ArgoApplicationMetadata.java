package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoApplicationMetadata {

    private String name;

    private String namespace;

    private String creationTimestamp;

    private String uid;
    
    private String resourceVersion;

}
