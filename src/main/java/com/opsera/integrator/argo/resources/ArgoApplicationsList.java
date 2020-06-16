package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
public class ArgoApplicationsList {

    List<ArgoApplicationItem> items;

}
