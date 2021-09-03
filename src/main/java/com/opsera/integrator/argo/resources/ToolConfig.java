package com.opsera.integrator.argo.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ToolConfig {

    private String toolURL;

    private String toolConfigId;

    private String userName;

    private String owner;

    private String applicationName;

    private String accountUsername;

    private AccountPassword accountPassword;

    private boolean twoFactorAuthentication;

    private VaultSecretDetails secretPrivateKey;

}
