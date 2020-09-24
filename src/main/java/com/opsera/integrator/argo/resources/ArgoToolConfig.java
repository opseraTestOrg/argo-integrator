package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ArgoToolConfig {

    private String toolURL;

    private String userName;

    private String applicationName;

    private AccountPassword accountPassword;

}
