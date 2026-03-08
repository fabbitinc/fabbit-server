package com.fabbitinc.server.application.image.service;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.domain.file.model.File;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

public class ImageVariantService {

    private static final int PROFILE_THUMBNAIL_SIZE = 256;
    private static final String WEBP_CONTENT_TYPE = "image/webp";

    private final StoragePort storagePort;

    public ImageVariantService(StoragePort storagePort) {
        this.storagePort = storagePort;
    }

    public void convertToThumbnail(File file) {
        String originalKey = file.getFileKey();
        byte[] originalBytes = storagePort.getObject(originalKey);
        byte[] thumbnailBytes = createThumbnailWebp(originalBytes);
        String thumbnailKey = replaceSuffix(originalKey, ".webp");

        storagePort.putObject(thumbnailKey, thumbnailBytes, WEBP_CONTENT_TYPE);
        if (!originalKey.equals(thumbnailKey)) {
            storagePort.deleteObject(originalKey);
        }

        file.changeStoredObject(thumbnailKey, WEBP_CONTENT_TYPE, thumbnailBytes.length);
    }

    private byte[] createThumbnailWebp(byte[] originalBytes) {
        BufferedImage sourceImage = readImage(originalBytes);
        BufferedImage croppedImage = centerCropSquare(sourceImage);
        BufferedImage thumbnailImage = resizeToThumbnail(croppedImage);
        return writeWebp(thumbnailImage);
    }

    private BufferedImage readImage(byte[] originalBytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "지원하지 않는 이미지 형식입니다");
            }
            return image;
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 변환 중 오류가 발생했습니다");
        }
    }

    private BufferedImage centerCropSquare(BufferedImage sourceImage) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int cropSize = Math.min(width, height);
        int startX = (width - cropSize) / 2;
        int startY = (height - cropSize) / 2;
        BufferedImage croppedImage = sourceImage.getSubimage(startX, startY, cropSize, cropSize);

        BufferedImage rgbImage = new BufferedImage(cropSize, cropSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.drawImage(croppedImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgbImage;
    }

    private BufferedImage resizeToThumbnail(BufferedImage sourceImage) {
        BufferedImage thumbnailImage = new BufferedImage(
                PROFILE_THUMBNAIL_SIZE,
                PROFILE_THUMBNAIL_SIZE,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = thumbnailImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, PROFILE_THUMBNAIL_SIZE, PROFILE_THUMBNAIL_SIZE, null);
        } finally {
            graphics.dispose();
        }
        return thumbnailImage;
    }

    private byte[] writeWebp(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean written = ImageIO.write(image, "webp", outputStream);
            if (!written) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "WebP 변환기를 찾을 수 없습니다");
            }
            return outputStream.toByteArray();
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "WebP 이미지 저장 중 오류가 발생했습니다");
        }
    }

    private String replaceSuffix(String value, String replacement) {
        int idx = value.lastIndexOf('.');
        if (idx < 0) {
            return value + replacement;
        }
        return value.substring(0, idx) + replacement;
    }
}
