package com.opsera.integrator.argo.resources;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class ArgoAccount implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -1750036828273638749L;

    private String name;

    private String token;
}
