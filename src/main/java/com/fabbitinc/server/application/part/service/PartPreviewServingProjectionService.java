package com.fabbitinc.server.application.part.service;

import com.fabbitinc.server.domain.part.model.PartPreview;
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
                partPreview.getPdfKey(),
                partPreview.getGlbKey(),
                partPreview.getWebpKey()
        );
        partPreviewServingProjectionRepository.save(projection);
    }
}
