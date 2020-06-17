package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ALL_ARGO_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_URL;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SESSION_TOKEN_URL;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.HTTP_EMPTY_BODY;
import static com.opsera.integrator.argo.resources.Constants.HTTP_HEADER_ACCEPT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationsList;
import com.opsera.integrator.argo.resources.ArgoSessionRequest;
import com.opsera.integrator.argo.resources.ArgoSessionToken;

/**
 * Class handles all the interaction with argo server
 */
@Component
public class ArgoHelper {

    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * get argo application details
     * 
     * @param applicationName
     * @param username
     * @param password
     * @return
     */
    public ArgoApplicationItem getArgoApplication(String applicationName, String username, String password) {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(username, password);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, applicationName);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoApplicationItem(response.getBody());
    }

    /**
     * get all argo applications
     * 
     * @param username
     * @param password
     * @return
     */
    public ArgoApplicationsList getAllArgoApplications(String username, String password) {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(username, password);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(ARGO_APPLICATION_URL, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoApplicationsList(response.getBody());
    }

    /**
     * sync an argo application
     * 
     * @param applicationName
     * @param username
     * @param password
     * @return
     */
    public ArgoApplicationItem syncApplication(String applicationName, String username, String password) {
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, username, password);
        String url = String.format(ARGO_SYNC_APPLICATION_URL_TEMPLATE, applicationName);
        ResponseEntity<ArgoApplicationItem> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.POST, requestEntity, ArgoApplicationItem.class);
        return response.getBody();
    }

    /**
     * 
     * Returns argo session token
     * 
     * @param username
     * @param password
     * @return
     */
    private ArgoSessionToken getSessionToken(String username, String password) {
        ArgoSessionRequest request = new ArgoSessionRequest(username, password);
        return serviceFactory.getRestTemplate().postForObject(ARGO_SESSION_TOKEN_URL, request, ArgoSessionToken.class);
    }

    /**
     * 
     * Returns HTTP request headers
     * 
     * @param username
     * @param password
     * @return
     */
    private HttpEntity<HttpHeaders> getRequestEntity(String username, String password) {
        ArgoSessionToken sessionToken = getSessionToken(username, password);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.setBearerAuth(sessionToken.getToken());
        return new HttpEntity<>(requestHeaders);
    }

    /**
     * 
     * Returns header with body
     * 
     * @param requestBody
     * @param username
     * @param password
     * @return
     */
    private HttpEntity<String> getRequestEntityWithBody(String requestBody, String username, String password) {
        ArgoSessionToken sessionToken = getSessionToken(username, password);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.setBearerAuth(sessionToken.getToken());
        return new HttpEntity<>(requestBody, requestHeaders);
    }
}
