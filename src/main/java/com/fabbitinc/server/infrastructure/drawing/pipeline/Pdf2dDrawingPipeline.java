package com.fabbitinc.server.infrastructure.drawing.pipeline;

import com.fabbitinc.server.application.drawing.port.PdfPreviewRenderPort;
import com.fabbitinc.server.application.drawing.service.DrawingPipeline;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineArtifact;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineCommand;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineDeadlineContext;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineResult;
import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;
import java.util.List;

public class Pdf2dDrawingPipeline implements DrawingPipeline {

    private final PdfPreviewRenderPort pdfPreviewRenderPort;

    public Pdf2dDrawingPipeline(PdfPreviewRenderPort pdfPreviewRenderPort) {
        this.pdfPreviewRenderPort = pdfPreviewRenderPort;
    }

    @Override
    public String key() {
        return "pdf-2d-default";
    }

    @Override
    public boolean supports(DrawingSourceType sourceType, DrawingDimension dimension, String profileKey) {
        return sourceType == DrawingSourceType.PDF_DOCUMENT && dimension == DrawingDimension.TWO_D;
    }

    @Override
    public DrawingPipelineResult process(DrawingPipelineCommand command) throws Exception {
        String previewName = buildPreviewName(command.sourceFile().getOriginalName());
        GeneratedBinary preview = DrawingPipelineDeadlineContext.call(
                "pdf_preview_render",
                () -> pdfPreviewRenderPort.render(command.inputPath(), previewName)
        );

        return new DrawingPipelineResult(List.of(
                DrawingPipelineArtifact.reuseSource(DrawingArtifactType.DERIVED_PDF),
                DrawingPipelineArtifact.generated(
                        DrawingArtifactType.DERIVED_WEBP,
                        preview.fileName(),
                        preview.contentType(),
                        preview.bytes()
                )
        ));
    }

    private String replaceSuffix(String value, String replacement) {
        int idx = value.lastIndexOf('.');
        if (idx < 0) {
            return value + replacement;
        }
        return value.substring(0, idx) + replacement;
    }

    private String buildPreviewName(String originalName) {
        String previewName = replaceSuffix(originalName, ".webp");
        if (previewName.equals(originalName)) {
            return replaceSuffix(originalName, "_thumbnail.webp");
        }
        return previewName;
    }
}
