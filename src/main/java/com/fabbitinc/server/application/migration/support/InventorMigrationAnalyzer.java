package com.fabbitinc.server.application.migration.support;

import com.fabbitinc.server.application.migration.model.InventorManifestFile;
import com.fabbitinc.server.application.migration.model.InventorManifestFileType;
import com.fabbitinc.server.application.migration.model.InventorMigrationSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class InventorMigrationAnalyzer {

    public Analysis analyze(InventorMigrationSession session) {
        List<InventorManifestFile> files = session.files();
        Map<String, List<InventorManifestFile>> drawingsByMatchKey = new LinkedHashMap<>();
        for (InventorManifestFile file : files) {
            if (file.type() != InventorManifestFileType.DRAWING) {
                continue;
            }
            drawingsByMatchKey.computeIfAbsent(matchKey(file.path()), key -> new ArrayList<>()).add(file);
        }

        List<ImportItem> items = new ArrayList<>();
        Map<String, Integer> partNumberCounts = new LinkedHashMap<>();
        Set<String> matchedDrawingPaths = new LinkedHashSet<>();

        for (InventorManifestFile file : files) {
            if (!isImportable(file)) {
                continue;
            }
            String derivedPartNumber = derivePartNumber(file.path());
            List<InventorManifestFile> matchedDrawings = drawingsByMatchKey.getOrDefault(matchKey(file.path()), List.of());
            List<String> drawingPaths = matchedDrawings.stream().map(InventorManifestFile::path).toList();
            List<UUID> drawingFileIds = matchedDrawings.stream()
                    .map(drawing -> session.fileIdOf(drawing.path()))
                    .filter(Objects::nonNull)
                    .toList();
            matchedDrawingPaths.addAll(drawingPaths);
            items.add(new ImportItem(
                    file.path(),
                    file.type(),
                    derivedPartNumber,
                    session.fileIdOf(file.path()),
                    drawingPaths,
                    drawingFileIds
            ));
            partNumberCounts.merge(derivedPartNumber, 1, Integer::sum);
        }

        List<String> duplicatePartNumbers = partNumberCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        List<OrphanDrawing> orphanDrawings = files.stream()
                .filter(file -> file.type() == InventorManifestFileType.DRAWING)
                .filter(file -> !matchedDrawingPaths.contains(file.path()))
                .map(file -> new OrphanDrawing(file.path(), session.fileIdOf(file.path())))
                .toList();

        return new Analysis(items, orphanDrawings, duplicatePartNumbers, files.size(), countImportable(files));
    }

    public String derivePartNumber(String path) {
        String fileName = fileName(path);
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    public int countImportable(List<InventorManifestFile> files) {
        return (int) files.stream().filter(this::isImportable).count();
    }

    private String matchKey(String path) {
        return parentDirectory(path) + "|" + basenameWithoutExtension(path);
    }

    private String parentDirectory(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(0, lastSlash) : "";
    }

    private String basenameWithoutExtension(String path) {
        String fileName = fileName(path);
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    private String fileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private boolean isImportable(InventorManifestFile file) {
        return file.type() == InventorManifestFileType.PART || file.type() == InventorManifestFileType.ASSEMBLY;
    }

    public record Analysis(
            List<ImportItem> items,
            List<OrphanDrawing> orphanDrawings,
            List<String> duplicatePartNumbers,
            int totalFileCount,
            int importableFileCount
    ) {
    }

    public record ImportItem(
            String path,
            InventorManifestFileType fileType,
            String derivedPartNumber,
            UUID modelFileId,
            List<String> matchedDrawingPaths,
            List<UUID> matchedDrawingFileIds
    ) {
    }

    public record OrphanDrawing(
            String path,
            UUID fileId
    ) {
    }
}
