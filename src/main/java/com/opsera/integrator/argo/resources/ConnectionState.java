package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionState {

    private String status;
    private String message;
    private String attemptedAt;

}
