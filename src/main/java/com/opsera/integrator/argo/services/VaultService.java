package com.opsera.integrator.argo.services;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opsera.core.exception.ServiceException;
import com.opsera.core.helper.VaultHelper;

/**
 * Class to handle interactions with vault server.
 */
@Component
public class VaultService {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(VaultService.class);


    /** The app config. */
    @Autowired
    private VaultHelper vaultHelper;

    /**
     * This method used to get the argo credentials from vault.
     *
     * @param customerId the customer id
     * @param vaultKey   the vault key
     * @return the argo password
     */
    public String getArgoPassword(String customerId, String vaultKey) {
        Map<String, String> response = vaultHelper.getSecrets(customerId, Collections.singletonList(vaultKey));
        if (!response.isEmpty()) {
            return new String(Base64.getDecoder().decode(response.get(vaultKey).getBytes()));
        } else {
            throw new ServiceException(String.format("SonarAuthToken Not found in Vault for CustomerID: %s", customerId));
        }
    }

    /**
     * Gets the secret.
     *
     * @param customerId the customer id
     * @param secretKey  the secret key
     * @param vaultId    the vault id
     * @return the secret
     */
    public String getSecret(String customerId, String secretKey, String vaultId) {
        Map<String, String> response = vaultHelper.getSecrets(customerId, Collections.singletonList(secretKey), vaultId);
        if (!response.isEmpty()) {
            return new String(Base64.getDecoder().decode(response.get(secretKey).getBytes()));
        } else {
            throw new ServiceException(String.format("GitCredential Not found in Vault for CustomerID: %s", customerId));
        }
    }
}
