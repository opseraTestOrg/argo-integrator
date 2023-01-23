package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class ErrorResponse {

    private String error;
    private String code;
    private String message;
}
