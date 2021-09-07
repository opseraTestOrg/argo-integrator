package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class CreateProjectRequest {

    private String toolId;

    private String customerId;

    private Project project;

    private boolean upsert;
}
