package com.opsera.integrator.argo.exceptions;

public class MessageProcessingException extends RuntimeException {

    private static final long serialVersionUID = -7539421896555507049L;

    /**
     *
     * @param message
     */
    public MessageProcessingException(String message) {
        super(message);
    }
}
