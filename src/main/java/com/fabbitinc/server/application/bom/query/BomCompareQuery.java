package com.fabbitinc.server.application.bom.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.query.condition.BomCompareCondition;
import com.fabbitinc.server.application.bom.query.condition.BomCompareExportCondition;
import com.fabbitinc.server.application.bom.query.result.BomCompareResult;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.model.PartRevisionDiffChangeType;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BomCompareQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringBomItemRepository engineeringBomItemRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final PartRepository partRepository;

    public BomCompareResult compare(BomCompareCondition condition) {
        currentAuthProvider.getCurrentAuth();

        PartRevision sourceRevision = partRevisionRepository.findById(condition.sourceRevisionId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "소스 PartRevision '%s'을(를) 찾을 수 없습니다".formatted(condition.sourceRevisionId())
                ));
        PartRevision targetRevision = partRevisionRepository.findById(condition.targetRevisionId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "대상 PartRevision '%s'을(를) 찾을 수 없습니다".formatted(condition.targetRevisionId())
                ));

        Map<String, EngineeringBomItem> sourceItems = engineeringBomItemRepository
                .findByParentPartRevisionIdOrderByCreatedAtAsc(sourceRevision.getId())
                .stream()
                .collect(Collectors.toMap(
                        EngineeringBomItem::getLineNumber, item -> item, (left, right) -> left, LinkedHashMap::new
                ));
        Map<String, EngineeringBomItem> targetItems = engineeringBomItemRepository
                .findByParentPartRevisionIdOrderByCreatedAtAsc(targetRevision.getId())
                .stream()
                .collect(Collectors.toMap(
                        EngineeringBomItem::getLineNumber, item -> item, (left, right) -> left, LinkedHashMap::new
                ));

        Set<UUID> childRevisionIds = new LinkedHashSet<>();
        sourceItems.values().forEach(item -> childRevisionIds.add(item.getChildPartRevisionId()));
        targetItems.values().forEach(item -> childRevisionIds.add(item.getChildPartRevisionId()));
        Map<UUID, PartRevision> childRevisionsById = loadPartRevisions(childRevisionIds);
        Map<UUID, Part> childPartsById = loadPartsByRevisionIds(childRevisionsById.values());

        TreeSet<String> lineNumbers = new TreeSet<>(this::compareLineNumbers);
        lineNumbers.addAll(sourceItems.keySet());
        lineNumbers.addAll(targetItems.keySet());

        List<BomCompareResult.Change> changes = new ArrayList<>();
        int unchangedCount = 0;

        for (String lineNumber : lineNumbers) {
            EngineeringBomItem source = sourceItems.get(lineNumber);
            EngineeringBomItem target = targetItems.get(lineNumber);

            if (source != null && target != null
                    && Objects.equals(source.getChildPartRevisionId(), target.getChildPartRevisionId())
                    && Objects.equals(source.getQuantity(), target.getQuantity())) {
                unchangedCount++;
                continue;
            }

            PartRevisionDiffChangeType changeType;
            if (source == null) {
                changeType = PartRevisionDiffChangeType.ADDED;
            } else if (target == null) {
                changeType = PartRevisionDiffChangeType.REMOVED;
            } else {
                changeType = PartRevisionDiffChangeType.CHANGED;
            }

            PartRevision sourceChildRevision = source == null ? null : childRevisionsById.get(source.getChildPartRevisionId());
            PartRevision targetChildRevision = target == null ? null : childRevisionsById.get(target.getChildPartRevisionId());
            Part sourcePart = sourceChildRevision == null ? null : childPartsById.get(sourceChildRevision.getPartId());
            Part targetPart = targetChildRevision == null ? null : childPartsById.get(targetChildRevision.getPartId());

            changes.add(new BomCompareResult.Change(
                    lineNumber,
                    changeType,
                    sourcePart == null ? null : sourcePart.getPartNumber(),
                    sourceChildRevision == null ? null : sourceChildRevision.getName(),
                    source == null ? null : source.getQuantity(),
                    targetPart == null ? null : targetPart.getPartNumber(),
                    targetChildRevision == null ? null : targetChildRevision.getName(),
                    target == null ? null : target.getQuantity()
            ));
        }

        int addedCount = (int) changes.stream()
                .filter(c -> c.changeType() == PartRevisionDiffChangeType.ADDED).count();
        int removedCount = (int) changes.stream()
                .filter(c -> c.changeType() == PartRevisionDiffChangeType.REMOVED).count();
        int changedCount = (int) changes.stream()
                .filter(c -> c.changeType() == PartRevisionDiffChangeType.CHANGED).count();
        int totalCount = addedCount + removedCount + changedCount + unchangedCount;

        BomCompareResult.Summary summary = new BomCompareResult.Summary(
                addedCount, removedCount, changedCount, unchangedCount, totalCount
        );

        return new BomCompareResult(changes, summary);
    }

    public byte[] exportExcel(BomCompareExportCondition condition) {
        BomCompareResult result = compare(new BomCompareCondition(
                condition.sourceRevisionId(),
                condition.targetRevisionId()
        ));

        List<String> headers = List.of(
                "line_number",
                "change_type",
                "source_part_number",
                "source_quantity",
                "target_part_number",
                "target_quantity"
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BOM Compare");
            writeHeader(sheet, headers);

            int rowIndex = 1;
            for (BomCompareResult.Change change : result.changes()) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, change.lineNumber());
                writeCell(row, 1, change.changeType());
                writeCell(row, 2, change.sourcePartNumber());
                writeCell(row, 3, change.sourceQuantity());
                writeCell(row, 4, change.targetPartNumber());
                writeCell(row, 5, change.targetQuantity());
            }

            autoFitColumns(sheet, headers.size());
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "엑셀 파일 생성에 실패했습니다");
        }
    }

    private Map<UUID, PartRevision> loadPartRevisions(Set<UUID> revisionIds) {
        if (revisionIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, PartRevision> revisionsById = new LinkedHashMap<>();
        partRevisionRepository.findAllById(revisionIds)
                .forEach(revision -> revisionsById.put(revision.getId(), revision));
        return revisionsById;
    }

    private Map<UUID, Part> loadPartsByRevisionIds(Iterable<PartRevision> revisions) {
        Map<UUID, Part> partsById = new LinkedHashMap<>();
        List<UUID> partIds = new ArrayList<>();
        revisions.forEach(revision -> {
            if (revision != null) {
                partIds.add(revision.getPartId());
            }
        });
        partRepository.findAllById(partIds).forEach(part -> partsById.put(part.getId(), part));
        return partsById;
    }

    private int compareLineNumbers(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        Integer leftNumber = parseLineNumber(left);
        Integer rightNumber = parseLineNumber(right);
        if (leftNumber != null && rightNumber != null) {
            return Integer.compare(leftNumber, rightNumber);
        }
        return left.compareTo(right);
    }

    private Integer parseLineNumber(String lineNumber) {
        if (lineNumber == null || lineNumber.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(lineNumber.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void writeHeader(Sheet sheet, List<String> headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            writeCell(header, i, headers.get(i));
        }
    }

    private void writeCell(Row row, int colIndex, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof BigDecimal decimal) {
            row.createCell(colIndex).setCellValue(decimal.stripTrailingZeros().toPlainString());
            return;
        }
        row.createCell(colIndex).setCellValue(String.valueOf(value));
    }

    private void autoFitColumns(Sheet sheet, int size) {
        for (int i = 0; i < size; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            int maxWidth = 50 * 256;
            if (width > maxWidth) {
                sheet.setColumnWidth(i, maxWidth);
            }
        }
    }
}
