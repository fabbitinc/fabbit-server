package com.fabbitinc.server.infrastructure.drawing.pipeline;

import com.fabbitinc.server.application.drawing.port.Cad3dPreviewRenderPort;
import com.fabbitinc.server.application.drawing.port.Cad3dToGlbPort;
import com.fabbitinc.server.application.drawing.service.DrawingPipeline;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineArtifact;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineCommand;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineDeadlineContext;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineResult;
import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;
import com.fabbitinc.server.infrastructure.drawing.adapter.ImageIoWebpTranscoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
public class Cad3dDrawingPipeline implements DrawingPipeline {

    private final Cad3dToGlbPort cad3dToGlbPort;
    private final Cad3dPreviewRenderPort cad3dPreviewRenderPort;
    private final ImageIoWebpTranscoder imageIoWebpTranscoder;

    public Cad3dDrawingPipeline(
            Cad3dToGlbPort cad3dToGlbPort,
            Cad3dPreviewRenderPort cad3dPreviewRenderPort,
            ImageIoWebpTranscoder imageIoWebpTranscoder
    ) {
        this.cad3dToGlbPort = cad3dToGlbPort;
        this.cad3dPreviewRenderPort = cad3dPreviewRenderPort;
        this.imageIoWebpTranscoder = imageIoWebpTranscoder;
    }

    @Override
    public String key() {
        return "cad-3d-default";
    }

    @Override
    public boolean supports(DrawingSourceType sourceType, DrawingDimension dimension, String profileKey) {
        return sourceType == DrawingSourceType.CAD_3D && dimension == DrawingDimension.THREE_D;
    }

    @Override
    public DrawingPipelineResult process(DrawingPipelineCommand command) throws Exception {
        List<DrawingPipelineArtifact> artifacts = new ArrayList<>();
        String originalName = command.sourceFile().getOriginalName();
        Path previewSourcePath = command.inputPath();

        if (hasExtension(originalName, ".glb")) {
            artifacts.add(DrawingPipelineArtifact.reuseSource(DrawingArtifactType.DERIVED_GLB));
        } else {
            String glbName = replaceSuffix(originalName, ".glb");
            GeneratedBinary glb = DrawingPipelineDeadlineContext.call(
                    "cad_3d_to_glb",
                    () -> cad3dToGlbPort.convertToGlb(command.inputPath(), glbName)
            );
            previewSourcePath = command.workDir().resolve(glb.fileName());
            Files.write(previewSourcePath, glb.bytes());
            artifacts.add(DrawingPipelineArtifact.generated(
                    DrawingArtifactType.DERIVED_GLB,
                    glb.fileName(),
                    glb.contentType(),
                    glb.bytes()
            ));
        }

        String previewPngName = replaceSuffix(originalName, ".png");
        GeneratedBinary previewPng = DrawingPipelineDeadlineContext.call(
                "cad_3d_glb_to_png",
                () -> cad3dPreviewRenderPort.renderPreview(previewSourcePath, previewPngName)
        );
        GeneratedBinary previewWebp = DrawingPipelineDeadlineContext.call(
                "cad_3d_png_to_webp",
                () -> imageIoWebpTranscoder.transcode(
                        previewPng.bytes(),
                        buildPreviewName(originalName)
                )
        );
        artifacts.add(DrawingPipelineArtifact.generated(
                DrawingArtifactType.DERIVED_WEBP,
                previewWebp.fileName(),
                previewWebp.contentType(),
                previewWebp.bytes()
        ));
        return new DrawingPipelineResult(artifacts);
    }

    private boolean hasExtension(String fileName, String extension) {
        return fileName != null && fileName.toLowerCase().endsWith(extension);
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
