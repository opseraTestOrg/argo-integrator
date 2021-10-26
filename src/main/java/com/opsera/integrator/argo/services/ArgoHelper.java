package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ALL_ARGO_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ALL_ARGO_REPOSITORY_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_ALL_CLUSTER_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_ALL_PROJECT_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_CREATE_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_PROJECT_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_REPOSITORY_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SESSION_TOKEN_URL;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.HTTP_EMPTY_BODY;
import static com.opsera.integrator.argo.resources.Constants.HTTP_HEADER_ACCEPT;
import static com.opsera.integrator.argo.resources.Constants.ARGO_CLUSTER_URL_TEMPLATE;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
import com.opsera.integrator.argo.resources.ArgoClusterItem;
import com.opsera.integrator.argo.resources.ArgoClusterList;
import com.opsera.integrator.argo.resources.ArgoRepositoriesList;
import com.opsera.integrator.argo.resources.ArgoRepositoryItem;
import com.opsera.integrator.argo.resources.ArgoSessionRequest;
import com.opsera.integrator.argo.resources.ArgoSessionToken;
import com.opsera.integrator.argo.resources.CreateClusterRequest;
import com.opsera.integrator.argo.resources.CreateProjectRequest;

/**
 * Class handles all the interaction with argo server.
 */
@Component
public class ArgoHelper {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(ArgoHelper.class);

    /** The service factory. */
    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * get argo application details.
     *
     * @param applicationName the application name
     * @param baseUrl         the base url
     * @param username        the username
     * @param password        the password
     * @return the argo application
     */
    public ArgoApplicationItem getArgoApplication(String applicationName, String baseUrl, String username, String password) {
        LOGGER.debug("Starting to fetch Argo Application for applicationName {}", applicationName);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, baseUrl, applicationName);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoApplicationItem(response.getBody());
    }

    /**
     * get all argo applications.
     *
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the all argo applications
     */
    public ArgoApplicationsList getAllArgoApplications(String baseUrl, String username, String password) {
        LOGGER.debug("Starting to get All Argo Applications for baseUrl {}", baseUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ARGO_APPLICATION_URL_TEMPLATE, baseUrl);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoApplicationsList(response.getBody());
    }

    /**
     * get all argo clusters.
     *
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the all argo clusters
     */
    public ArgoClusterList getAllArgoClusters(String baseUrl, String username, String password) {
        LOGGER.debug("Starting to get All Argo Clusters for baseUrl {}", baseUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ARGO_ALL_CLUSTER_URL_TEMPLATE, baseUrl);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoClustersList(response.getBody());
    }

    /**
     * get all argo projects.
     *
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the all argo projects
     */
    public ArgoApplicationsList getAllArgoProjects(String baseUrl, String username, String password) {
        LOGGER.debug("Starting to get All Argo Projects for baseUrl {}", baseUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ARGO_ALL_PROJECT_URL_TEMPLATE, baseUrl);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoApplicationsList(response.getBody());
    }

    /**
     * sync an argo application.
     *
     * @param applicationName the application name
     * @param baseUrl         the base url
     * @param username        the username
     * @param password        the password
     * @return the argo application item
     */
    public ArgoApplicationItem syncApplication(String applicationName, String baseUrl, String username, String password) {
        LOGGER.debug("Starting to Sync Argo Application for applicationName {}", applicationName);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, baseUrl, username, password);
        String url = String.format(ARGO_SYNC_APPLICATION_URL_TEMPLATE, baseUrl, applicationName);
        ResponseEntity<ArgoApplicationItem> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.POST, requestEntity, ArgoApplicationItem.class);
        return response.getBody();
    }

    /**
     * creates an argo application.
     *
     * @param argoApplication the argo application
     * @param baseUrl         the base url
     * @param username        the username
     * @param password        the password
     * @return the response entity
     */
    public ResponseEntity<String> createApplication(ArgoApplicationItem argoApplication, String baseUrl, String username, String password) {
        LOGGER.debug("Starting to Create Argo Application for request {}", argoApplication);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), baseUrl, username, password);
        String url = String.format(ARGO_CREATE_APPLICATION_URL_TEMPLATE, baseUrl);
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.POST, requestEntity, String.class);
    }

    /**
     * update an argo application.
     *
     * @param argoApplication the argo application
     * @param baseUrl         the base url
     * @param username        the username
     * @param password        the password
     * @param applicationName the application name
     * @return the response entity
     */
    public ResponseEntity<String> updateApplication(ArgoApplicationItem argoApplication, String baseUrl, String username, String password, String applicationName) {
        LOGGER.debug("Starting to Update Argo Application for request {}", argoApplication);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), baseUrl, username, password);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, baseUrl, applicationName);
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.PUT, requestEntity, String.class);
    }

    /**
     * delete the application details.
     *
     * @param applicationName the application name
     * @param baseUrl         the base url
     * @param username        the username
     * @param password        the password
     */
    public void deleteArgoApplication(String applicationName, String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to delete the application {} and url {} ", applicationName, baseUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, baseUrl, applicationName);
        serviceFactory.getRestTemplate().exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        LOGGER.debug("To Completed to delete the application {} and url {} ", applicationName, baseUrl);
    }

    /**
     * Returns argo session token.
     *
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the session token
     */
    private ArgoSessionToken getSessionToken(String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to get session token with baseUrl {}", baseUrl);
        ArgoSessionRequest request = new ArgoSessionRequest(username, password);
        String url = String.format(ARGO_SESSION_TOKEN_URL, baseUrl);
        return serviceFactory.getRestTemplate().postForObject(url, request, ArgoSessionToken.class);
    }

    /**
     * Returns HTTP request headers.
     *
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the request entity
     */
    private HttpEntity<HttpHeaders> getRequestEntity(String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to get Request Entity for baseUrl {}", baseUrl);
        ArgoSessionToken sessionToken = getSessionToken(baseUrl, username, password);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.setBearerAuth(sessionToken.getToken());
        return new HttpEntity<>(requestHeaders);
    }

    /**
     * Returns header with body.
     *
     * @param requestBody the request body
     * @param baseUrl     the base url
     * @param username    the username
     * @param password    the password
     * @return the request entity with body
     */
    private HttpEntity<String> getRequestEntityWithBody(String requestBody, String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to get Request Entity with Body {}", requestBody);
        ArgoSessionToken sessionToken = getSessionToken(baseUrl, username, password);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.setBearerAuth(sessionToken.getToken());
        return new HttpEntity<>(requestBody, requestHeaders);
    }

    /**
     * Gets the argo repository.
     *
     * @param repositoryUrl the repository url
     * @param baseUrl       the base url
     * @param username      the username
     * @param password      the password
     * @return the argo repository
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public ArgoRepositoryItem getArgoRepository(String repositoryUrl, String baseUrl, String username, String password) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to get the repository {} and url {} ", repositoryUrl, baseUrl);
        repositoryUrl = encodeURL(repositoryUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ARGO_REPOSITORY_URL_TEMPLATE, baseUrl, repositoryUrl);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoRepositoryItem(response.getBody());
    }

    /**
     * Gets the argo repositories list.
     *
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the argo repositories list
     */
    public ArgoRepositoriesList getArgoRepositoriesList(String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to get all the repositories {} ", baseUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ALL_ARGO_REPOSITORY_URL_TEMPLATE, baseUrl);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoRepositoriesList(response.getBody());
    }

    /**
     * Creates the repository.
     *
     * @param argoApplication the argo application
     * @param baseUrl         the base url
     * @param username        the username
     * @param password        the password
     * @return the response entity
     */
    public ResponseEntity<String> createRepository(ArgoRepositoryItem argoApplication, String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to create the repository {} and url {} ", argoApplication.getRepo(), baseUrl);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), baseUrl, username, password);
        String url = String.format(ALL_ARGO_REPOSITORY_URL_TEMPLATE, baseUrl);
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.POST, requestEntity, String.class);
    }

    /**
     * Update repository.
     *
     * @param argoApplication the argo application
     * @param baseUrl         the base url
     * @param username        the username
     * @param password        the password
     * @return the response entity
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public ResponseEntity<String> updateRepository(ArgoRepositoryItem argoApplication, String baseUrl, String username, String password) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to update the repository {} and url {} ", argoApplication.getRepo(), baseUrl);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), baseUrl, username, password);
        String repositoryUrl = encodeURL(argoApplication.getRepo());
        String url = String.format(ARGO_REPOSITORY_URL_TEMPLATE, baseUrl, repositoryUrl);
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.PUT, requestEntity, String.class);
    }

    /**
     * Delete argo repository.
     *
     * @param repositoryUrl the repository url
     * @param baseUrl       the base url
     * @param username      the username
     * @param password      the password
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public void deleteArgoRepository(String repositoryUrl, String baseUrl, String username, String password) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to delete the repository {} and url {} ", repositoryUrl, baseUrl);
        repositoryUrl = encodeURL(repositoryUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ARGO_REPOSITORY_URL_TEMPLATE, baseUrl, repositoryUrl);
        serviceFactory.getRestTemplate().exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        LOGGER.debug("To Completed to delete the repository {} and url {} ", repositoryUrl, baseUrl);
    }

    /**
     * Encode URL.
     *
     * @param repositoryUrl the repository url
     * @return the string
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    private String encodeURL(String repositoryUrl) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to encoding the repository url {} ", repositoryUrl);
        repositoryUrl = URLEncoder.encode(repositoryUrl, StandardCharsets.UTF_8.toString());
        return repositoryUrl;
    }

    /**
     * Gets the argo project.
     *
     * @param name     the name
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the argo project
     */
    public ArgoApplicationItem getArgoProject(String name, String baseUrl, String username, String password) {
        LOGGER.debug("Starting to get Argo Project for projectname {}", name);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ARGO_PROJECT_URL_TEMPLATE, baseUrl, name);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoApplicationItem(response.getBody());
    }

    /**
     * Creates the project.
     *
     * @param request  the request
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the response entity
     */
    public ResponseEntity<String> createProject(CreateProjectRequest request, String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to create the project {} and url {} ", request.getProject().getMetadata().getName(), baseUrl);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), baseUrl, username, password);
        String url = String.format(ARGO_ALL_PROJECT_URL_TEMPLATE, baseUrl);
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.POST, requestEntity, String.class);
    }

    /**
     * Delete argo project.
     *
     * @param projectName the project name
     * @param baseUrl     the base url
     * @param username    the username
     * @param password    the password
     */
    public void deleteArgoProject(String projectName, String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to delete the project {} and url {} ", projectName, baseUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String url = String.format(ARGO_PROJECT_URL_TEMPLATE, baseUrl, projectName);
        serviceFactory.getRestTemplate().exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        LOGGER.debug("To Completed to delete the project {} and url {} ", projectName, baseUrl);
    }

    /**
     * Update project.
     *
     * @param request  the request
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the response entity
     */
    public ResponseEntity<String> updateProject(CreateProjectRequest request, String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to create the project {} and url {} ", request.getProject().getMetadata().getName(), baseUrl);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), baseUrl, username, password);
        String url = String.format(ARGO_PROJECT_URL_TEMPLATE, baseUrl, request.getProject().getMetadata().getName());
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.PUT, requestEntity, String.class);
    }

    /**
     * Gets the argo cluster.
     *
     * @param serverName the server name
     * @param baseUrl    the base url
     * @param username   the username
     * @param password   the password
     * @return the argo cluster
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public ArgoClusterItem getArgoCluster(String serverName, String baseUrl, String username, String password) throws UnsupportedEncodingException {
        LOGGER.debug("Starting to get All Argo Clusters for baseUrl {}", baseUrl);
        ResponseEntity<String> response = null;
        try {
            HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
            String serverUrl = encodeURL(serverName);
            String url = String.format(ARGO_CLUSTER_URL_TEMPLATE, baseUrl, serverUrl);
            response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
            return serviceFactory.getResponseParser().extractArgoCluster(response.getBody());
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Creates the cluster.
     *
     * @param request  the request
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the response entity
     */
    public ResponseEntity<String> createCluster(CreateClusterRequest request, String baseUrl, String username, String password) {
        LOGGER.debug("To Starting to create the cluster {} and url {} ", request.getName(), baseUrl);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), baseUrl, username, password);
        String url = String.format(ARGO_ALL_CLUSTER_URL_TEMPLATE, baseUrl);
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.POST, requestEntity, String.class);
    }

    /**
     * Update cluster.
     *
     * @param request  the request
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the response entity
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public ResponseEntity<String> updateCluster(CreateClusterRequest request, String baseUrl, String username, String password) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to create the cluster {} and url {} ", request.getName(), baseUrl);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), baseUrl, username, password);
        String serverUrl = encodeURL(request.getServer());
        String url = String.format(ARGO_CLUSTER_URL_TEMPLATE, baseUrl, serverUrl);
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.PUT, requestEntity, String.class);
    }

    /**
     * Delete argo cluster.
     *
     * @param server   the server
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public void deleteArgoCluster(String server, String baseUrl, String username, String password) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to delete the project {} and url {} ", server, baseUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(baseUrl, username, password);
        String serverUrl = encodeURL(server);
        String url = String.format(ARGO_CLUSTER_URL_TEMPLATE, baseUrl, serverUrl);
        serviceFactory.getRestTemplate().exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        LOGGER.debug("To Completed to delete the project {} and url {} ", server, baseUrl);
    }
}
