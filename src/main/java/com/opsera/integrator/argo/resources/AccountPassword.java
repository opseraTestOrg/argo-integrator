package com.opsera.integrator.argo.resources;

import java.io.Serializable;

import lombok.Data;

@Data
public class AccountPassword implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String name;
    private String vaultKey;
}
