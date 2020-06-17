package com.opsera.integrator.argo.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
public class ArgoApplicationMetadataList {

    private List<ArgoApplicationMetadata> applicationList;

}
