/**
 * 
 */
package com.opsera.integrator.argo.exceptions;

/**
 * @author sundar
 *
 */
public class ResourcesNotAvailable extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -4920231650381553884L;

    /**
     * 
     * @param message
     */
    public ResourcesNotAvailable(String message) {
        super(message);
    }

    public ResourcesNotAvailable(String message, Throwable err) {
        super(message, err);
    }

}
