package com.opsera.integrator.argo.resources;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Response {

    private String message;
    private String status;

}
