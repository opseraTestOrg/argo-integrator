package com.opsera.integrator.argo.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.opsera.integrator.argo.resources.ArgoApplicationItem;
import com.opsera.integrator.argo.resources.ArgoApplicationMetadata;
import com.opsera.integrator.argo.resources.ArgoApplicationMetadataList;
import com.opsera.integrator.argo.resources.ArgoApplicationsList;

/**
 * Class to translate objects to desired types
 */
@Component
public class ObjectTranslator {

    /**
     * extracts the metadata info from all argo applications
     * 
     * @param argoApplicationsList
     * @return
     */
    public ArgoApplicationMetadataList translateToArgoApplicationMetadataList(ArgoApplicationsList argoApplicationsList) {
        List<ArgoApplicationMetadata> metadataList = argoApplicationsList.getItems().stream().map(ArgoApplicationItem::getMetadata).collect(Collectors.toList());
        return new ArgoApplicationMetadataList(metadataList);
    }
}
