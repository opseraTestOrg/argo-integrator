/**
 * 
 */
package com.opsera.integrator.argo.resources;

import java.io.Serializable;

import lombok.Data;

/**
 * @author Purusothaman
 *
 */
@Data
public class VaultSecretDetails implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String name;
    private String vaultKey;

}
