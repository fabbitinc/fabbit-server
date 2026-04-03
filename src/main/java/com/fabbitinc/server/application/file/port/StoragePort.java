package com.fabbitinc.server.application.file.port;

import java.time.Duration;
import java.util.List;

public interface StoragePort {

    String generateUploadPresignedUrl(String fileKey, String contentType, long contentLength);

    String generateGetPresignedUrl(String fileKey, Duration expireDuration);

    StorageObjectMeta headObject(String fileKey);

    StorageObjectListPage listObjects(String prefix, String continuationToken, int maxKeys);

    byte[] getObject(String fileKey);

    void putObject(String fileKey, byte[] content, String contentType);

    StorageDeleteResult deleteObjects(List<String> fileKeys);

    void deleteObject(String fileKey);
}
