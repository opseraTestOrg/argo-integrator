package com.opsera.integrator.argo.resources;

import lombok.Data;

@Data
public class Cluster {

    private String name;
    
    private String endpoint;
    
    private CertificateAuthority certificateAuthority;
}
