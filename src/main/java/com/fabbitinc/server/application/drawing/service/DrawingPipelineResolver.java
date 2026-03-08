package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;
import java.util.List;

public class DrawingPipelineResolver {

    private final List<DrawingPipeline> pipelines;

    public DrawingPipelineResolver(List<DrawingPipeline> pipelines) {
        this.pipelines = List.copyOf(pipelines);
    }

    public DrawingPipeline resolve(DrawingSourceType sourceType, DrawingDimension dimension, String profileKey) {
        return pipelines.stream()
                .filter(pipeline -> pipeline.supports(sourceType, dimension, profileKey))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "지원하는 도면 파이프라인이 없습니다: sourceType=%s, dimension=%s, profile=%s"
                                .formatted(sourceType, dimension, profileKey)
                ));
    }
}
