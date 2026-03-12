package com.fabbitinc.server.application.drawing.config;

import com.fabbitinc.server.application.drawing.port.Cad2dToPdfPort;
import com.fabbitinc.server.application.drawing.port.PdfPreviewRenderPort;
import com.fabbitinc.server.application.drawing.port.RasterImageToPdfPort;
import com.fabbitinc.server.application.drawing.service.DrawingPipeline;
import com.fabbitinc.server.application.drawing.service.DrawingPipelineResolver;
import com.fabbitinc.server.application.drawing.service.DrawingSourceClassifier;
import com.fabbitinc.server.infrastructure.drawing.adapter.EzdxfCad2dToPdfAdapter;
import com.fabbitinc.server.infrastructure.drawing.adapter.ImageIoWebpTranscoder;
import com.fabbitinc.server.infrastructure.drawing.adapter.Mayo3dConverterAdapter;
import com.fabbitinc.server.infrastructure.drawing.adapter.PdfBoxPdfPreviewRenderAdapter;
import com.fabbitinc.server.infrastructure.drawing.adapter.PdfBoxRasterImageToPdfAdapter;
import com.fabbitinc.server.infrastructure.drawing.pipeline.Cad2dDrawingPipeline;
import com.fabbitinc.server.infrastructure.drawing.pipeline.Cad3dDrawingPipeline;
import com.fabbitinc.server.infrastructure.drawing.pipeline.Pdf2dDrawingPipeline;
import com.fabbitinc.server.infrastructure.drawing.pipeline.Raster2dDrawingPipeline;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DrawingPipelineConfiguration {

    @Bean
    public DrawingSourceClassifier drawingSourceClassifier() {
        return new DrawingSourceClassifier();
    }

    @Bean
    public Cad2dToPdfPort cad2dToPdfPort(DrawingConverterProperties drawingConverterProperties) {
        return new EzdxfCad2dToPdfAdapter(drawingConverterProperties);
    }

    @Bean
    public PdfPreviewRenderPort pdfPreviewRenderPort() {
        return new PdfBoxPdfPreviewRenderAdapter();
    }

    @Bean
    public RasterImageToPdfPort rasterImageToPdfPort() {
        return new PdfBoxRasterImageToPdfAdapter();
    }

    @Bean
    public ImageIoWebpTranscoder imageIoWebpTranscoder() {
        return new ImageIoWebpTranscoder();
    }

    @Bean
    public Mayo3dConverterAdapter mayo3dConverterAdapter(DrawingConverterProperties drawingConverterProperties) {
        return new Mayo3dConverterAdapter(drawingConverterProperties);
    }

    @Bean
    public DrawingPipeline pdf2dDrawingPipeline(PdfPreviewRenderPort pdfPreviewRenderPort) {
        return new Pdf2dDrawingPipeline(pdfPreviewRenderPort);
    }

    @Bean
    public DrawingPipeline raster2dDrawingPipeline(
            RasterImageToPdfPort rasterImageToPdfPort,
            PdfPreviewRenderPort pdfPreviewRenderPort
    ) {
        return new Raster2dDrawingPipeline(rasterImageToPdfPort, pdfPreviewRenderPort);
    }

    @Bean
    public DrawingPipeline cad2dDrawingPipeline(
            Cad2dToPdfPort cad2dToPdfPort,
            PdfPreviewRenderPort pdfPreviewRenderPort
    ) {
        return new Cad2dDrawingPipeline(cad2dToPdfPort, pdfPreviewRenderPort);
    }

    @Bean
    public DrawingPipeline cad3dDrawingPipeline(
            Mayo3dConverterAdapter mayo3dConverterAdapter,
            ImageIoWebpTranscoder imageIoWebpTranscoder
    ) {
        return new Cad3dDrawingPipeline(
                mayo3dConverterAdapter,
                mayo3dConverterAdapter,
                imageIoWebpTranscoder
        );
    }

    @Bean
    public DrawingPipelineResolver drawingPipelineResolver(List<DrawingPipeline> drawingPipelines) {
        return new DrawingPipelineResolver(drawingPipelines);
    }
}
