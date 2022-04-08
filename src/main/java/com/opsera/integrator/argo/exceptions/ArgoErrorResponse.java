package com.opsera.integrator.argo.exceptions;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

/**
 * Error Response
 */
@Setter
@Getter
@JsonInclude(value = Include.NON_NULL)
public class ArgoErrorResponse implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5983466883002203161L;
    
    private List<ArgoErrorResponse> errors;

    private String status;

    private String message;

    private String detail;

    private String title;
    
    private String error;

    public ArgoErrorResponse() {
        // do nothing
    }

    public ArgoErrorResponse(String status, String message) {
    }

}
