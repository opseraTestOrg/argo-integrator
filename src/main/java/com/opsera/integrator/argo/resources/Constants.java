package com.opsera.integrator.argo.resources;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public static final String PIPELINE_TABLE_ENDPOINT = "/tools/configuration";
    public static final String ARGO_SESSION_TOKEN_URL = "https://aws-argocd.opsera.io/api/v1/session";
    public static final String ALL_ARGO_APPLICATION_URL_TEMPLATE = "https://aws-argocd.opsera.io/api/v1/applications/%s";
    public static final String ARGO_APPLICATION_URL = "https://aws-argocd.opsera.io/api/v1/applications";
    public static final String ARGO_SYNC_APPLICATION_URL_TEMPLATE = "https://aws-argocd.opsera.io/api/v1/applications/%s/sync";
    public static final String ARGO_VAULT_KEY_TEMPLATE = "%s-argo";
    public static final String TOOL_REGISTRY_ENDPOINT = "/registry/tool";
    public static final String VAULT_READ_ENDPOINT = "/read";
    public static final String HTTP_EMPTY_BODY = "{}";
    public static final String HTTP_HEADER_ACCEPT = "Accept";
    public static final String QUERY_PARM_TOOLID = "toolId";
    public static final String GET_PIPELINE_LOGS_URL = "%s/pipelines/%s";
    public static final String EMPTY_RESPONSE = "Empty response from spinnaker";
}
