package com.fabbitinc.server.integration.support;

import com.fabbitinc.server.application.file.port.StorageDeleteResult;
import com.fabbitinc.server.application.file.port.StorageObjectListPage;
import com.fabbitinc.server.application.file.port.StorageObjectMeta;
import com.fabbitinc.server.application.file.port.StoragePort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestStoragePort implements StoragePort {

    private final Map<String, StoredObject> objects = new LinkedHashMap<>();

    @Override
    public String generateUploadPresignedUrl(String fileKey, String contentType, long contentLength) {
        return "http://test-storage/" + fileKey;
    }

    @Override
    public StorageObjectMeta headObject(String fileKey) {
        StoredObject object = objects.get(fileKey);
        if (object == null) {
            return null;
        }
        return new StorageObjectMeta(object.content.length, object.contentType);
    }

    @Override
    public StorageObjectListPage listObjects(String prefix, String continuationToken, int maxKeys) {
        List<String> keys = objects.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .limit(maxKeys)
                .toList();
        return new StorageObjectListPage(keys, null);
    }

    @Override
    public byte[] getObject(String fileKey) {
        StoredObject object = objects.get(fileKey);
        return object == null ? null : object.content;
    }

    @Override
    public void putObject(String fileKey, byte[] content, String contentType) {
        objects.put(fileKey, new StoredObject(content, contentType));
    }

    @Override
    public StorageDeleteResult deleteObjects(List<String> fileKeys) {
        List<String> deleted = fileKeys.stream()
                .filter(objects::containsKey)
                .toList();
        deleted.forEach(objects::remove);
        return new StorageDeleteResult(deleted, List.of());
    }

    @Override
    public void deleteObject(String fileKey) {
        objects.remove(fileKey);
    }

    private record StoredObject(byte[] content, String contentType) {
    }
}
