package com.opsera.integrator.argo.resources;

import java.util.List;

import lombok.Data;

@Data
public class Node {

    private String version;
    private String kind;
    private String name;
    private String namespace;
    private List<Info> info;
    private String group;
    private Health health;
    private List<String> images;
    private List<Node> parentRefs;
    

}
