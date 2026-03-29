package com.fabbitinc.server.application.bom.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.usecase.command.PreviewBomImportCommand;
import com.fabbitinc.server.application.bom.usecase.result.PreviewBomImportResult;
import com.fabbitinc.server.application.bom.usecase.result.PreviewBomImportResult.RowResult;
import com.fabbitinc.server.application.bom.usecase.result.PreviewBomImportResult.RowStatus;
import com.fabbitinc.server.application.bom.usecase.result.PreviewBomImportResult.Summary;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreviewBomImportUseCase {

    private static final int MAX_ROWS = 500;
    private static final List<String> REQUIRED_HEADERS = List.of(
            "line_number", "child_part_number", "child_revision_code", "quantity"
    );

    private final CurrentAuthProvider currentAuthProvider;
    private final FileRepository fileRepository;
    private final StoragePort storagePort;
    private final SpreadsheetParserSupport spreadsheetParserSupport;
    private final PartRevisionRepository partRevisionRepository;
    private final EngineeringBomItemRepository engineeringBomItemRepository;

    public PreviewBomImportResult execute(PreviewBomImportCommand command) {
        currentAuthProvider.getCurrentAuth();

        PartRevision parentRevision = partRevisionRepository.findByIdAndPartId(command.revisionId(), command.partId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "리비전을 찾을 수 없습니다"));
        if (parentRevision.getStatus() != PartRevisionStatus.DRAFT) {
            throw new AppException(ErrorCode.PRECONDITION_FAILED, "DRAFT 상태의 리비전만 BOM 가져오기가 가능합니다");
        }

        File file = fileRepository.findById(command.fileId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "파일을 찾을 수 없습니다"));
        if (file.getStatus() != FileStatus.UPLOADED) {
            throw new AppException(ErrorCode.PRECONDITION_FAILED, "업로드 완료된 파일만 사용할 수 있습니다");
        }

        byte[] content = storagePort.getObject(file.getFileKey());
        SpreadsheetParserSupport.ParsedSheet parsed = spreadsheetParserSupport.parse(
                content, file.getOriginalName(), null, MAX_ROWS
        );

        validateHeaders(parsed.headers());

        Set<String> existingLineNumbers = engineeringBomItemRepository
                .findByParentPartRevisionIdOrderByCreatedAtAsc(command.revisionId())
                .stream()
                .map(item -> item.getLineNumber())
                .collect(Collectors.toSet());

        List<RowResult> rows = new ArrayList<>();
        Set<String> fileLineNumbers = new HashSet<>();
        int successCount = 0;
        int errorCount = 0;
        int warningCount = 0;

        for (int i = 0; i < parsed.rows().size(); i++) {
            Map<String, Object> row = parsed.rows().get(i);
            int rowNumber = i + 2;

            String lineNumber = toStringValue(row.get("line_number"));
            String childPartNumber = toStringValue(row.get("child_part_number"));
            String childRevisionCode = toStringValue(row.get("child_revision_code"));
            BigDecimal quantity = toBigDecimalValue(row.get("quantity"));

            String errorMessage = validate(
                    lineNumber, childPartNumber, childRevisionCode, quantity,
                    fileLineNumbers, existingLineNumbers
            );

            if (errorMessage != null) {
                rows.add(new RowResult(rowNumber, lineNumber, childPartNumber, childRevisionCode, quantity, RowStatus.ERROR, errorMessage));
                errorCount++;
            } else {
                rows.add(new RowResult(rowNumber, lineNumber, childPartNumber, childRevisionCode, quantity, RowStatus.SUCCESS, null));
                successCount++;
            }

            if (lineNumber != null) {
                fileLineNumbers.add(lineNumber);
            }
        }

        return new PreviewBomImportResult(
                rows,
                new Summary(rows.size(), successCount, errorCount, warningCount)
        );
    }

    private void validateHeaders(List<String> headers) {
        List<String> normalized = headers.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();
        for (String required : REQUIRED_HEADERS) {
            if (!normalized.contains(required)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "필수 헤더 '%s'이(가) 누락되었습니다. 템플릿을 다운로드하여 사용해주세요".formatted(required));
            }
        }
    }

    private String validate(
            String lineNumber,
            String childPartNumber,
            String childRevisionCode,
            BigDecimal quantity,
            Set<String> fileLineNumbers,
            Set<String> existingLineNumbers
    ) {
        if (lineNumber == null || lineNumber.isBlank()) {
            return "줄 번호(line_number)는 필수입니다";
        }
        if (childPartNumber == null || childPartNumber.isBlank()) {
            return "하위 부품 번호(child_part_number)는 필수입니다";
        }
        if (childRevisionCode == null || childRevisionCode.isBlank()) {
            return "하위 리비전 코드(child_revision_code)는 필수입니다";
        }
        if (quantity == null) {
            return "수량(quantity)은 필수입니다";
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return "수량(quantity)은 0보다 커야 합니다";
        }
        if (fileLineNumbers.contains(lineNumber)) {
            return "줄 번호 '%s'이(가) 파일 내에서 중복됩니다".formatted(lineNumber);
        }
        if (existingLineNumbers.contains(lineNumber)) {
            return "줄 번호 '%s'이(가) 기존 BOM 항목과 중복됩니다".formatted(lineNumber);
        }

        Optional<PartRevision> childRevision = partRevisionRepository
                .findByPartNumberAndRevisionCode(childPartNumber, childRevisionCode);
        if (childRevision.isEmpty()) {
            return "부품 '%s' 리비전 '%s'을(를) 찾을 수 없습니다".formatted(childPartNumber, childRevisionCode);
        }

        return null;
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        String str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }

    private BigDecimal toBigDecimalValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
