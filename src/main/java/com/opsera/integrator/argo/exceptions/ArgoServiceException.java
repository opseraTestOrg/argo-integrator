package com.opsera.integrator.argo.exceptions;

import lombok.Data;

@Data
public class ArgoServiceException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 4704778492789155180L;
    private Integer errorCode;

    /**
     * @param message
     */
    public ArgoServiceException(String message) {
        super(message);
    }

    /**
     * @param message
     */
    public ArgoServiceException(String message, Throwable ex) {
        super(message, ex);
    }
    
    public ArgoServiceException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
