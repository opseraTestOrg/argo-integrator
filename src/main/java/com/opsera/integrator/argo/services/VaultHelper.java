package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.VAULT_READ_ENDPOINT;

import java.util.Base64;
import java.util.Collections;
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
 * Class to handle interactions with vault server
 */
@Component
public class VaultHelper {

    public static final Logger LOGGER = LoggerFactory.getLogger(VaultHelper.class);

    @Autowired
    private IServiceFactory serviceFactory;

    @Autowired
    private AppConfig appConfig;

    /**
     * This method used to get the argo credentials from vault
     *
     * @param customerId
     * @param vaultKey
     * @return
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
}
