package com.opsera.integrator.argo.exceptions;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * Error Response
 */
@Setter
@Getter
public class ArgoErrorResponse implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5983466883002203161L;

    private String status;

    private String message;


}
