package com.opsera.integrator.argo.exceptions;

import java.io.Serializable;

/**
 * Error Response
 */
public class ArgoErrorResponse implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5983466883002203161L;

    private String status;

    private String message;

    public ArgoErrorResponse() {
        // do nothing
    }

    public ArgoErrorResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
}

