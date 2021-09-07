package com.opsera.integrator.argo.resources;

import java.util.List;

import lombok.Data;

@Data
public class ArgoProjectSpec {

    private String description;

    private List<String> sourceRepos;

    private List<ArgoApplicationDestination> destinations;

    private List<ArgoProjectClusterResourceWhiteList> clusterResourceWhitelist;

    private List<ArgoProjectNamespaceResourceBlacklist> namespaceResourceBlacklist;

    private List<ArgoProjectNamespaceResourceWhitelist> namespaceResourceWhitelist;

    private ArgoOrphanedResources orphanedResources;

}
