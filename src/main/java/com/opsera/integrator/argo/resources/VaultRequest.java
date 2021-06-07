package com.opsera.integrator.argo.resources;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VaultRequest {

    private String customerId;

    private String vaultId;

    private List<String > componentKeys;

}
