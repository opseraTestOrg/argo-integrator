package com.opsera.integrator.argo.services;

import com.opsera.integrator.argo.resources.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Class to translate objects to desired types
 */
@Component
public class ObjectTranslator {

    /**
     * extracts the metadata info from all argo applications
     * @param argoApplicationsList
     * @return
     */
    public ArgoApplicationMetadataList translateToArgoApplicationMetadataList(ArgoApplicationsList argoApplicationsList) {
        List<ArgoApplicationMetadata> metadataList = argoApplicationsList.getItems().stream()
                .map(ArgoApplicationItem::getMetadata)
                .collect(Collectors.toList());
        return new ArgoApplicationMetadataList(metadataList);
    }
}
