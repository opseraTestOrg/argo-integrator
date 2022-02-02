package com.opsera.integrator.argo.exceptions;

public class UnAuthorizedException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 4439155603236054631L;

    public UnAuthorizedException(String message) {
        super(message);
    }

    /**
     * @param message
     */
    public UnAuthorizedException(String message, Throwable ex) {
        super(message, ex);
    }
}
