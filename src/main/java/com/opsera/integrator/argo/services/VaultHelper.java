package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.VAULT_READ;
import static com.opsera.integrator.argo.resources.Constants.VAULT_READ_ENDPOINT;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.opsera.integrator.argo.config.AppConfig;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.InternalServiceException;
import com.opsera.integrator.argo.resources.VaultData;
import com.opsera.integrator.argo.resources.VaultRequest;

/**
 * Class to handle interactions with vault server.
 */
@Component
public class VaultHelper {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(VaultHelper.class);

    /** The service factory. */
    @Autowired
    private IServiceFactory serviceFactory;

    /** The app config. */
    @Autowired
    private AppConfig appConfig;

    /**
     * This method used to get the argo credentials from vault.
     *
     * @param customerId the customer id
     * @param vaultKey   the vault key
     * @return the argo password
     */
    public String getArgoPassword(String customerId, String vaultKey) {
        RestTemplate restTemplate = serviceFactory.getRestTemplate();
        String readURL = appConfig.getVaultBaseUrl() + VAULT_READ_ENDPOINT;
        VaultRequest request = VaultRequest.builder().customerId(customerId).componentKeys(Collections.singletonList(vaultKey)).build();

        VaultData response = restTemplate.postForObject(readURL, request, VaultData.class);

        Optional<VaultData> vaultData = Optional.ofNullable(response);
        if (vaultData.isPresent()) {
            return new String(Base64.getDecoder().decode(response.getData().get(vaultKey).getBytes()));
        } else {
            LOGGER.info("Empty response from vault for request: {}", request);
            throw new InternalServiceException(String.format("SonarAuthToken Not found in Vault for CustomerID: %s", customerId));
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
        RestTemplate restTemplate = serviceFactory.getRestTemplate();
        String readURL = appConfig.getVaultBaseUrl() + VAULT_READ;
        VaultRequest request = VaultRequest.builder().customerId(customerId).vaultId(vaultId).componentKeys(Collections.singletonList(secretKey)).build();
        LOGGER.info("Request to Vault: {}", request);
        VaultData response = restTemplate.postForObject(readURL, request, VaultData.class);
        Optional<VaultData> vaultData = Optional.ofNullable(response);
        if (vaultData.isPresent()) {
            return new String(Base64.getDecoder().decode(response.getData().get(secretKey).getBytes()));
        } else {
            throw new InternalServiceException(String.format("GitCredential Not found in Vault for CustomerID: %s", customerId));
        }
    }

    public Map<String, String> getSecrets(String customerId, List<String> secretKeys, String vaultId) {
        RestTemplate restTemplate = serviceFactory.getRestTemplate();
        String readURL = appConfig.getVaultBaseUrl() + VAULT_READ;
        VaultRequest request = VaultRequest.builder().customerId(customerId).vaultId(vaultId).componentKeys(secretKeys).build();
        LOGGER.info("Request to Vault: {}", request);
        VaultData response = restTemplate.postForObject(readURL, request, VaultData.class);
        Optional<VaultData> vaultData = Optional.ofNullable(response);
        if (vaultData.isPresent()) {
            Map<String, String> vaultDataMap = new LinkedHashMap<>();
            vaultData.get().getData().entrySet().forEach(data -> vaultDataMap.put(data.getKey(), new String(Base64.getDecoder().decode(data.getValue().getBytes()))));
            return vaultDataMap;
        } else {
            throw new InternalServiceException(String.format("Secrets not found in Vault for CustomerID: %s", customerId));
        }
    }

}
