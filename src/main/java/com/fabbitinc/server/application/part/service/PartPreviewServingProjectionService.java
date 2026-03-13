package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewArtifact;
import com.fabbitinc.server.domain.part.model.PartPreviewServingProjection;
import com.fabbitinc.server.domain.part.repository.PartPreviewServingProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartPreviewServingProjectionService {

    private final PartPreviewServingProjectionRepository partPreviewServingProjectionRepository;

    public void upsert(PartPreview partPreview) {
        PartPreviewServingProjection projection = partPreviewServingProjectionRepository.findById(partPreview.getId())
                .orElseGet(() -> PartPreviewServingProjection.create(partPreview.getId()));
        projection.changeServingKeys(
                partPreview.getOriginalFileKey(),
                partPreview.getPdfKey(),
                findGlbKey(partPreview),
                partPreview.getWebpKey()
        );
        partPreviewServingProjectionRepository.save(projection);
    }

    private String findGlbKey(PartPreview partPreview) {
        return partPreview.getArtifacts().stream()
                .filter(artifact -> artifact.getArtifactType() == DrawingArtifactType.DERIVED_GLB)
                .map(PartPreviewArtifact::getStorageKey)
                .findFirst()
                .orElse(null);
    }
}
