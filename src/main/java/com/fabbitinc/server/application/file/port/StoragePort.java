package com.fabbitinc.server.application.file.port;

public interface StoragePort {

    String generateUploadPresignedUrl(String fileKey, String contentType, long contentLength);

    StorageObjectMeta headObject(String fileKey);

    byte[] getObject(String fileKey);
}
