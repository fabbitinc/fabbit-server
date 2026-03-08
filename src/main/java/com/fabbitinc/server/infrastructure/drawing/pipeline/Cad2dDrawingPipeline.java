package com.fabbitinc.server.infrastructure.drawing.pipeline;

import com.fabbitinc.server.application.drawing.port.Cad2dToPdfPort;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Cad2dDrawingPipeline implements DrawingPipeline {

    private final Cad2dToPdfPort cad2dToPdfPort;
    private final PdfPreviewRenderPort pdfPreviewRenderPort;

    public Cad2dDrawingPipeline(Cad2dToPdfPort cad2dToPdfPort, PdfPreviewRenderPort pdfPreviewRenderPort) {
        this.cad2dToPdfPort = cad2dToPdfPort;
        this.pdfPreviewRenderPort = pdfPreviewRenderPort;
    }

    @Override
    public String key() {
        return "cad-2d-default";
    }

    @Override
    public boolean supports(DrawingSourceType sourceType, DrawingDimension dimension, String profileKey) {
        return sourceType == DrawingSourceType.CAD_2D && dimension == DrawingDimension.TWO_D;
    }

    @Override
    public DrawingPipelineResult process(DrawingPipelineCommand command) throws Exception {
        String pdfName = replaceSuffix(command.sourceFile().getOriginalName(), ".pdf");
        GeneratedBinary pdf = DrawingPipelineDeadlineContext.call(
                "cad_2d_to_pdf",
                () -> cad2dToPdfPort.convert(command.inputPath(), pdfName)
        );
        Path pdfPath = command.workDir().resolve(pdf.fileName());
        Files.write(pdfPath, pdf.bytes());

        String previewName = buildPreviewName(command.sourceFile().getOriginalName());
        GeneratedBinary preview = DrawingPipelineDeadlineContext.call(
                "cad_2d_pdf_preview_render",
                () -> pdfPreviewRenderPort.render(pdfPath, previewName)
        );

        return new DrawingPipelineResult(List.of(
                DrawingPipelineArtifact.generated(
                        DrawingArtifactType.DERIVED_PDF,
                        pdf.fileName(),
                        pdf.contentType(),
                        pdf.bytes()
                ),
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
