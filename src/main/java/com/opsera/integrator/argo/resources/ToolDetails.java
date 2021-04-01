/**
 * 
 */
package com.opsera.integrator.argo.resources;

import java.io.Serializable;

import lombok.Data;

/**
 * @author Purusothaman
 *
 */
@Data
public class ToolDetails implements Serializable {
    /**
    * 
    */
    private static final long serialVersionUID = 5963015657342766377L;

    private String customerId;

    private String toolId;

    private String url;

    private String userName;

    private String password;

    private String applicationName;
}
