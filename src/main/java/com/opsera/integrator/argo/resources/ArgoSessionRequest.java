package com.opsera.integrator.argo.resources;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArgoSessionRequest {

    private String username;

    private String password;

}
