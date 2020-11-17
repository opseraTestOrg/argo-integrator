/**
 * 
 */
package com.opsera.integrator.argo.resources;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Purusothaman
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@Builder
public class ValidationResponse implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String status;

    private String message;

}
