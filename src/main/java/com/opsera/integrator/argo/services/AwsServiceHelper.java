package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.STS_SESSION_TOKEN;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.opsera.integrator.argo.config.AppConfig;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.InternalServiceException;
import com.opsera.integrator.argo.resources.AwsDetails;

/**
 * Helper class to facilitate all communications with vault
 */
@Service
public class AwsServiceHelper {

    public static final Logger LOGGER = LoggerFactory.getLogger(AwsServiceHelper.class);

    @Autowired
    private IServiceFactory serviceFactory;

    @Autowired
    private AppConfig appConfig;

    /**
     * gets the secret from vault
     *
     * @return
     */
    public AwsDetails getCredentials(AwsDetails awsDetails) {
        LOGGER.info("Enter the getting credentials secret for customer {}", awsDetails.getCustomerId());
        RestTemplate restTemplate = serviceFactory.getRestTemplate();
        String sessionTokenUrl = appConfig.getAwsServiceBaseUrl() + STS_SESSION_TOKEN;
        AwsDetails response = restTemplate.postForObject(sessionTokenUrl, awsDetails, AwsDetails.class);
        Optional<AwsDetails> responseData = Optional.ofNullable(response);
        if (responseData.isPresent()) {
            LOGGER.info("Completed the getting  credentials secret for customer {}", awsDetails.getCustomerId());
            return responseData.get();
        } else {
            throw new InternalServiceException(String.format("credentials secret Not found in Vault for CustomerID: %s", awsDetails.getCustomerId()));
        }
    }

}
