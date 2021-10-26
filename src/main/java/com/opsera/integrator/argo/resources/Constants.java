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
    public static final String ARGO_CREATE_APPLICATION_URL_TEMPLATE = "%s/api/v1/applications";
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
    public static final String AWS = "AWS";
    public static final String AZURE = "AZURE";
    public static final String ARGO_CLUSTER_URL_TEMPLATE = "%s/api/v1/clusters/%s";
    public static final String CLUSTERS = "/clusters";
}
