package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class Result {

    private String content;
    private String timeStamp;
    private Boolean last;
    private String timeStampStr;
    private String podName;
}
