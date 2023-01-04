package com.opsera.integrator.argo.services;

import static com.opsera.integrator.argo.resources.Constants.ARM_CLIENT_ID;
import static com.opsera.integrator.argo.resources.Constants.ARM_CLIENT_SECRET;
import static com.opsera.integrator.argo.resources.Constants.ARM_RESOURCE_GROUP_ID;
import static com.opsera.integrator.argo.resources.Constants.ARM_SUBSCRIPTION_ID;
import static com.opsera.integrator.argo.resources.Constants.ARM_TENANT_ID;
import static com.opsera.integrator.argo.resources.Constants.ASTERISK;
import static com.opsera.integrator.argo.resources.Constants.AWS;
import static com.opsera.integrator.argo.resources.Constants.AWS_ACCESS_KEY_ID;
import static com.opsera.integrator.argo.resources.Constants.AWS_DEFAULT_REGION;
import static com.opsera.integrator.argo.resources.Constants.AWS_SECRET_ACCESS_KEY;
import static com.opsera.integrator.argo.resources.Constants.AWS_SESSION_TOKEN;
import static com.opsera.integrator.argo.resources.Constants.AZURE;
import static com.opsera.integrator.argo.resources.Constants.AZURE_DEVOPS_TOOL_IDENTIFIER;
import static com.opsera.integrator.argo.resources.Constants.CLUSTER_NAME;
import static com.opsera.integrator.argo.resources.Constants.CREATE_NAMESPACE_FLAG;
import static com.opsera.integrator.argo.resources.Constants.CUSTOMER_CLUSTER_INFO_MISSING;
import static com.opsera.integrator.argo.resources.Constants.NAMESPACE_OPSERA;
import static com.opsera.integrator.argo.resources.Constants.OPSERA_USER;
import static com.opsera.integrator.argo.resources.Constants.V1;
import static com.opsera.integrator.argo.resources.Constants.VAULT_CLUSTER_TOKEN;
import static com.opsera.integrator.argo.resources.Constants.VAULT_CLUSTER_URL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.opsera.core.exception.ServiceException;
import com.opsera.core.helper.ToolConfigurationHelper;
import com.opsera.core.helper.VaultHelper;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.exceptions.ArgoServiceException;
import com.opsera.integrator.argo.exceptions.ResourcesNotAvailable;
import com.opsera.integrator.argo.resources.ArgoApplicationDestination;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationMetadata;
import com.opsera.integrator.argo.resources.ArgoApplicationSource;
import com.opsera.integrator.argo.resources.ArgoApplicationSpec;
import com.opsera.integrator.argo.resources.ArgoClusterConfig;
import com.opsera.integrator.argo.resources.ArgoProjectClusterResourceWhiteList;
import com.opsera.integrator.argo.resources.ArgoProjectMetadata;
import com.opsera.integrator.argo.resources.ArgoProjectNamespaceResourceBlacklist;
import com.opsera.integrator.argo.resources.ArgoProjectNamespaceResourceWhitelist;
import com.opsera.integrator.argo.resources.ArgoRepositoryItem;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.Automated;
import com.opsera.integrator.argo.resources.AwsClusterDetails;
import com.opsera.integrator.argo.resources.AwsDetails;
import com.opsera.integrator.argo.resources.AzureClusterDetails;
import com.opsera.integrator.argo.resources.CreateApplicationRequest;
import com.opsera.integrator.argo.resources.CreateCluster;
import com.opsera.integrator.argo.resources.CreateClusterRequest;
import com.opsera.integrator.argo.resources.CreateProjectRequest;
import com.opsera.integrator.argo.resources.CreateRepositoryRequest;
import com.opsera.integrator.argo.resources.Directory;
import com.opsera.integrator.argo.resources.Helm;
import com.opsera.integrator.argo.resources.Kustomize;
import com.opsera.integrator.argo.resources.Project;
import com.opsera.integrator.argo.resources.SyncPolicy;
import com.opsera.integrator.argo.resources.TLSClientConfig;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.integrator.argo.resources.ToolDetails;
import com.opsera.kubernetes.helper.KubernetesPodHandler;
import com.opsera.kubernetes.helper.exception.KubernetesHelperException;
import com.opsera.kubernetes.helper.listener.KubernetesLogListener;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Config;

/**
 * The Class RequestBuilder.
 */
@Service
public class RequestBuilder {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(RequestBuilder.class);

    /** The service factory. */
    @Autowired
    private IServiceFactory serviceFactory;

    @Autowired
    private ToolConfigurationHelper toolConfigurationHelper;

    @Autowired
    private VaultHelper vaultHelper;

    /**
     * Creates the application request.
     *
     * @param request the request
     * @return the argo application item
     */
    public ArgoApplicationItem createApplicationRequest(CreateApplicationRequest request) {
        LOGGER.debug("Starting to create Argo Application Request {}", request);
        ArgoApplicationItem argoApplication = new ArgoApplicationItem();
        ArgoApplicationMetadata metadata = new ArgoApplicationMetadata();
        metadata.setName(request.getApplicationName());
        ArgoApplicationSpec spec = new ArgoApplicationSpec();
        ArgoApplicationSource source = new ArgoApplicationSource();
        setApplicationTypeAttributes(request, source);
        source.setRepoURL(request.getGitUrl());
        source.setPath(request.getGitPath());
        source.setTargetRevision(request.getBranchName());
        ArgoApplicationDestination destination = new ArgoApplicationDestination();
        destination.setNamespace(request.getNamespace());
        destination.setServer(request.getCluster());
        spec.setSource(source);      
        spec.setDestination(destination);
        spec.setProject(request.getProjectName());
        SyncPolicy syncPolicy = new SyncPolicy();
        if (request.isAutoSync()) {
            Automated automated = new Automated();
            automated.setPrune(true);
            automated.setSelfHeal(true);
            syncPolicy.setAutomated(automated);
        }
        List<String> syncOptions = new ArrayList<>();
        syncOptions.add(CREATE_NAMESPACE_FLAG);
        syncPolicy.setSyncOptions(syncOptions);
        spec.setSyncPolicy(syncPolicy);
        argoApplication.setMetadata(metadata);
        argoApplication.setSpec(spec);
        return argoApplication;
    }

    private void setApplicationTypeAttributes(CreateApplicationRequest request, ArgoApplicationSource source) {
        if (request.getType().equalsIgnoreCase("Helm")) {
            setHelmTypeSourceAttributes(request, source);
        } else if (request.getType().equalsIgnoreCase("Kustomize")) {
            setKustomizeSourceAttributes(request, source);
        } else {
            if (request.isRecursive()) {
                Directory directory = new Directory();
                directory.setRecurse(true);
                source.setDirectory(directory);
            }
        }
    }

    private void setKustomizeSourceAttributes(CreateApplicationRequest request, ArgoApplicationSource source) {
        if (StringUtils.hasText(request.getNamePrefix()) || StringUtils.hasText(request.getNameSuffix())) {
            Kustomize kustomize = new Kustomize();
            if (StringUtils.hasText(request.getNamePrefix())) {
                kustomize.setNamePrefix(request.getNamePrefix());  
            }
            if(StringUtils.hasText(request.getNameSuffix())) {
                kustomize.setNameSuffix(request.getNameSuffix());
            }
            source.setKustomize(kustomize);
        }
    }

    private void setHelmTypeSourceAttributes(CreateApplicationRequest request, ArgoApplicationSource source) {
        if (StringUtils.hasText(request.getValues()) || !CollectionUtils.isEmpty(request.getValueFiles())) {
            Helm helm = new Helm();
            if (!CollectionUtils.isEmpty(request.getValueFiles())) {
                helm.setValueFiles(request.getValueFiles());
            }
            if (StringUtils.hasText(request.getValues())) {
                helm.setValues(request.getValues());
            }
            source.setHelm(helm);
        } else {
            throw new ServiceException("Values and Values Files cannot be Empty for the type Helm");
        }
    }

    /**
     * Creates the repository request.
     *
     * @param request    the request
     * @param toolConfig the tool config
     * @param secret     the secret
     * @return the argo repository item
     */
    public ArgoRepositoryItem createRepositoryRequest(CreateRepositoryRequest request, ToolDetails toolDetails, String secret) {
        LOGGER.debug("Starting to create Argo Repository Request {}", request);
        ArgoRepositoryItem argoRepositoryItem = new ArgoRepositoryItem();
        argoRepositoryItem.setName(request.getRepositoryName());
        argoRepositoryItem.setType(request.getRepositoryType());
        if (StringUtils.hasText(request.getProjectName())) {
            argoRepositoryItem.setProject(request.getProjectName());
        }
        ToolConfig toolConfig = toolDetails.getConfiguration();
        if (toolConfig.isTwoFactorAuthentication()) {
            argoRepositoryItem.setRepo(request.getSshUrl());
            argoRepositoryItem.setSshPrivateKey(secret);
        } else {
            argoRepositoryItem.setRepo(request.getHttpsUrl());
            if (AZURE_DEVOPS_TOOL_IDENTIFIER.equalsIgnoreCase(toolDetails.getToolIdentifier()) && !StringUtils.hasText(toolConfig.getAccountUsername()))
                argoRepositoryItem.setUsername(OPSERA_USER);
            else
                argoRepositoryItem.setUsername(toolConfig.getAccountUsername());
            argoRepositoryItem.setPassword(secret);
        }
        return argoRepositoryItem;
    }

    /**
     * Update project request.
     *
     * @param projectItem the project item
     * @param request     the request
     * @return the creates the project request
     */
    public CreateProjectRequest updateProjectRequest(ArgoApplicationItem projectItem, CreateProjectRequest request) {
        LOGGER.debug("Starting to update Argo Project Request {}", request);
        CreateProjectRequest createProjectRequest = new CreateProjectRequest();
        Project project = new Project();
        ArgoProjectMetadata metaData = new ArgoProjectMetadata();
        metaData.setName(projectItem.getMetadata().getName());
        metaData.setResourceVersion(projectItem.getMetadata().getResourceVersion());
        project.setMetadata(metaData);
        project.setSpec(request.getProject().getSpec());
        createProjectRequest.setProject(project);
        return createProjectRequest;
    }

    /**
     * Creates the cluster request.
     *
     * @param request the request
     * @return the creates the cluster request
     */
    public CreateClusterRequest createClusterRequest(CreateCluster request) {
        LOGGER.debug("Starting to create Argo cluster Request {}", request);
        AwsClusterDetails awsClusterDetails;
        AzureClusterDetails azureClusterDetails;
        CreateClusterRequest createClusterRequest = new CreateClusterRequest();
        TLSClientConfig tlsClientConfig = new TLSClientConfig();
        ArgoClusterConfig argoClusterConfig = new ArgoClusterConfig();
        if (AWS.equalsIgnoreCase(request.getPlatform().toUpperCase())) {
            awsClusterDetails = serviceFactory.getConfigCollector().getAWSEKSClusterDetails(request.getPlatformToolId(), request.getCustomerId(), request.getClusterName());
            createClusterRequest.setServer(awsClusterDetails.getCluster().getEndpoint());
            createClusterRequest.setName(awsClusterDetails.getCluster().getName());
            tlsClientConfig.setCaData(awsClusterDetails.getCluster().getCertificateAuthority().getData());
            argoClusterConfig.setTlsClientConfig(tlsClientConfig);
            String eksToken = serviceFactory.getConfigCollector().getAWSEKSClusterToken(request);
            String bearerToken = serviceFactory.getConfigCollector().getBearerToken(createClusterRequest.getServer(), eksToken, request.getArgoToolId(), NAMESPACE_OPSERA);
            argoClusterConfig.setBearerToken(bearerToken);
            createClusterRequest.setConfig(argoClusterConfig);
        } else if (AZURE.equalsIgnoreCase(request.getPlatform().toUpperCase())) {
            azureClusterDetails = serviceFactory.getConfigCollector().getAKSClusterDetails(request);
            createClusterRequest.setServer(azureClusterDetails.getServer());
            createClusterRequest.setName(azureClusterDetails.getName());
            tlsClientConfig.setCaData(azureClusterDetails.getCaData());
            tlsClientConfig.setCertData(azureClusterDetails.getCertData());
            tlsClientConfig.setKeyData(azureClusterDetails.getKeyData());
            argoClusterConfig.setTlsClientConfig(tlsClientConfig);
            argoClusterConfig.setBearerToken(azureClusterDetails.getBearerToken());
            createClusterRequest.setConfig(argoClusterConfig);
        }
        return createClusterRequest;
    }

    public void createNamespace(CreateCluster request) {
        try {
            ApiClient client = null;
            if (AWS.equalsIgnoreCase(request.getPlatform().toUpperCase())) {
                AwsClusterDetails awsClusterDetails = serviceFactory.getConfigCollector().getAWSEKSClusterDetails(request.getPlatformToolId(), request.getCustomerId(), request.getClusterName());
                String eksToken = serviceFactory.getConfigCollector().getAWSEKSClusterToken(request);
                client = Config.fromToken(awsClusterDetails.getCluster().getEndpoint(), eksToken, false);
            } else if (AZURE.equalsIgnoreCase(request.getPlatform().toUpperCase())) {
                AzureClusterDetails azureClusterDetails = serviceFactory.getConfigCollector().getAKSClusterDetails(request);
                client = Config.fromToken(azureClusterDetails.getServer(), azureClusterDetails.getBearerToken(), false);
            }
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();
            V1Namespace v1Namespace = new V1Namespace();
            v1Namespace.setApiVersion(V1);
            v1Namespace.setKind("Namespace");
            V1ObjectMeta nameSpacemeta = new V1ObjectMeta();
            nameSpacemeta.setName(request.getNamespace());
            v1Namespace.setMetadata(nameSpacemeta);
            V1NamespaceList v1NamespaceList = api.listNamespace(null, null, null, null, null, null, null, null, null, null);
            boolean isNamespaceExists = v1NamespaceList.getItems().stream().anyMatch(applicationMetadata -> applicationMetadata.getMetadata().getName().equals(request.getNamespace()));
            if (!isNamespaceExists) {
                api.createNamespace(v1Namespace, null, null, null);
            }
        } catch (Exception e) {
            throw new ArgoServiceException(String.format("Exception occured while creating namespace. message: %s", e.getMessage()));
        }
    }

    /**
     * Creates the project request.
     *
     * @param request the request
     */
    public void createProjectRequest(CreateProjectRequest request) {
        LOGGER.debug("Starting to Build Request for Argo Project {}", request);
        List<ArgoProjectClusterResourceWhiteList> clusterResourceWhitelist = setClusterResourceWhiteList(request);
        request.getProject().getSpec().setClusterResourceWhitelist(clusterResourceWhitelist);
        List<ArgoProjectNamespaceResourceBlacklist> namespaceResourceBlacklist = setNamespaceResourceBlacklist(request);
        request.getProject().getSpec().setNamespaceResourceBlacklist(namespaceResourceBlacklist);
        List<ArgoProjectNamespaceResourceWhitelist> namespaceResourceWhitelist = setNamespaceResourceWhitelist(request);
        request.getProject().getSpec().setNamespaceResourceWhitelist(namespaceResourceWhitelist);
        LOGGER.debug("Completed to Build Request for Argo Project {}", request);
    }

    /**
     * Sets the cluster resource white list.
     *
     * @param request the request
     * @return the list
     */
    private List<ArgoProjectClusterResourceWhiteList> setClusterResourceWhiteList(CreateProjectRequest request) {
        LOGGER.debug("Starting to set Cluster Resource WhiteList for Argo Project {}", request);
        List<ArgoProjectClusterResourceWhiteList> clusterResourceWhitelist = new ArrayList<>();
        ArgoProjectClusterResourceWhiteList clusterResourceWhite;
        if (request.getProject().getSpec().getClusterResourceWhitelist() != null) {
            for (ArgoProjectClusterResourceWhiteList clusterResource : request.getProject().getSpec().getClusterResourceWhitelist()) {
                clusterResourceWhite = new ArgoProjectClusterResourceWhiteList();
                if (clusterResource.getGroup().isEmpty()) {
                    clusterResourceWhite.setGroup(ASTERISK);
                } else {
                    clusterResourceWhite.setGroup(clusterResource.getGroup());
                }
                if (clusterResource.getKind().isEmpty()) {
                    clusterResourceWhite.setKind(ASTERISK);
                } else {
                    clusterResourceWhite.setKind(clusterResource.getKind());
                }
                clusterResourceWhitelist.add(clusterResourceWhite);
            }
        } else {
            clusterResourceWhite = new ArgoProjectClusterResourceWhiteList();
            clusterResourceWhite.setGroup(ASTERISK);
            clusterResourceWhite.setKind(ASTERISK);
            clusterResourceWhitelist.add(clusterResourceWhite);
        }
        return clusterResourceWhitelist;
    }

    /**
     * Sets the namespace resource blacklist.
     *
     * @param request the request
     * @return the list
     */
    private List<ArgoProjectNamespaceResourceBlacklist> setNamespaceResourceBlacklist(CreateProjectRequest request) {
        LOGGER.debug("Starting to set Namespace Resource Blacklist for Argo Project {}", request);
        List<ArgoProjectNamespaceResourceBlacklist> namespaceResourceBlacklist = new ArrayList<>();
        ArgoProjectNamespaceResourceBlacklist namespaceResourceBlack;
        if (request.getProject().getSpec().getNamespaceResourceBlacklist() != null) {
            for (ArgoProjectNamespaceResourceBlacklist namespaceResource : request.getProject().getSpec().getNamespaceResourceBlacklist()) {
                namespaceResourceBlack = new ArgoProjectNamespaceResourceBlacklist();
                if (namespaceResource.getGroup().isEmpty()) {
                    namespaceResourceBlack.setGroup(ASTERISK);
                } else {
                    namespaceResourceBlack.setGroup(namespaceResource.getGroup());
                }
                if (namespaceResource.getKind().isEmpty()) {
                    namespaceResourceBlack.setKind(ASTERISK);
                } else {
                    namespaceResourceBlack.setKind(namespaceResource.getKind());
                }
                namespaceResourceBlacklist.add(namespaceResourceBlack);
            }
        } else {
            namespaceResourceBlack = new ArgoProjectNamespaceResourceBlacklist();
            namespaceResourceBlack.setGroup(ASTERISK);
            namespaceResourceBlack.setKind(ASTERISK);
            namespaceResourceBlacklist.add(namespaceResourceBlack);
        }
        return namespaceResourceBlacklist;
    }

    /**
     * Sets the namespace resource whitelist.
     *
     * @param request the request
     * @return the list
     */
    private List<ArgoProjectNamespaceResourceWhitelist> setNamespaceResourceWhitelist(CreateProjectRequest request) {
        LOGGER.debug("Starting to set Namespace Resource Whitelist for Argo Project {}", request);
        List<ArgoProjectNamespaceResourceWhitelist> namespaceResourceWhitelist = new ArrayList<>();
        ArgoProjectNamespaceResourceWhitelist namespaceResourceWhite;
        if (request.getProject().getSpec().getNamespaceResourceWhitelist() != null) {
            for (ArgoProjectNamespaceResourceWhitelist projectNamespaceResource : request.getProject().getSpec().getNamespaceResourceWhitelist()) {
                namespaceResourceWhite = new ArgoProjectNamespaceResourceWhitelist();
                if (projectNamespaceResource.getGroup().isEmpty()) {
                    namespaceResourceWhite.setGroup(ASTERISK);
                } else {
                    namespaceResourceWhite.setGroup(projectNamespaceResource.getGroup());
                }
                if (projectNamespaceResource.getKind().isEmpty()) {
                    namespaceResourceWhite.setKind(ASTERISK);
                } else {
                    namespaceResourceWhite.setKind(projectNamespaceResource.getKind());
                }
                namespaceResourceWhitelist.add(namespaceResourceWhite);
            }
        } else {
            namespaceResourceWhite = new ArgoProjectNamespaceResourceWhitelist();
            namespaceResourceWhite.setGroup(ASTERISK);
            namespaceResourceWhite.setKind(ASTERISK);
            namespaceResourceWhitelist.add(namespaceResourceWhite);
        }
        return namespaceResourceWhitelist;
    }

    public void execKubectlOnPod(CreateCluster request) throws ResourcesNotAvailable, IOException {
        String parentId = toolConfigurationHelper.getParentId(request.getCustomerId());
        Map<String, String> vaultData = vaultHelper.getSecrets(parentId, Arrays.asList(VAULT_CLUSTER_URL, VAULT_CLUSTER_TOKEN), null);
        String url = vaultData.get(VAULT_CLUSTER_URL);
        String token = vaultData.get(VAULT_CLUSTER_TOKEN);
        if (!StringUtils.hasText(url) || !StringUtils.hasText(token))
            throw new ResourcesNotAvailable(CUSTOMER_CLUSTER_INFO_MISSING);
        LOGGER.info("Successfully fetched the customer cluster information");
        ArgoToolDetails config = toolConfigurationHelper.getToolConfig(parentId, request.getPlatformToolId(), ArgoToolDetails.class);
        Map<String, String> envVar = new HashMap<>();
        List<String> commands = getCommands(config, request, envVar);
        LOGGER.info("commands: \n {}", commands);
        LOGGER.info("Starting to create kubernetes pod on the customer data plane");
        CompletableFuture.runAsync(() -> {
            try {
                processKubctlPodHandler(request, url, token, envVar, commands);
            } catch (KubernetesHelperException e) {
                e.printStackTrace();
            }
        });
    }

    private void processKubctlPodHandler(CreateCluster request, String url, String token, Map<String, String> envVar, List<String> commands) throws KubernetesHelperException {
        KubernetesPodHandler handler = new KubernetesPodHandler(url, token, request.getClusterName(), request.getArgoToolId(), 0);
        try {
            if (AWS.equalsIgnoreCase(request.getPlatform())) {
                handler.createJob("ubuntu", commands, envVar);
            } else if (AZURE.equalsIgnoreCase(request.getPlatform())) {
                handler.createJob("mcr.microsoft.com/azure-cli:latest", commands, envVar);
            }
            String logs = podLogs(handler);
            LOGGER.debug("Argo Rollouts pod logs: \n{}", logs);
        } catch (Exception e) {
            handler.terminatePod();
            throw new ArgoServiceException(String.format("Exception occured while argo rollouts controller installation. message: %s", e.getMessage()));
        }
    }

    private String podLogs(KubernetesPodHandler handler) throws KubernetesHelperException {
        KubernetesLogListener listener = new KubernetesLogListener() {
            @Override
            public void processCompleted(String arg0, int arg1, String arg2) {
            }

            @Override
            public void onLogData(String arg0) {
            }
        };
        return handler.streamJobLogs(listener, 1);
    }

    private List<String> getCommands(ArgoToolDetails config, CreateCluster request, Map<String, String> envVar) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add("/bin/bash");
        commands.add("-exc");
        if (AWS.equalsIgnoreCase(request.getPlatform())) {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("aws_rollout_script.txt");
            String baseScript = IOUtils.toString(inputStream, Charset.defaultCharset());
            StringBuilder command = new StringBuilder(baseScript).append(System.lineSeparator());
            getAwsDetails(config, request, envVar, command);
            commands.add(command.toString());
        } else if (AZURE.equalsIgnoreCase(request.getPlatform())) {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("azure_rollout_script.txt");
            String baseScript = IOUtils.toString(inputStream, Charset.defaultCharset());
            StringBuilder command = new StringBuilder(baseScript).append(System.lineSeparator());
            getAzureDetails(config, request, envVar, command);
            commands.add(command.toString());
        }
        LOGGER.info("Successfully to construct the rollout commands");
        return commands;
    }

    private void getAwsDetails(ArgoToolDetails config, CreateCluster request, Map<String, String> envVar, StringBuilder command) {
        ToolConfig configuration = config.getConfiguration();
        String secretKey = configuration.getSecretKey().getVaultKey();
        String accessKey = configuration.getAccessKey().getVaultKey();
        List<String> vaultKey = Arrays.asList(accessKey, secretKey);
        Map<String, String> secrects = vaultHelper.getSecrets(config.getOwner(), vaultKey, null);
        AwsDetails awsDetails = new AwsDetails();
        if (request.isIamRoleFlag()) {
            awsDetails.setCustomerId(config.getOwner());
            awsDetails.setToolId(request.getPlatformToolId());
            awsDetails.setRoleArn(request.getRoleArn());
            awsDetails.setRoleSessionName(config.getOwner());
            awsDetails = serviceFactory.getAwsServiceHelper().getCredentials(awsDetails);
            awsDetails.setRegion(configuration.getRegions());
        } else {
            awsDetails.setAccessKeyId(secrects.get(accessKey));
            awsDetails.setSecretAccessKey(secrects.get(secretKey));
            awsDetails.setRegion(configuration.getRegions());
        }
        if (StringUtils.hasText(awsDetails.getAccessKeyId())) {
            envVar.put(AWS_ACCESS_KEY_ID, awsDetails.getAccessKeyId());
        }
        if (StringUtils.hasText(awsDetails.getSecretAccessKey())) {
            envVar.put(AWS_SECRET_ACCESS_KEY, awsDetails.getSecretAccessKey());
        }
        if (StringUtils.hasText(awsDetails.getRegion())) {
            envVar.put(AWS_DEFAULT_REGION, awsDetails.getRegion());
        }
        if (StringUtils.hasText(awsDetails.getSessionToken())) {
            envVar.put(AWS_SESSION_TOKEN, awsDetails.getSessionToken());
        }
        if (StringUtils.hasText(request.getClusterName())) {
            envVar.put(CLUSTER_NAME, request.getClusterName());
        }
    }

    private void getAzureDetails(ArgoToolDetails config, CreateCluster request, Map<String, String> envVar, StringBuilder command) {
        ToolConfig configuration = config.getConfiguration();
        String subscriptionIdKey = configuration.getAzureSubscriptionId();
        String tenantIdKey = configuration.getAzureTenantId();
        String applicationName = request.getClientId();
        String applicationPassword = request.getClientSecret();
        List<String> vaultKey = Arrays.asList(applicationName, applicationPassword);
        Map<String, String> secrects = vaultHelper.getSecrets(request.getCustomerId(), vaultKey, null);
        if (StringUtils.hasText(request.getClientId())) {
            envVar.put(ARM_CLIENT_ID, secrects.get(request.getClientId()));
        }
        if (StringUtils.hasText(request.getClientSecret())) {
            envVar.put(ARM_CLIENT_SECRET, secrects.get(request.getClientSecret()));
        }
        if (StringUtils.hasText(subscriptionIdKey)) {
            envVar.put(ARM_SUBSCRIPTION_ID, subscriptionIdKey);
        }
        if (StringUtils.hasText(tenantIdKey)) {
            envVar.put(ARM_TENANT_ID, tenantIdKey);
        }
        if (StringUtils.hasText(request.getResourceGroup())) {
            envVar.put(ARM_RESOURCE_GROUP_ID, request.getResourceGroup());
        }
        if (StringUtils.hasText(request.getClusterName())) {
            envVar.put(CLUSTER_NAME, request.getClusterName());
        }
    }
}
