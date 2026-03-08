package com.fabbitinc.server.infrastructure.drawing.adapter;

import com.fabbitinc.server.application.drawing.service.GeneratedBinary;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

public class ImageIoWebpTranscoder {

    public GeneratedBinary transcode(byte[] sourceBytes, String outputFileName) throws Exception {
        BufferedImage image = readImage(sourceBytes);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(image, "webp", outputStream);
            if (!written) {
                throw new IllegalStateException("WebP 변환기를 찾을 수 없습니다");
            }
            return new GeneratedBinary(outputFileName, "image/webp", outputStream.toByteArray());
        }
    }

    private BufferedImage readImage(byte[] sourceBytes) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceBytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다");
            }
            return image;
        }
    }
}
