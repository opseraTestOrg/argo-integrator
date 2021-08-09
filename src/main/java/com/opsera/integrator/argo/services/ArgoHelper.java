package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ALL_ARGO_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_ALL_CLUSTER_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_ALL_PROJECT_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_CREATE_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SESSION_TOKEN_URL;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.HTTP_EMPTY_BODY;
import static com.opsera.integrator.argo.resources.Constants.HTTP_HEADER_ACCEPT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.opsera.integrator.argo.resources.ArgoClusterList;
import com.opsera.integrator.argo.resources.ArgoSessionRequest;
import com.opsera.integrator.argo.resources.ArgoSessionToken;

/**
 * Class handles all the interaction with argo server
 */
@Component
public class ArgoHelper {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoHelper.class);

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
    public ArgoApplicationItem getArgoApplication(String applicationName, String baseUrl, String username, String password) {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, baseUrl, applicationName);
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
    public ArgoApplicationsList getAllArgoApplications(String baseUrl, String username, String password) {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ARGO_APPLICATION_URL_TEMPLATE, baseUrl);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoApplicationsList(response.getBody());
    }

    /**
     * get all argo clusters
     *
     * @param username
     * @param password
     * @return
     */
    public ArgoClusterList getAllArgoClusters(String baseUrl, String username, String password) {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ARGO_ALL_CLUSTER_URL_TEMPLATE, baseUrl);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoClustersList(response.getBody());
    }

    /**
     * get all argo projects
     *
     * @param username
     * @param password
     * @return
     */
    public ArgoApplicationsList getAllArgoProjects(String baseUrl, String username, String password) {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ARGO_ALL_PROJECT_URL_TEMPLATE, baseUrl);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
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
    public ArgoApplicationItem syncApplication(String applicationName, String baseUrl, String username, String password) {
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, baseUrl, username, password);
        String url = String.format(ARGO_SYNC_APPLICATION_URL_TEMPLATE, baseUrl, applicationName);
        ResponseEntity<ArgoApplicationItem> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.POST, requestEntity, ArgoApplicationItem.class);
        return response.getBody();
    }

    /**
     * creates an argo application
     *
     * @param argoApplication
     * @param username
     * @param password
     * @return
     */
    public ResponseEntity<String> createApplication(ArgoApplicationItem argoApplication, String baseUrl, String username, String password) {
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), baseUrl, username, password);
        String url = String.format(ARGO_CREATE_APPLICATION_URL_TEMPLATE, baseUrl);
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.POST, requestEntity, String.class);
    }

    /**
     * update an argo application
     *
     * @param argoApplication
     * @param username
     * @param password
     * @return
     */
    public ResponseEntity<String> updateApplication(ArgoApplicationItem argoApplication, String baseUrl, String username, String password, String applicationName) {
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), baseUrl, username, password);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, baseUrl, applicationName);
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.PUT, requestEntity, String.class);
    }

    /**
     * delete the application details
     * 
     * @param applicationName
     * @param
     * @param username
     * @param password
     * @return
     */
    public void deleteArgoApplication(String applicationName, String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to delete the application {} and url {} ", applicationName, baseUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, baseUrl, applicationName);
        serviceFactory.getRestTemplate().exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        LOGGER.debug("To Completed to delete the application {} and url {} ", applicationName, baseUrl);
    }

    /**
     * 
     * Returns argo session token
     * 
     * @param username
     * @param password
     * @return
     */
    private ArgoSessionToken getSessionToken(String baseUrl, String username, String password) {
        ArgoSessionRequest request = new ArgoSessionRequest(username, password);
        String url = String.format(ARGO_SESSION_TOKEN_URL, baseUrl);
        return serviceFactory.getRestTemplate().postForObject(url, request, ArgoSessionToken.class);
    }

    /**
     * 
     * Returns HTTP request headers
     * 
     * @param username
     * @param password
     * @return
     */
    private HttpEntity<HttpHeaders> getRequestEntity(String baseUrl, String username, String password) {
        ArgoSessionToken sessionToken = getSessionToken(baseUrl, username, password);
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
    private HttpEntity<String> getRequestEntityWithBody(String requestBody, String baseUrl, String username, String password) {
        ArgoSessionToken sessionToken = getSessionToken(baseUrl, username, password);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.setBearerAuth(sessionToken.getToken());
        return new HttpEntity<>(requestBody, requestHeaders);
    }
}
