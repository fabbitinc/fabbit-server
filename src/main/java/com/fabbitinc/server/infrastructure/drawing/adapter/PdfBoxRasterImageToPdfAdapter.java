package com.fabbitinc.server.infrastructure.drawing.adapter;

import com.fabbitinc.server.application.drawing.port.RasterImageToPdfPort;
import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class PdfBoxRasterImageToPdfAdapter implements RasterImageToPdfPort {

    @Override
    public GeneratedBinary convert(Path inputPath, String outputFileName) throws Exception {
        BufferedImage image = ImageIO.read(inputPath.toFile());
        if (image == null) {
            throw new IOException("이미지 파일을 읽을 수 없습니다: " + inputPath.getFileName());
        }

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
            document.addPage(page);

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
            }

            document.save(outputStream);
            return new GeneratedBinary(outputFileName, "application/pdf", outputStream.toByteArray());
        }
    }
}
