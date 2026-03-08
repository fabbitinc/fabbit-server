package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifact;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import com.fabbitinc.server.domain.drawing.model.DrawingServingProjection;
import com.fabbitinc.server.domain.drawing.repository.DrawingServingProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DrawingServingProjectionService {

    private final DrawingServingProjectionRepository drawingServingProjectionRepository;

    public void upsert(Drawing drawing) {
        DrawingServingProjection projection = drawingServingProjectionRepository.findById(drawing.getId())
                .orElseGet(() -> DrawingServingProjection.create(drawing.getId()));
        projection.changeServingKeys(
                drawing.getOriginalFileKey(),
                drawing.getPdfKey(),
                findGlbKey(drawing),
                drawing.getWebpKey()
        );
        drawingServingProjectionRepository.save(projection);
    }

    private String findGlbKey(Drawing drawing) {
        return drawing.getArtifacts().stream()
                .filter(artifact -> artifact.getArtifactType() == DrawingArtifactType.DERIVED_GLB)
                .map(DrawingArtifact::getStorageKey)
                .findFirst()
                .orElse(null);
    }
}
