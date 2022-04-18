package com.opsera.integrator.argo.resources;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ToolConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String toolURL;

    private String toolConfigId;

    private String userName;

    private String owner;

    private String applicationName;

    private String accountUsername;

    private AccountPassword accountPassword;

    private boolean twoFactorAuthentication;

    private VaultSecretDetails secretPrivateKey;

    private boolean secretAccessTokenEnabled;

    private VaultSecretDetails secretAccessTokenKey;

    private VaultSecretDetails accessKey;

    private VaultSecretDetails secretKey;

    private VaultSecretDetails awsAccountId;

    private String regions;

    private VaultSecretDetails subscriptionId;

    private VaultSecretDetails tenantId;

    private VaultSecretDetails applicationId;

    private VaultSecretDetails applicationPassword;

    private VaultSecretDetails token;

    private VaultSecretDetails clientId;

    private VaultSecretDetails clientSecret;

    private String azureTenantId;

    private String azureSubscriptionId;

    private VaultSecretDetails gcpConfigFile;

}
