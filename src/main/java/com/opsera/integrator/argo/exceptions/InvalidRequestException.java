package com.opsera.integrator.argo.exceptions;

public class InvalidRequestException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 9048261618755200002L;

    /**
     * @param message
     */
    public InvalidRequestException(String message) {
        super(message);
    }

    /**
     * @param message
     */
    public InvalidRequestException(String message, Throwable ex) {
        super(message, ex);
    }
    
    
}
