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
import static com.opsera.integrator.argo.resources.Constants.QUERY_PARM_AWS_TOOLID;
import static com.opsera.integrator.argo.resources.Constants.QUERY_PARM_CUSTOMERID;
import static com.opsera.integrator.argo.resources.Constants.READ_SECRETS_GLOBAL;
import static com.opsera.integrator.argo.resources.Constants.SERVICE_ACCOUNT;
import static com.opsera.integrator.argo.resources.Constants.TOKEN;
import static com.opsera.integrator.argo.resources.Constants.V1;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.opsera.core.helper.VaultHelper;
import com.opsera.core.rest.RestTemplateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsera.integrator.argo.config.AppConfig;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.ArgoServiceException;
import com.opsera.integrator.argo.exceptions.ResourcesNotAvailable;
import com.opsera.integrator.argo.exceptions.UnAuthorizedException;
import com.opsera.integrator.argo.resources.AwsClusterDetails;
import com.opsera.integrator.argo.resources.AzureClusterDetails;
import com.opsera.integrator.argo.resources.CreateCluster;
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

    @Autowired
    private VaultHelper vaultHelper;

    @Autowired
    private RestTemplateHelper restTemplateHelper;

    /**
     * To call the tool details to get it from customer service.
     *
     * @param customerId the customer id
     * @param toolId     the tool id
     * @return the tools details
     * @throws ResourcesNotAvailable the resources not available
     */
    public ToolDetails getToolsDetails(String customerId, String toolId) throws ResourcesNotAvailable {
        LOGGER.debug("Starting to fetch Tool Details for toolId {} and customerId {}", toolId, customerId);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(appConfig.getCustomerBaseUrl()).path(String.format(GET_TOOL_DETAILS, customerId, toolId));
        try {
            ToolDetails responseEntity = restTemplateHelper.getForEntity(ToolDetails.class, uriBuilder.toUriString(), getRequestHeaders());
            Optional<ToolDetails> response = Optional.ofNullable(responseEntity);
            if (response.isPresent()) {
                return response.get();
            } else {
                LOGGER.warn("No Tools Details available for tool Id {}", toolId);
                throw new ResourcesNotAvailable(String.format("No Tools Details available for tool Id %s", toolId));
            }
        } catch (Exception ex) {
            LOGGER.warn("Exception while retrieving tools Details", ex);
            throw new ResourcesNotAvailable(String.format("No Tools Details available for tool Id %s", toolId));
        }
    }

    /**
     * This method used to construct http header.
     *
     * @return the request entity
     */
    private HttpHeaders getRequestHeaders() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return requestHeaders;
    }

    /**
     * Gets the AWSEKS cluster details.
     *
     * @param awsToolConfigId the aws tool config id
     * @param customerId      the customer id
     * @param clusterName     the cluster name
     * @return the AWSEKS cluster details
     */
    public AwsClusterDetails getAWSEKSClusterDetails(String awsToolConfigId, String customerId, String clusterName) {
        LOGGER.debug("Starting to get Cluster Details for toolId {} and customerId {}", awsToolConfigId, customerId);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(appConfig.getAwsServiceBaseUrl() + AWS_EKS_CLUSTER_ENDPOINT + clusterName).queryParam(QUERY_PARM_AWS_TOOLID, awsToolConfigId)
                .queryParam(QUERY_PARM_CUSTOMERID, customerId);
        String response = restTemplateHelper.getForEntity(String.class, uriBuilder.toUriString());
        return serviceFactory.getResponseParser().extractEKSClusterDetails(response);
    }

    /**
     * Gets the AKS cluster details.
     *
     * @param request the request
     * @return the AKS cluster details
     */
    public AzureClusterDetails getAKSClusterDetails(CreateCluster request) {
        LOGGER.debug("Starting to get AKS Cluster Details request {}", request);
        String toolsConfigURL = appConfig.getAzureServiceBaseUrl() + CLUSTERS;
        return restTemplateHelper.postForEntity(AzureClusterDetails.class, toolsConfigURL, request);
    }

    /**
     * Gets the AWSEKS cluster token.
     *
     * @param request
     * @return the AWSEKS cluster token
     */
    public String getAWSEKSClusterToken(CreateCluster request) {
        LOGGER.debug("Starting to get EKS Cluster Details for request {} ", request);
        String clusterConfigURL = appConfig.getAwsServiceBaseUrl() + AWS_STS_CLUSTER_TOKEN_ENDPOINT;
        return restTemplateHelper.postForEntity(String.class, clusterConfigURL, request);
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
    @SuppressWarnings("unchecked")
    private void processException(ApiException ex) {
        Map<String, Object> map = new HashMap<>();
        V1Status status = serviceFactory.gson().fromJson(ex.getResponseBody(), V1Status.class);
        try {
            map = new ObjectMapper().readValue(ex.getResponseBody(), Map.class);
        } catch (Exception e) {
            throw new ArgoServiceException(ex.getMessage());
        }
        if (null != map.get("message")) {
            if (HttpStatus.UNAUTHORIZED.name().equalsIgnoreCase(map.get("message").toString())) {
                throw new UnAuthorizedException("Invalid platform credentials or user not authorized to perform this action");
            }
        }
        throw new ArgoServiceException(status.getMessage(), status.getCode());
    }

}
