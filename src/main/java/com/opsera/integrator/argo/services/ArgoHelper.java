package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ALL_ARGO_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ALL_ARGO_REPOSITORY_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_ALL_CLUSTER_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_ALL_PROJECT_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_DETAILS;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_LOG_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_RESOURCE_ACTIONS_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_RESOURCE_TREE_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_CLUSTER_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_CREATE_APPLICATION_URL_TEMPLATE;
import static com.opsera.integrator.argo.resources.Constants.ARGO_DELETE_REPLICASET_CUSTOM;
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

import com.opsera.core.rest.RestTemplateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.InvalidRequestException;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationSource;
import com.opsera.integrator.argo.resources.ArgoApplicationSpec;
import com.opsera.integrator.argo.resources.ArgoApplicationsList;
import com.opsera.integrator.argo.resources.ArgoClusterList;
import com.opsera.integrator.argo.resources.ArgoRepositoriesList;
import com.opsera.integrator.argo.resources.ArgoRepositoryItem;
import com.opsera.integrator.argo.resources.ArgoSessionRequest;
import com.opsera.integrator.argo.resources.ArgoSessionToken;
import com.opsera.integrator.argo.resources.CreateClusterRequest;
import com.opsera.integrator.argo.resources.CreateProjectRequest;
import com.opsera.integrator.argo.resources.LogResult;
import com.opsera.integrator.argo.resources.Node;
import com.opsera.integrator.argo.resources.ResourceTree;
import com.opsera.integrator.argo.resources.RolloutActions;
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

    @Autowired
    private RestTemplateHelper restTemplateHelper;
    /**
     * get argo application details.
     *
     * @param applicationName the application name
     * @param toolConfig
     * @param argoPassword
     * @return the argo application
     */
    public ArgoApplicationItem getArgoApplication(String applicationName, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("Starting to fetch Argo Application for applicationName {}", applicationName);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        String response = restTemplateHelper.getForEntity(String.class, url, requestEntity);
        return serviceFactory.getResponseParser().extractArgoApplicationItem(response);
    }

    /**
     * get all argo applications.
     *
     * @param toolConfig
     * @param argoPassword
     * @return the all argo applications
     */
    public ArgoApplicationsList getAllArgoApplications(ToolConfig toolConfig, String argoPassword) throws IOException {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL());
        String response = restTemplateHelper.getForEntity(String.class, url, requestEntity);
        return serviceFactory.getResponseParser().extractArgoApplicationsList(response);
    }

    /**
     * get all argo clusters.
     *
     * @param toolConfig
     * @param argoPassword
     * @return the all argo clusters
     */
    public ArgoClusterList getAllArgoClusters(ToolConfig toolConfig, String argoPassword) throws IOException {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_ALL_CLUSTER_URL_TEMPLATE, toolConfig.getToolURL());
        String response = restTemplateHelper.getForEntity(String.class, url, requestEntity);
        return serviceFactory.getResponseParser().extractArgoClustersList(response);
    }

    /**
     * get all argo projects.
     *
     * @param toolConfig
     * @param argoPassword
     * @return the all argo projects
     */
    public ArgoApplicationsList getAllArgoProjects(ToolConfig toolConfig, String argoPassword) throws IOException {
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_ALL_PROJECT_URL_TEMPLATE, toolConfig.getToolURL());
        String response = restTemplateHelper.getForEntity(String.class, url, requestEntity);
        return serviceFactory.getResponseParser().extractArgoApplicationsList(response);
    }

    /**
     * sync an argo application.
     *
     * @param applicationName the application name
     * @param toolConfig
     * @param argoPassword
     * @return the argo application item
     */
    public ArgoApplicationItem syncApplication(String applicationName, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("Starting to Sync Argo Application for applicationName {}", applicationName);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, toolConfig, argoPassword);
        String url = String.format(ARGO_SYNC_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        ArgoApplicationItem response = restTemplateHelper.postForEntity(ArgoApplicationItem.class, url, requestEntity);
        return response;
    }

    /**
     * Sync application operation.
     *
     * @param applicationName the application name
     * @param toolConfig
     * @param argoPassword
     * @return the argo application item
     */
    public ArgoApplicationItem syncApplicationOperation(String applicationName, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("Starting to Sync Argo Application Operation for applicationName {}", applicationName);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, toolConfig, argoPassword);
        String url = String.format(ARGO_SYNC_APPLICATION_OPERATION_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        ArgoApplicationItem response = restTemplateHelper.getForEntity(ArgoApplicationItem.class, url, requestEntity);

        return response;
    }

    /**
     * creates an argo application.
     *
     * @param argoApplication the argo application
     * @param toolConfig
     * @param argoPassword
     * @return the response entity
     */
    public ResponseEntity<String> createApplication(ArgoApplicationItem argoApplication, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("Starting to Create Argo Application for request {}", argoApplication);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), toolConfig, argoPassword);
        String url = String.format(ARGO_CREATE_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL());
        return new ResponseEntity<>(restTemplateHelper.postForEntity(String.class, url, requestEntity), HttpStatus.OK);
    }

    /**
     * update an argo application.
     *
     * @param argoApplication the argo application
     * @param toolConfig
     * @param argoPassword
     * @param applicationName the application name
     * @return the response entity
     */
    public ResponseEntity<String> updateApplication(ArgoApplicationItem argoApplication, ToolConfig toolConfig, String argoPassword, String applicationName) throws IOException {
        LOGGER.debug("Starting to Update Argo Application for request {}", argoApplication);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), toolConfig, argoPassword);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        return new ResponseEntity<>(restTemplateHelper.putForEntity(String.class, url, requestEntity), HttpStatus.OK);
    }

    /**
     * delete the application details.
     *
     * @param applicationName the application name
     * @param toolConfig
     * @param argoPassword
     */
    public void deleteArgoApplication(String applicationName, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to delete the application {} and url {} ", applicationName, toolConfig.getToolURL());
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ALL_ARGO_APPLICATION_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        restTemplateHelper.delete(url, requestEntity);
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
            return restTemplateHelper.postForEntity(ArgoSessionToken.class, url, request);
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
            UserInfo response = restTemplateHelper.getForEntity(UserInfo.class, url, HttpMethod.GET, new HttpEntity<>(requestHeaders));
            return response;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new InvalidRequestException(INVALID_CONNECTION_DETAILS);
        }
    }

    /**
     * Returns HTTP request headers.
     *
     * @param toolConfig
     * @param argoPassword
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
            if (StringUtils.hasText(userInfo.getUsername())) {
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
     * @param toolConfig
     * @param argoPassword
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
     * @param toolConfig
     * @param argoPassword
     * @return the argo repository
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public ArgoRepositoryItem getArgoRepository(String repositoryUrl, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("To Starting to get the repository {} and url {} ", repositoryUrl, toolConfig.getToolURL());
        repositoryUrl = encodeURL(repositoryUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_REPOSITORY_URL_TEMPLATE, toolConfig.getToolURL(), repositoryUrl);
        String response = restTemplateHelper.getForEntity(String.class, url, requestEntity);
        return serviceFactory.getResponseParser().extractArgoRepositoryItem(response);
    }

    /**
     * Gets the argo repositories list.
     *
     * @param toolConfig
     * @param argoPassword
     * @return the argo repositories list
     */
    public ArgoRepositoriesList getArgoRepositoriesList(ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("To Starting to get all the repositories {} ", toolConfig.getToolURL());
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ALL_ARGO_REPOSITORY_URL_TEMPLATE, toolConfig.getToolURL());
        String response = restTemplateHelper.getForEntity(String.class, url, requestEntity);
        return serviceFactory.getResponseParser().extractArgoRepositoriesList(response);
    }

    /**
     * Creates the repository.
     *
     * @param argoApplication the argo application
     * @param toolConfig
     * @param argoPassword
     * @return the response entity
     */
    public ResponseEntity<String> createRepository(ArgoRepositoryItem argoApplication, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("To Starting to create the repository {} and url {} ", argoApplication.getRepo(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), toolConfig, argoPassword);
        String url = String.format(ALL_ARGO_REPOSITORY_URL_TEMPLATE, toolConfig.getToolURL());
        return new ResponseEntity<>(restTemplateHelper.postForEntity(String.class, url, requestEntity), HttpStatus.OK);
    }

    /**
     * Update repository.
     *
     * @param argoApplication the argo application
     * @param toolConfig
     * @param argoPassword
     * @return the response entity
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public ResponseEntity<String> updateRepository(ArgoRepositoryItem argoApplication, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("To Starting to update the repository {} and url {} ", argoApplication.getRepo(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(argoApplication), toolConfig, argoPassword);
        String repositoryUrl = encodeURL(argoApplication.getRepo());
        String url = String.format(ARGO_REPOSITORY_URL_TEMPLATE, toolConfig.getToolURL(), repositoryUrl);
        return new ResponseEntity<>(restTemplateHelper.putForEntity(String.class, url, requestEntity), HttpStatus.OK);
    }

    /**
     * Delete argo repository.
     *
     * @param repositoryUrl the repository url
     * @param toolConfig
     * @param argoPassword
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public void deleteArgoRepository(String repositoryUrl, ToolConfig toolConfig, String argoPassword) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to delete the repository {} and url {} ", repositoryUrl, toolConfig.getToolURL());
        repositoryUrl = encodeURL(repositoryUrl);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_REPOSITORY_URL_TEMPLATE, toolConfig.getToolURL(), repositoryUrl);
        restTemplateHelper.delete(url, requestEntity);
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
     * @param toolConfig
     * @param argoPassword
     * @return the argo project
     */
    public ArgoApplicationItem getArgoProject(String name, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("Starting to get Argo Project for projectname {}", name);
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_PROJECT_URL_TEMPLATE, toolConfig.getToolURL(), name);
        String response = restTemplateHelper.getForEntity(String.class, url, requestEntity);
        return serviceFactory.getResponseParser().extractArgoApplicationItem(response);
    }

    /**
     * Creates the project.
     *
     * @param request  the request
     * @param toolConfig
     * @param argoPassword
     * @return the response entity
     */
    public ResponseEntity<String> createProject(CreateProjectRequest request, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("To Starting to create the project {} and url {} ", request.getProject().getMetadata().getName(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), toolConfig, argoPassword);
        String url = String.format(ARGO_ALL_PROJECT_URL_TEMPLATE, toolConfig.getToolURL());
        return new ResponseEntity<>(restTemplateHelper.postForEntity(String.class, url, requestEntity), HttpStatus.OK);
    }

    /**
     * Delete argo project.
     *
     * @param projectName the project name
     * @param toolConfig
     * @param argoPassword
     */
    public void deleteArgoProject(String projectName, ToolConfig toolConfig, String argoPassword) {
        LOGGER.debug("To Starting to delete the project {} and url {} ", projectName, toolConfig.getToolURL());
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String url = String.format(ARGO_PROJECT_URL_TEMPLATE, toolConfig.getToolURL(), projectName);
        restTemplateHelper.delete(url, requestEntity);
        LOGGER.debug("To Completed to delete the project {} and url {} ", projectName, toolConfig.getToolURL());
    }

    /**
     * Update project.
     *
     * @param request  the request
     * @param toolConfig
     * @param argoPassword
     * @return the response entity
     */
    public ResponseEntity<String> updateProject(CreateProjectRequest request, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("To Starting to update the project {} and url {} ", request.getProject().getMetadata().getName(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), toolConfig, argoPassword);
        String url = String.format(ARGO_PROJECT_URL_TEMPLATE, toolConfig.getToolURL(), request.getProject().getMetadata().getName());
        return new ResponseEntity<>(restTemplateHelper.putForEntity(String.class, url, requestEntity), HttpStatus.OK);
    }

    /**
     * Creates the cluster.
     *
     * @param request  the request
     * @param toolConfig
     * @param argoPassword
     * @return the response entity
     */
    public ResponseEntity<String> createCluster(CreateClusterRequest request, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("To Starting to create the cluster {} and url {} ", request.getName(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), toolConfig, argoPassword);
        String url = String.format(ARGO_ALL_CLUSTER_URL_TEMPLATE, toolConfig.getToolURL());
        return new ResponseEntity<>(restTemplateHelper.postForEntity(String.class, url, requestEntity), HttpStatus.OK);
    }

    /**
     * Update cluster.
     *
     * @param request  the request
     * @param toolConfig
     * @param argoPassword
     * @return the response entity
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public ResponseEntity<String> updateCluster(CreateClusterRequest request, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("To Starting to update the cluster {} and url {} ", request.getName(), toolConfig.getToolURL());
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(request), toolConfig, argoPassword);
        String serverUrl = encodeURL(request.getServer());
        String url = String.format(ARGO_CLUSTER_URL_TEMPLATE, toolConfig.getToolURL(), serverUrl);
        return new ResponseEntity<>(restTemplateHelper.putForEntity(String.class, url, requestEntity), HttpStatus.OK);
    }

    /**
     * Delete argo cluster.
     *
     * @param server   the server
     * @param toolConfig
     * @param argoPassword
     * @throws UnsupportedEncodingException the unsupported encoding exception
     */
    public void deleteArgoCluster(String server, ToolConfig toolConfig, String argoPassword) throws UnsupportedEncodingException {
        LOGGER.debug("To Starting to delete the cluster {} and url {} ", server, toolConfig.getToolURL());
        HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
        String serverUrl = encodeURL(server);
        String url = String.format(ARGO_CLUSTER_URL_TEMPLATE, toolConfig.getToolURL(), serverUrl);
        restTemplateHelper.delete(url, requestEntity);
        LOGGER.debug("To Completed to delete the cluster {} and url {} ", server, toolConfig.getToolURL());
    }

    /**
     * Gets the argo application log.
     *
     * @param applicationName the application name
     * @param toolConfig
     * @param argoPassword
     * @param podName
     * @param namespace
     * @return the argo application log
     */
    public String getArgoApplicationLog(String applicationName, ToolConfig toolConfig, String argoPassword, String podName, String namespace) throws IOException {
        LOGGER.debug("Starting to get argo Application log for applicationName {}", applicationName);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, toolConfig, argoPassword);
        String url = String.format(ARGO_APPLICATION_LOG_URL_TEMPLATE, toolConfig.getToolURL(), applicationName, podName, namespace);
        String response = restTemplateHelper.getForEntity(String.class, url, requestEntity);
        String structure = response;
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

    public ResourceTree getResourceTree(String applicationName, ToolConfig toolConfig, String argoPassword) throws IOException {
        LOGGER.debug("Starting to get argo Application log for applicationName {}", applicationName);
        HttpEntity<String> requestEntity = getRequestEntityWithBody(HTTP_EMPTY_BODY, toolConfig, argoPassword);
        String url = String.format(ARGO_APPLICATION_RESOURCE_TREE_URL_TEMPLATE, toolConfig.getToolURL(), applicationName);
        ResourceTree response = restTemplateHelper.getForEntity(ResourceTree.class, url, requestEntity);
        return response;
    }

    public RolloutActions getArgoApplicationResourceActions(String applicationName, Node node, ToolConfig toolConfig, String argoPassword, String status) throws IOException {
        LOGGER.debug("Starting to get application resource actions {}", applicationName);
        String url = String.format(ARGO_APPLICATION_RESOURCE_ACTIONS_TEMPLATE, toolConfig.getToolURL(), applicationName, node.getNamespace(), node.getName());
        RolloutActions response = null;
        if (StringUtils.hasText(status)) {
            HttpEntity<String> requestEntity = getRequestEntityWithBody("\"" + status + "\"", toolConfig, argoPassword);
            response = restTemplateHelper.postForEntity(RolloutActions.class, url, requestEntity);
        } else {
            HttpEntity<HttpHeaders> requestEntity = getRequestEntity(toolConfig, argoPassword);
            response = restTemplateHelper.getForEntity(RolloutActions.class, url, requestEntity);
        }
        return response;
    }

    /**
     * get Argo app details
     *
     * @param toolConfig
     * @param argoPassword
     * @param spec
     * @return the argo application item
     * @throws UnsupportedEncodingException
     */
    public ArgoApplicationSource getAppdetails(ToolConfig toolConfig, String argoPassword, ArgoApplicationSpec spec) throws IOException {
        LOGGER.debug("Starting to get the appdetails ");
        HttpEntity<String> requestEntity = getRequestEntityWithBody(serviceFactory.gson().toJson(spec), toolConfig, argoPassword);
        String url = String.format(ARGO_APPLICATION_DETAILS, toolConfig.getToolURL(), URLEncoder.encode(spec.getSource().getRepoURL(), "UTF-8"));
        ArgoApplicationSource response = restTemplateHelper.postForEntity(ArgoApplicationSource.class, url, requestEntity);
        return response;
    }
}
