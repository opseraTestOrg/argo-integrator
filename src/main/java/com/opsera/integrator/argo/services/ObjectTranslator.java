package com.opsera.integrator.argo.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

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
        List<ArgoApplicationMetadata> metadataList = new ArrayList<>();
        if (!ObjectUtils.isEmpty(argoApplicationsList.getItems())) {
            for (ArgoApplicationItem item : argoApplicationsList.getItems()) {
                ArgoApplicationMetadata appMetadata = constructAppMetadataListResult(item);
                metadataList.add(appMetadata);
            }
        }
        return new ArgoApplicationMetadataList(metadataList);
    }

    private ArgoApplicationMetadata constructAppMetadataListResult(ArgoApplicationItem item) {
        ArgoApplicationMetadata appMetadata = new ArgoApplicationMetadata();
        if (!ObjectUtils.isEmpty(item.getMetadata())) {
            ArgoApplicationMetadata itemMetadata = item.getMetadata();
            appMetadata.setName(itemMetadata.getName());
            appMetadata.setClusterName(itemMetadata.getNamespace());
            appMetadata.setCreationTimestamp(itemMetadata.getCreationTimestamp());
            appMetadata.setResourceVersion(itemMetadata.getResourceVersion());
            appMetadata.setUid(itemMetadata.getUid());
        }
        if (!ObjectUtils.isEmpty(item.getSpec())) {
            appMetadata.setProject(item.getSpec().getProject());
            if (!ObjectUtils.isEmpty(item.getSpec().getSource())) {
                appMetadata.setRepoUrl(item.getSpec().getSource().getRepoURL());
                appMetadata.setPath(item.getSpec().getSource().getPath());
                appMetadata.setBranch(item.getSpec().getSource().getTargetRevision());
            }
            if (!ObjectUtils.isEmpty(item.getSpec().getDestination())) {
                appMetadata.setClusterUrl(item.getSpec().getDestination().getServer());
            }
            if (!ObjectUtils.isEmpty(item.getSpec().getSyncPolicy()) && !ObjectUtils.isEmpty(item.getSpec().getSyncPolicy().getAutomated())) {
                appMetadata.setAutoSync(true);
            }
        }
        if (!ObjectUtils.isEmpty(item.getStatus())) {
            appMetadata.setPhase(item.getStatus().getPhase());
            if (!ObjectUtils.isEmpty(item.getStatus().getSync())) {
                appMetadata.setSyncStatus(item.getStatus().getSync().getStatus());
            }
            if (!ObjectUtils.isEmpty(item.getStatus().getHealth())) {
                appMetadata.setSyncStatus(item.getStatus().getHealth().getStatus());
            }
        }
        return appMetadata;
    }
    
    public ArgoApplicationMetadataList translateToArgoProjectMetadataList(ArgoApplicationsList argoApplicationsList) {
        List<ArgoApplicationMetadata> metadataList = argoApplicationsList.getItems().stream().map(ArgoApplicationItem::getMetadata).collect(Collectors.toList());
        return new ArgoApplicationMetadataList(metadataList);
    }
}
