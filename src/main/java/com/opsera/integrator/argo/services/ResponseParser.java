package com.opsera.integrator.argo.services;

import java.lang.reflect.Type;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.reflect.TypeToken;
import com.opsera.integrator.argo.config.IServiceFactory;
import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationsList;
import com.opsera.integrator.argo.resources.ArgoToolConfig;
import com.opsera.integrator.argo.resources.ArgoToolDetails;

/**
 * Class to deserialize all the json responses from rest endpoints
 */
@Component
public class ResponseParser {

    @Autowired
    private IServiceFactory serviceFactory;

    /**
     * extractArgoToolDetails
     * 
     * @param response
     * @return
     */
    public ArgoToolDetails extractArgoToolDetails(String response) {
        Type type = new TypeToken<ArgoToolDetails>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * extractArgoToolConfig
     * 
     * @param response
     * @return
     */
    public ArgoToolConfig extractArgoToolConfig(String response) {
        Type type = new TypeToken<ArgoToolConfig>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * extractArgoApplicationItem
     * 
     * @param response
     * @return
     */
    public ArgoApplicationItem extractArgoApplicationItem(String response) {
        Type type = new TypeToken<ArgoApplicationItem>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }

    /**
     * extractArgoApplicationsList
     * 
     * @param response
     * @return
     */
    public ArgoApplicationsList extractArgoApplicationsList(String response) {
        Type type = new TypeToken<ArgoApplicationsList>() {
        }.getType();
        return serviceFactory.gson().fromJson(response, type);
    }
}
