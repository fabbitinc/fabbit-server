package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;

public interface DrawingPipeline {

    String key();

    boolean supports(DrawingSourceType sourceType, DrawingDimension dimension, String profileKey);

    DrawingPipelineResult process(DrawingPipelineCommand command) throws Exception;
}
