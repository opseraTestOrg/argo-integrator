package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
public class ArgoApplicationMetadataList {

    List<ArgoApplicationMetadata> applicationList;

}
