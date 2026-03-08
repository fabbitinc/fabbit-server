package com.fabbitinc.server.infrastructure.drawing.adapter;

import com.fabbitinc.server.application.drawing.port.PdfPreviewRenderPort;
import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PdfBoxPdfPreviewRenderAdapter implements PdfPreviewRenderPort {

    private static final float THUMBNAIL_DPI = 150.0f;

    @Override
    public GeneratedBinary render(Path pdfPath, String outputFileName) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, THUMBNAIL_DPI);
            boolean written = ImageIO.write(image, "webp", outputStream);
            if (!written) {
                throw new IllegalStateException("WebP 이미지 렌더러를 찾을 수 없습니다");
            }
            return new GeneratedBinary(outputFileName, "image/webp", outputStream.toByteArray());
        }
    }
}
