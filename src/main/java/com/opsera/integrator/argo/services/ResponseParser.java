package com.opsera.integrator.argo.services;

import java.lang.reflect.Type;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.reflect.TypeToken;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationsList;
import com.opsera.integrator.argo.resources.ArgoClusterItem;
import com.opsera.integrator.argo.resources.ArgoClusterList;
import com.opsera.integrator.argo.resources.ArgoRepositoriesList;
import com.opsera.integrator.argo.resources.ArgoRepositoryItem;
import com.opsera.integrator.argo.resources.ArgoToolDetails;
import com.opsera.integrator.argo.resources.AwsClusterDetails;
import com.opsera.integrator.argo.resources.Project;
import com.opsera.integrator.argo.resources.ProjectList;
import com.opsera.integrator.argo.resources.ToolConfig;

/**
 * Class to deserialize all the json responses from rest endpoints.
 */
@Component
public class ResponseParser {

    /** The service factory. */
    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * extractArgoToolDetails.
     *
     * @param response the response
     * @return the argo tool details
     */
    public ArgoToolDetails extractArgoToolDetails(String response) {
        Type type = new TypeToken<ArgoToolDetails>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * extractArgoToolConfig.
     *
     * @param response the response
     * @return the tool config
     */
    public ToolConfig extractArgoToolConfig(String response) {
        Type type = new TypeToken<ToolConfig>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * extractArgoApplicationItem.
     *
     * @param response the response
     * @return the argo application item
     */
    public ArgoApplicationItem extractArgoApplicationItem(String response) {
        Type type = new TypeToken<ArgoApplicationItem>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * extractArgoApplicationsList.
     *
     * @param response the response
     * @return the argo applications list
     */
    public ArgoApplicationsList extractArgoApplicationsList(String response) {
        Type type = new TypeToken<ArgoApplicationsList>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * extractArgoClustersList.
     *
     * @param response the response
     * @return the argo cluster list
     */
    public ArgoClusterList extractArgoClustersList(String response) {
        Type type = new TypeToken<ArgoClusterList>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * Extract argo repository item.
     *
     * @param response the response
     * @return the argo repository item
     */
    public ArgoRepositoryItem extractArgoRepositoryItem(String response) {
        Type type = new TypeToken<ArgoRepositoryItem>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * Extract argo repositories list.
     *
     * @param response the response
     * @return the argo repositories list
     */
    public ArgoRepositoriesList extractArgoRepositoriesList(String response) {
        Type type = new TypeToken<ArgoRepositoriesList>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * Extract EKS cluster details.
     *
     * @param response the response
     * @return the aws cluster details
     */
    public AwsClusterDetails extractEKSClusterDetails(String response) {
        Type type = new TypeToken<AwsClusterDetails>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * Extract argo cluster.
     *
     * @param response the response
     * @return the argo cluster item
     */
    public ArgoClusterItem extractArgoCluster(String response) {
        Type type = new TypeToken<ArgoClusterItem>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }
    
    /**
     * Extract argo projects list.
     *
     * @param response the response
     * @return the project list
     */
    public ProjectList extractArgoProjectsList(String response) {
        Type type = new TypeToken<ProjectList>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }
    
    /**
     * Extract argo project dtls.
     *
     * @param response the response
     * @return the project
     */
    public Project extractArgoProjectDtls(String response) {
        Type type = new TypeToken<Project>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }
}
