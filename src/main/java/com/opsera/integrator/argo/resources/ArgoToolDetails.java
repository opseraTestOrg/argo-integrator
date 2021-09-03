package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoToolDetails {

    private String id;

    private String name;

    private String description;

    private String owner;

    private String[] type;

    private String[] tags;

    private Boolean active;

    private String status;

    private String toolIdentifier;

    private ToolConfig configuration;

}
