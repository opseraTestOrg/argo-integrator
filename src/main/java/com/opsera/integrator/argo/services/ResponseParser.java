package com.opsera.integrator.argo.services;

import java.lang.reflect.Type;

import com.opsera.integrator.argo.resources.ArgoClusterList;
import com.opsera.integrator.argo.resources.ArgoRepositoriesList;
import com.opsera.integrator.argo.resources.ArgoRepositoryItem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.reflect.TypeToken;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationsList;
import com.opsera.integrator.argo.resources.ToolConfig;
import com.opsera.integrator.argo.resources.ArgoToolDetails;

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
}
