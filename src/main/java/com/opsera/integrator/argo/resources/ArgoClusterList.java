package com.opsera.integrator.argo.resources;

import lombok.Data;

import java.util.List;

@Data
public class ArgoClusterList {

    private List<ArgoClusterItem> items;

}
