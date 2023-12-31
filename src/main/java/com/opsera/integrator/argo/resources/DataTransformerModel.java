package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTransformerModel {

    OpseraPipelineMetadata pipelineMetadata;
    ArgoToolDetails toolMetadata;
    ArgoApplicationItem applicationResponse;

}
