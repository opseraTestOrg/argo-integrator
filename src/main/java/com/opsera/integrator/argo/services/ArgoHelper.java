package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ALL_ARGO_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ALL_ARGO_REPOSITORY_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_ALL_CLUSTER_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_ALL_PROJECT_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_LOG_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_RESOURCE_TREE_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_CLUSTER_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_CREATE_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_GET_USER_INFO_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_PROJECT_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_REPOSITORY_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SESSION_TOKEN_URL;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_APPLICATION_OPERATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_SYNC_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.HTTP_EMPTY_BODY;
import static com.opsera.integrator.argo.resources.Constants.HTTP_HEADER_ACCEPT;
import static com.opsera.integrator.argo.resources.Constants.INVALID_CONNECTION_DETAILS;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.InvalidRequestException;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationsList;
import com.opsera.integrator.argo.resources.ArgoClusterList;
import com.opsera.integrator.argo.resources.ArgoRepositoriesList;
import com.opsera.integrator.argo.resources.ArgoRepositoryItem;
import com.opsera.integrator.argo.resources.ArgoSessionRequest;
import com.opsera.integrator.argo.resources.ArgoSessionToken;
import com.opsera.integrator.argo.resources.CreateClusterRequest;
import com.opsera.integrator.argo.resources.CreateProjectRequest;
import com.opsera.integrator.argo.resources.LogResult;
import com.opsera.integrator.argo.resources.ResourceTree;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.integrator.argo.resources.UserInfo;

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
    public ArgoApplicationItem getArgoApplication(String applicationName, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("Starting to fetch Argo Application for applicationName {}", applicationName);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        return serviceFactory.getResponseParser().extractArgoApplicationItem(response.getBody());
    }

    /**
     * get all argo applications.
     *
     * @param toolConfig the base url
     * @param username   the username
     * @return the all argo applications
     */
    public ArgoApplicationsList getAllArgoApplications(ToolConfig toolConfig, String argoPassword) {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL());
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
    public ArgoClusterList getAllArgoClusters(ToolConfig toolConfig, String argoPassword) {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_ALL_CLUSTER_URL_TEMPLATE, toolConfig.getToolURL());
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
    public ArgoApplicationsList getAllArgoProjects(ToolConfig toolConfig, String argoPassword) {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_ALL_PROJECT_URL_TEMPLATE, toolConfig.getToolURL());
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
    public ArgoApplicationItem syncApplication(String applicationName, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("Starting to Sync Argo Application for applicationName {}", applicationName);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, toolConfig, argoPassword);
        String url = String.format(ARGO_SYNC_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        ResponseEntity<ArgoApplicationItem> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.POST, requestEntity, ArgoApplicationItem.class);
        return response.getBody();
    }

    /**
     * Sync application operation.
     *
     * @param applicationName the application name
     * @param baseUrl         the base url
     * @param username        the username
     * @param password        the password
     * @return the argo application item
     */
    public ArgoApplicationItem syncApplicationOperation(String applicationName, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("Starting to Sync Argo Application Operation for applicationName {}", applicationName);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, toolConfig, argoPassword);
        String url = String.format(ARGO_SYNC_APPLICATION_OPERATION_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        ResponseEntity<ArgoApplicationItem> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, ArgoApplicationItem.class);
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
    public ResponseEntity<String> createApplication(ArgoApplicationItem argoApplication, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("Starting to Create Argo Application for request {}", argoApplication);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), toolConfig, argoPassword);
        String url = String.format(ARGO_CREATE_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL());
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
    public ResponseEntity<String> updateApplication(ArgoApplicationItem argoApplication, ToolConfig toolConfig, String argoPassword, String applicationName) {
        LOGGER.debug("Starting to Update Argo Application for request {}", argoApplication);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), toolConfig, argoPassword);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
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
    public void deleteArgoApplication(String applicationName, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to delete the application {} and url {} ", applicationName, toolConfig.getToolURL());
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        serviceFactory.getRestTemplate().exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        LOGGER.debug("To Completed to delete the application {} and url {} ", applicationName, toolConfig.getToolURL());
    }

    /**
     * Returns argo session token.
     *
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the session token
     */

    public ArgoSessionToken getSessionToken(String baseUrl, String username, String password) {
        try {
            LOGGER.debug("To Starting to get session token with baseUrl {}, username: {}", baseUrl, username);
            ArgoSessionRequest request = new ArgoSessionRequest(username, password);
            String url = String.format(ARGO_SESSION_TOKEN_URL, baseUrl);
            return serviceFactory.getRestTemplate().postForObject(url, request, ArgoSessionToken.class);
        } catch (Exception e) {
            LOGGER.error("Exception occured while invoking getSessionToken. Message: {}", e.getMessage());
            throw new InvalidRequestException(INVALID_CONNECTION_DETAILS);
        }
    }

    private UserInfo getUserInfo(String baseUrl, String argoToken) {
        try {
            LOGGER.debug("To Starting to get user info to valiate token with baseUrl {}", baseUrl);
            String url = String.format(ARGO_GET_USER_INFO_TEMPLATE, baseUrl);
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add(HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            requestHeaders.setBearerAuth(argoToken);
            ResponseEntity<UserInfo> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeaders), UserInfo.class);
            return response.getBody();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new InvalidRequestException(INVALID_CONNECTION_DETAILS);
        }
    }

    /**
     * Returns HTTP request headers.
     *
     * @param baseUrl  the base url
     * @param username the username
     * @param password the password
     * @return the request entity
     */
    private HttpEntity<HttpHeaders> getRequestEntity(ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to get Request Entity for baseUrl {}", toolConfig.getToolURL());
        String argoToken = getArgoBearerToken(toolConfig, argoPassword);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.setBearerAuth(argoToken);
        return new HttpEntity<>(requestHeaders);
    }

    private String getArgoBearerToken(ToolConfig toolConfig, String argoPassword) {
        String argoToken;
        if (!toolConfig.isSecretAccessTokenEnabled()) {
            ArgoSessionToken sessionToken = getSessionToken(toolConfig.getToolURL(), toolConfig.getUserName(), argoPassword);
            argoToken = sessionToken.getToken();
        } else {
            UserInfo userInfo = getUserInfo(toolConfig.getToolURL(), argoPassword);
            if (!StringUtils.isEmpty(userInfo.getUsername())) {
                argoToken = argoPassword;
            } else {
                throw new InvalidRequestException(INVALID_CONNECTION_DETAILS);
            }
        }
        return argoToken;
    }

    /**
     * Return header with object body
     * 
     * @param argoToken
     * @param request
     * @return
     */
    public HttpEntity<Object> getRequestEntity(String argoToken, Object request) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.setBearerAuth(argoToken);
        return new HttpEntity<>(request, requestHeaders);
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
    public HttpEntity<String> getRequestEntityWithBody(String requestBody, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to get Request Entity for baseUrl {} with payload", toolConfig.getToolURL());
        String argoToken = getArgoBearerToken(toolConfig, argoPassword);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HTTP_HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.setBearerAuth(argoToken);
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
    public ArgoRepositoryItem getArgoRepository(String repositoryUrl, ToolConfig toolConfig, String argoPassword) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to get the repository {} and url {} ", repositoryUrl, toolConfig.getToolURL());
        repositoryUrl = encodeURL(repositoryUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_REPOSITORY_URL_TEMPLATE, toolConfig.getToolURL(), repositoryUrl);
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
    public ArgoRepositoriesList getArgoRepositoriesList(ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to get all the repositories {} ", toolConfig.getToolURL());
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ALL_ARGO_REPOSITORY_URL_TEMPLATE, toolConfig.getToolURL());
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
    public ResponseEntity<String> createRepository(ArgoRepositoryItem argoApplication, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to create the repository {} and url {} ", argoApplication.getRepo(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), toolConfig, argoPassword);
        String url = String.format(ALL_ARGO_REPOSITORY_URL_TEMPLATE, toolConfig.getToolURL());
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
    public ResponseEntity<String> updateRepository(ArgoRepositoryItem argoApplication, ToolConfig toolConfig, String argoPassword) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to update the repository {} and url {} ", argoApplication.getRepo(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), toolConfig, argoPassword);
        String repositoryUrl = encodeURL(argoApplication.getRepo());
        String url = String.format(ARGO_REPOSITORY_URL_TEMPLATE, toolConfig.getToolURL(), repositoryUrl);
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
    public void deleteArgoRepository(String repositoryUrl, ToolConfig toolConfig, String argoPassword) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to delete the repository {} and url {} ", repositoryUrl, toolConfig.getToolURL());
        repositoryUrl = encodeURL(repositoryUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_REPOSITORY_URL_TEMPLATE, toolConfig.getToolURL(), repositoryUrl);
        serviceFactory.getRestTemplate().exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        LOGGER.debug("To Completed to delete the repository {} and url {} ", repositoryUrl, toolConfig.getToolURL());
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
    public ArgoApplicationItem getArgoProject(String name, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("Starting to get Argo Project for projectname {}", name);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_PROJECT_URL_TEMPLATE, toolConfig.getToolURL(), name);
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
    public ResponseEntity<String> createProject(CreateProjectRequest request, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to create the project {} and url {} ", request.getProject().getMetadata().getName(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), toolConfig, argoPassword);
        String url = String.format(ARGO_ALL_PROJECT_URL_TEMPLATE, toolConfig.getToolURL());
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
    public void deleteArgoProject(String projectName, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to delete the project {} and url {} ", projectName, toolConfig.getToolURL());
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_PROJECT_URL_TEMPLATE, toolConfig.getToolURL(), projectName);
        serviceFactory.getRestTemplate().exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        LOGGER.debug("To Completed to delete the project {} and url {} ", projectName, toolConfig.getToolURL());
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
    public ResponseEntity<String> updateProject(CreateProjectRequest request, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to update the project {} and url {} ", request.getProject().getMetadata().getName(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), toolConfig, argoPassword);
        String url = String.format(ARGO_PROJECT_URL_TEMPLATE, toolConfig.getToolURL(), request.getProject().getMetadata().getName());
        return serviceFactory.getRestTemplate().exchange(url, HttpMethod.PUT, requestEntity, String.class);
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
    public ResponseEntity<String> createCluster(CreateClusterRequest request, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to create the cluster {} and url {} ", request.getName(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), toolConfig, argoPassword);
        String url = String.format(ARGO_ALL_CLUSTER_URL_TEMPLATE, toolConfig.getToolURL());
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
    public ResponseEntity<String> updateCluster(CreateClusterRequest request, ToolConfig toolConfig, String argoPassword) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to update the cluster {} and url {} ", request.getName(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), toolConfig, argoPassword);
        String serverUrl = encodeURL(request.getServer());
        String url = String.format(ARGO_CLUSTER_URL_TEMPLATE, toolConfig.getToolURL(), serverUrl);
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
    public void deleteArgoCluster(String server, ToolConfig toolConfig, String argoPassword) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to delete the cluster {} and url {} ", server, toolConfig.getToolURL());
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String serverUrl = encodeURL(server);
        String url = String.format(ARGO_CLUSTER_URL_TEMPLATE, toolConfig.getToolURL(), serverUrl);
        serviceFactory.getRestTemplate().exchange(url, HttpMethod.DELETE, requestEntity, Void.class);
        LOGGER.debug("To Completed to delete the cluster {} and url {} ", server, toolConfig.getToolURL());
    }

    /**
     * Gets the argo application log.
     *
     * @param applicationName the application name
     * @param baseUrl         the base url
     * @param username        the username
     * @param password        the password
     * @return the argo application log
     */
    public String getArgoApplicationLog(String applicationName, ToolConfig toolConfig, String argoPassword, String podName, String namespace) {
        LOGGER.debug("Starting to get argo Application log for applicationName {}", applicationName);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, toolConfig, argoPassword);
        String url = String.format(ARGO_APPLICATION_LOG_URL_TEMPLATE, toolConfig.getToolURL(), applicationName, podName, namespace);
        ResponseEntity<String> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, String.class);
        String structure = response.getBody();
        StringBuilder sb = new StringBuilder();
        final JsonMapper mapper = new JsonMapper();
        try (MappingIterator<LogResult> it = mapper.readerFor(LogResult.class).readValues(structure)) {
            while (it.hasNextValue()) {
                LogResult logResult = it.nextValue();
                sb.append(String.format(logResult.getResult().getContent()));
                sb.append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            LOGGER.error("Exception occured while parsing the console log. message: {}", e.getMessage());
            return "";
        }
        return sb.toString();
    }

    public ResourceTree getResourceTree(String applicationName, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("Starting to get argo Application log for applicationName {}", applicationName);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, toolConfig, argoPassword);
        String url = String.format(ARGO_APPLICATION_RESOURCE_TREE_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        ResponseEntity<ResourceTree> response = serviceFactory.getRestTemplate().exchange(url, HttpMethod.GET, requestEntity, ResourceTree.class);
        return response.getBody();
    }
}
