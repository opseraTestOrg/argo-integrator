package com.opsera.integrator.argo.exceptions;

public class ArgoServiceException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 4704778492789155180L;

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
}
