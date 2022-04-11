package com.opsera.integrator.argo.resources;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    public static final String PIPELINE_TABLE_ENDPOINT = "/tools/configuration";
    public static final String ARGO_SESSION_TOKEN_URL = "%s/api/v1/session";
    public static final String ALL_ARGO_APPLICATION_URL_TEMPLATE = "%s/api/v1/applications/%s";
    public static final String ARGO_APPLICATION_URL_TEMPLATE = "%s/api/v1/applications";
    public static final String ARGO_ALL_CLUSTER_URL_TEMPLATE = "%s/api/v1/clusters";
    public static final String ARGO_ALL_PROJECT_URL_TEMPLATE = "%s/api/v1/projects";
    public static final String ARGO_PROJECT_URL_TEMPLATE = "%s/api/v1/projects/%s";
    public static final String ARGO_SYNC_APPLICATION_URL_TEMPLATE = "%s/api/v1/applications/%s/sync";
    public static final String ARGO_SYNC_APPLICATION_OPERATION_URL_TEMPLATE = "%s/api/v1/applications/%s?operation=true";
    public static final String ARGO_APPLICATION_LOG_URL_TEMPLATE = "%s/api/v1/applications/%s/pods/%s/logs?namespace=%s";
    public static final String ARGO_APPLICATION_RESOURCE_TREE_URL_TEMPLATE = "%s/api/v1/applications/%s/resource-tree";
    public static final String ARGO_CREATE_APPLICATION_URL_TEMPLATE = "%s/api/v1/applications";
    public static final String ARGO_GET_USER_INFO_TEMPLATE = "%s/api/v1/session/userinfo";
    public static final String ARGO_VAULT_KEY_TEMPLATE = "%s-argo";
    public static final String TOOL_REGISTRY_ENDPOINT = "/v2/registry/tool";
    public static final String VAULT_READ_ENDPOINT = "/read";
    public static final String HTTP_EMPTY_BODY = "{}";
    public static final String HTTP_HEADER_ACCEPT = "Accept";
    public static final String QUERY_PARM_TOOLID = "toolId";
    public static final String QUERY_PARM_CUSTOMERID = "customerId";
    public static final String GET_TOOL_DETAILS = "/tooldetails/%s/%s";
    public static final String ARGO_REPOSITORY_URL_TEMPLATE = "%s/api/v1/repositories/%s";
    public static final String ALL_ARGO_REPOSITORY_URL_TEMPLATE = "%s/api/v1/repositories";
    public static final String VAULT_READ = "/read";
    public static final String FAILED = "Failed";
    public static final String QUERY_PARM_AWS_TOOLID = "awsToolConfigId";
    public static final String AWS_EKS_CLUSTER_ENDPOINT = "/eks/clusters/";
    public static final String AWS_STS_CLUSTER_TOKEN_ENDPOINT = "/sts/eksToken";
    public static final String AWS = "AWS";
    public static final String AZURE = "AZURE";
    public static final String ARGO_CLUSTER_URL_TEMPLATE = "%s/api/v1/clusters/%s";
    public static final String CLUSTERS = "/clusters";
    public static final String ASTERISK ="*";
    public static final String V1 = "v1";
    public static final String SERVICE_ACCOUNT = "ServiceAccount";
    public static final String CLUSTER_ROLE_BINDING = "ClusterRoleBinding";
    public static final String READ_SECRETS_GLOBAL = "read-secrets-global";
    public static final String CLUSTER_ROLE = "ClusterRole";
    public static final String CLUSTER_ADMIN = "cluster-admin";
    public static final String API_GROUP = "rbac.authorization.k8s.io";
    public static final String API_VERSION = "rbac.authorization.k8s.io/v1";
    public static final String K8_SERVCE_ACCOUNT_NAME = "kubernetes.io/service-account.name";
    public static final String TOKEN = "token";
    public static final String NAMESPACE_OPSERA = "opsera";
    public static final String AMAZON_AWS = "amazonaws";
    public static final String AZURE_K8S= "azmk8s";
    public static final String OPSERA_USER = "opsera_user";
    public static final String AZURE_DEVOPS_TOOL_IDENTIFIER = "azure-devops";
    public static final String SUCCESS = "Success";
    public static final String COMPLETED = "Completed";
    public static final String RUNNING = "Running";
    public static final String OPSERA_PIPELINE_ARGO_REQUEST = "opsera.pipeline.argo.request";
    public static final String RUN_COUNT_BY_PIPELINE_V2 = "/v2/pipeline/runcount";
    public static final String QUERY_PARM_PIPELINE_ID = "pipelineId";
    public static final String ARGO_SYNC_FAILED = "Argo Application Sync Failed";
    public static final String ARGO_SYNC_CONSOLE_FAILED = "Argo pod logs retrieved only for successfully synced applications.";
}