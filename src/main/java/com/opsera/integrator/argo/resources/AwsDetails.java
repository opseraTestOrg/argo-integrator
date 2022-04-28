package com.opsera.integrator.argo.resources;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(value = Include.NON_NULL)
public class AwsDetails implements Serializable {

    private static final long serialVersionUID = -4049511035663828096L;

    private String toolId;
    private String customerId;
    private String roleArn;
    private String roleSessionName;
    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;
    private String region;

}
