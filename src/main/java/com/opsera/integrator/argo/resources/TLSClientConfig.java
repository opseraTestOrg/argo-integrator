package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class TLSClientConfig {

    private String caData;
    
    private String certData;
    
    private String keyData;
    
    private boolean insecure;
    
    private String serverName;
    
}
