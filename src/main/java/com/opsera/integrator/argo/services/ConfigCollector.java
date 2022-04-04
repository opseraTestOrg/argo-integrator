package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.API_GROUP;
import static com.opsera.integrator.argo.resources.Constants.API_VERSION;
import static com.opsera.integrator.argo.resources.Constants.AWS_EKS_CLUSTER_ENDPOINT;
import static com.opsera.integrator.argo.resources.Constants.AWS_STS_CLUSTER_TOKEN_ENDPOINT;
import static com.opsera.integrator.argo.resources.Constants.CLUSTERS;
import static com.opsera.integrator.argo.resources.Constants.CLUSTER_ADMIN;
import static com.opsera.integrator.argo.resources.Constants.CLUSTER_ROLE;
import static com.opsera.integrator.argo.resources.Constants.CLUSTER_ROLE_BINDING;
import static com.opsera.integrator.argo.resources.Constants.GET_TOOL_DETAILS;
import static com.opsera.integrator.argo.resources.Constants.K8_SERVCE_ACCOUNT_NAME;
import static com.opsera.integrator.argo.resources.Constants.PIPELINE_TABLE_ENDPOINT;
import static com.opsera.integrator.argo.resources.Constants.QUERY_PARM_AWS_TOOLID;
import static com.opsera.integrator.argo.resources.Constants.QUERY_PARM_CUSTOMERID;
import static com.opsera.integrator.argo.resources.Constants.QUERY_PARM_TOOLID;
import static com.opsera.integrator.argo.resources.Constants.READ_SECRETS_GLOBAL;
import static com.opsera.integrator.argo.resources.Constants.SERVICE_ACCOUNT;
import static com.opsera.integrator.argo.resources.Constants.TOKEN;
import static com.opsera.integrator.argo.resources.Constants.TOOL_REGISTRY_ENDPOINT;
import static com.opsera.integrator.argo.resources.Constants.V1;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsera.core.exception.ServiceException;
import com.opsera.core.rest.RestTemplateHelper;
import com.opsera.integrator.argo.config.AppConfig;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.AwsClusterDetails;
import com.opsera.integrator.argo.resources.AzureClusterDetails;
import com.opsera.integrator.argo.resources.CreateCluster;
import com.opsera.integrator.argo.resources.OpseraPipelineMetadata;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.integrator.argo.resources.ToolDetails;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.kubernetes.client.openapi.models.V1ClusterRoleBindingList;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1RoleRef;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1ServiceAccountList;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.openapi.models.V1Subject;
import io.kubernetes.client.util.Config;

/**
 * Class to interact and fetch the configurations.
 */
@Component
public class ConfigCollector {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(ConfigCollector.class);

    /** The service factory. */
    @Autowired
    private IServiceFactory serviceFactory;

    /** The app config. */
    @Autowired
    private AppConfig appConfig;

    /**
     * Get the argo config defined for the given pipeline/step.
     *
     * @param opseraPipelineMetadata the opsera pipeline metadata
     * @return the argo details
     * @throws IOException 
     */
    public ToolConfig getArgoDetails(OpseraPipelineMetadata opseraPipelineMetadata) throws IOException {
        LOGGER.debug("Starting to get Tool Config Details for request {}", opseraPipelineMetadata);
        RestTemplateHelper restTemplate = serviceFactory.getRestTemplate();
        String toolsConfigURL = appConfig.getPipelineConfigBaseUrl() + PIPELINE_TABLE_ENDPOINT;
        String response = restTemplate.postForEntity(String.class, toolsConfigURL, opseraPipelineMetadata);
        return serviceFactory.getResponseParser().extractArgoToolConfig(response);
    }

    /**
     * Get the argo config.
     *
     * @param argoToolId the argo tool id
     * @param customerId the customer id
     * @return the argo details
     * @throws IOException 
     */
    public ArgoToolDetails getArgoDetails(String argoToolId, String customerId) throws IOException {
        LOGGER.debug("Starting to get Argo Tool Details for toolId {} and customerId {}", argoToolId, customerId);
        RestTemplateHelper restTemplate = serviceFactory.getRestTemplate();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(appConfig.getPipelineConfigBaseUrl() + TOOL_REGISTRY_ENDPOINT).queryParam(QUERY_PARM_TOOLID, argoToolId)
                .queryParam(QUERY_PARM_CUSTOMERID, customerId);
        String response = restTemplate.getForEntity(String.class, uriBuilder.toUriString());
        return serviceFactory.getResponseParser().extractArgoToolDetails(response);
    }

    /**
     * To call the tool details to get it from customer service.
     *
     * @param customerId the customer id
     * @param toolId     the tool id
     * @return the tools details
     * @throws ResourcesNotAvailable the resources not available
     */
    public ToolDetails getToolsDetails(String customerId, String toolId) throws ServiceException {
        LOGGER.debug("Starting to fetch Tool Details for toolId {} and customerId {}", toolId, customerId);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(appConfig.getCustomerBaseUrl()).path(String.format(GET_TOOL_DETAILS, customerId, toolId));
        try {
            ToolDetails responseEntity = serviceFactory.getRestTemplate().getForEntity(ToolDetails.class, uriBuilder.toUriString());
            Optional<ToolDetails> response = Optional.ofNullable(responseEntity);
            if (response.isPresent()) {
                return response.get();
            } else {
                LOGGER.warn("No Tools Details available for tool Id {}", toolId);
                throw new ServiceException(String.format("No Tools Details available for tool Id %s", toolId));
            }
        } catch (Exception ex) {
            LOGGER.warn("Exception while retrieving tools Details", ex);
            throw new ServiceException(String.format("No Tools Details available for tool Id %s", toolId));
        }
    }

    /**
     * Gets the tool details.
     *
     * @param toolConfigId the tool config id
     * @param customerId   the customer id
     * @return the tool details
     * @throws IOException 
     */
    public ToolDetails getToolDetails(String toolConfigId, String customerId) throws IOException {
        LOGGER.debug("Starting to get Tool Details for toolId {} and customerId {}", toolConfigId, customerId);
        RestTemplateHelper restTemplate = serviceFactory.getRestTemplate();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(appConfig.getPipelineConfigBaseUrl()).path(TOOL_REGISTRY_ENDPOINT).queryParam(QUERY_PARM_TOOLID, toolConfigId)
                .queryParam(QUERY_PARM_CUSTOMERID, customerId);
        return restTemplate.getForEntity(ToolDetails.class, uriBuilder.toUriString());
    }

    /**
     * Gets the AWSEKS cluster details.
     *
     * @param awsToolConfigId the aws tool config id
     * @param customerId      the customer id
     * @param clusterName     the cluster name
     * @return the AWSEKS cluster details
     * @throws IOException 
     */
    public AwsClusterDetails getAWSEKSClusterDetails(String awsToolConfigId, String customerId, String clusterName) throws IOException {
        LOGGER.debug("Starting to get Cluster Details for toolId {} and customerId {}", awsToolConfigId, customerId);
        RestTemplateHelper restTemplate = serviceFactory.getRestTemplate();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(appConfig.getAwsServiceBaseUrl() + AWS_EKS_CLUSTER_ENDPOINT + clusterName).queryParam(QUERY_PARM_AWS_TOOLID, awsToolConfigId)
                .queryParam(QUERY_PARM_CUSTOMERID, customerId);
        String response = restTemplate.getForEntity(String.class, uriBuilder.toUriString());
        return serviceFactory.getResponseParser().extractEKSClusterDetails(response);
    }

    /**
     * Gets the AKS cluster details.
     *
     * @param request the request
     * @return the AKS cluster details
     * @throws IOException 
     */
    public AzureClusterDetails getAKSClusterDetails(CreateCluster request) throws IOException {
        LOGGER.debug("Starting to get AKS Cluster Details request", request);
        RestTemplateHelper restTemplate = serviceFactory.getRestTemplate();
        String toolsConfigURL = appConfig.getAzureServiceBaseUrl() + CLUSTERS;
        AzureClusterDetails response = restTemplate.postForEntity(AzureClusterDetails.class, toolsConfigURL, request);
        return response;
    }

    /**
     * Gets the AWSEKS cluster token.
     *
     * @param awsToolConfigId the aws tool config id
     * @param customerId      the customer id
     * @param clusterName     the cluster name
     * @return the AWSEKS cluster token
     * @throws IOException 
     */
    public String getAWSEKSClusterToken(CreateCluster request) throws IOException {
        LOGGER.debug("Starting to get EKS Cluster Details for request {} ", request);
        RestTemplateHelper restTemplate = serviceFactory.getRestTemplate();
        String clusterConfigURL = appConfig.getAwsServiceBaseUrl() + AWS_STS_CLUSTER_TOKEN_ENDPOINT;
        return restTemplate.postForEntity(String.class, clusterConfigURL, request);
    }

    /**
     * Gets the bearer token.
     *
     * @param serverUrl  the server url
     * @param token      the token
     * @param argoToolId the argo tool id
     * @param nameSpace  the name space
     * @return the bearer token
     */
    public String getBearerToken(String serverUrl, String token, String argoToolId, String nameSpace) {
        LOGGER.debug("Starting to get bearer token for toolId {} and cluster {}", argoToolId, serverUrl);
        String serviceToken = "";
        try {
            ApiClient client = Config.fromToken(serverUrl, token, false);
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();
            V1Namespace v1Namespace = new V1Namespace();
            v1Namespace.setApiVersion(V1);
            v1Namespace.setKind("Namespace");
            V1ObjectMeta nameSpacemeta = new V1ObjectMeta();
            nameSpacemeta.setName(argoToolId);
            Map<String, String> map = new HashMap<>();
            map.put("name", argoToolId);
            nameSpacemeta.setLabels(map);
            v1Namespace.setMetadata(nameSpacemeta);

            V1NamespaceList v1NamespaceList = api.listNamespace(null, null, null, null, null, null, null, null, null, null);
            boolean isNamespaceExists = v1NamespaceList.getItems().stream().anyMatch(applicationMetadata -> applicationMetadata.getMetadata().getName().equals(argoToolId));
            if (!isNamespaceExists) {
                api.createNamespace(v1Namespace, null, null, null);
            }

            V1ServiceAccount body = new V1ServiceAccount();
            body.setApiVersion(V1);
            body.setKind(SERVICE_ACCOUNT);
            V1ObjectMeta meta = new V1ObjectMeta();
            meta.setName(argoToolId);
            body.setMetadata(meta);
            ApiResponse<V1ServiceAccountList> v1ServiceAccountList = api.listNamespacedServiceAccountWithHttpInfo(argoToolId, null, null, null, null, null, null, null, null, null, null);
            boolean isServiceAccountExists = v1ServiceAccountList.getData().getItems().stream().anyMatch(applicationMetadata -> applicationMetadata.getMetadata().getName().equals(argoToolId));
            if (isServiceAccountExists) {
                api.deleteNamespacedServiceAccount(argoToolId, argoToolId, null, null, null, null, null, null);
            }
            api.createNamespacedServiceAccount(argoToolId, body, null, null, null);
            v1ServiceAccountList = api.listNamespacedServiceAccountWithHttpInfo(argoToolId, null, null, null, null, null, null, null, null, null, null);
            V1ClusterRoleBinding v1ClusterRoleBinding = new V1ClusterRoleBinding();
            v1ClusterRoleBinding.setApiVersion(API_VERSION);
            v1ClusterRoleBinding.setKind(CLUSTER_ROLE_BINDING);
            V1ObjectMeta v1ClusterRoleBindingMeta = new V1ObjectMeta();
            v1ClusterRoleBindingMeta.setName(READ_SECRETS_GLOBAL);
            v1ClusterRoleBinding.setMetadata(meta);
            V1Subject v1Subject = new V1Subject();
            v1Subject.setKind(SERVICE_ACCOUNT);
            v1Subject.setName(argoToolId);
            v1Subject.setNamespace(argoToolId);
            v1ClusterRoleBinding.setSubjects(Arrays.asList(v1Subject));
            V1RoleRef roleRef = new V1RoleRef();
            roleRef.setKind(CLUSTER_ROLE);
            roleRef.setName(CLUSTER_ADMIN);
            roleRef.apiGroup(API_GROUP);
            v1ClusterRoleBinding.setRoleRef(roleRef);

            RbacAuthorizationV1Api rbacAuthorizationV1Api = new RbacAuthorizationV1Api(client);
            V1ClusterRoleBindingList clusterRoleBindingList = rbacAuthorizationV1Api.listClusterRoleBinding(null, null, null, null, null, null, null, null, null, null);
            boolean isClusterRoleBindingExists = clusterRoleBindingList.getItems().stream().anyMatch(applicationMetadata -> applicationMetadata.getMetadata().getName().equals(argoToolId));
            if (isClusterRoleBindingExists) {
                rbacAuthorizationV1Api.deleteClusterRoleBinding(argoToolId, null, null, null, null, null, null);
            }
            rbacAuthorizationV1Api.createClusterRoleBinding(v1ClusterRoleBinding, null, null, null);
            V1SecretList v1SecretList = api.listNamespacedSecret(argoToolId, null, null, null, null, null, null, null, null, null, null);
            for (V1Secret item : v1SecretList.getItems()) {
                if (null != item.getMetadata().getAnnotations() && item.getMetadata().getAnnotations().containsKey(K8_SERVCE_ACCOUNT_NAME)
                        && meta.getName().equalsIgnoreCase(item.getMetadata().getAnnotations().get(K8_SERVCE_ACCOUNT_NAME))) {
                    System.out.println(new String(item.getData().get(TOKEN)));
                    serviceToken = new String(item.getData().get(TOKEN));
                }
            }
        } catch (ApiException ex) {
            LOGGER.error("A problem occurred while creating service account and cluster role binding to get aws eks cluster token for service account {} and cluster {}", argoToolId, serverUrl);
            processException(ex);
        }
        LOGGER.debug("Completed to get bearer token for toolId {} and cluster {}", argoToolId, serverUrl);
        return serviceToken;
    }

    /**
     * Process exception.
     *
     * @param ex the ex
     */
    private void processException(ApiException ex) {
        Map<String, Object> map = new HashMap<>();
        V1Status status = serviceFactory.gson().fromJson(ex.getResponseBody(), V1Status.class);
        try {
            map = new ObjectMapper().readValue(ex.getResponseBody(), Map.class);
        } catch (Exception e) {
            throw new ServiceException(ex.getMessage());
        }
        if (null != map.get("message")) {
            if (HttpStatus.UNAUTHORIZED.name().equalsIgnoreCase(map.get("message").toString())) {
                throw new ServiceException("Invalid platform credentials or user not authorized to perform this action");
            }
        }
        throw new ServiceException(status.getMessage());
    }
}
