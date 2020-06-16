package com.opsera.integrator.argo.exceptions;

import com.google.gson.Gson;
import com.opsera.integrator.argo.config.IServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Exception Handler
 */
public class ArgoExceptionHandler extends ResponseEntityExceptionHandler {

    @Autowired
    private IServiceFactory factory;

    /**
     * Custom exception handler for record not found
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(InternalServiceException.class)
    protected ResponseEntity<Object> handleRecordNotFound(InternalServiceException ex) {
        ArgoErrorResponse sonarQubeErrorResponse = new ArgoErrorResponse();
        sonarQubeErrorResponse.setMessage(ex.getMessage());
        sonarQubeErrorResponse.setStatus(HttpStatus.NOT_FOUND.name());
        return new ResponseEntity<>(sonarQubeErrorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     *
     * Handling Server side error while calling other service
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(HttpServerErrorException.class)
    protected ResponseEntity<Object> handleHttpServerErrorException(HttpServerErrorException ex) {
        String response = ex.getResponseBodyAsString();
        Gson gson = factory.gson();
        ArgoErrorResponse sonarQubeErrorResponse = gson.fromJson(response, ArgoErrorResponse.class);
        return new ResponseEntity<>(sonarQubeErrorResponse, ex.getStatusCode());
    }

    /**
     *
     * Handling client side error while calling other service
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(HttpClientErrorException.class)
    protected ResponseEntity<Object> handleHttpClientErrorException(HttpClientErrorException ex) {
        String response = ex.getResponseBodyAsString();
        Gson gson = factory.gson();
        ArgoErrorResponse sonarQubeErrorResponse = gson.fromJson(response, ArgoErrorResponse.class);
        return new ResponseEntity<>(sonarQubeErrorResponse, ex.getStatusCode());
    }
}

