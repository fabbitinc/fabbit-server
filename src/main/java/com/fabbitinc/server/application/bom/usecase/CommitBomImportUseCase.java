package com.fabbitinc.server.application.bom.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.service.EngineeringBomService;
import com.fabbitinc.server.application.bom.service.input.AddBomItemsBatchInput;
import com.fabbitinc.server.application.bom.usecase.command.CommitBomImportCommand;
import com.fabbitinc.server.application.bom.usecase.command.CommitBomImportCommand.BomImportMode;
import com.fabbitinc.server.application.bom.usecase.result.CommitBomImportResult;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.file.port.StoragePort;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
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
@Transactional
public class CommitBomImportUseCase {

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
    private final EngineeringBomService engineeringBomService;

    public CommitBomImportResult execute(CommitBomImportCommand command) {
        currentAuthProvider.getCurrentAuth();

        PartRevision parentRevision = partRevisionRepository.findById(command.revisionId())
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

        Set<String> existingLineNumbers = command.mode() == BomImportMode.REPLACE
                ? Set.of()
                : engineeringBomItemRepository
                        .findByParentPartRevisionIdOrderByCreatedAtAsc(command.revisionId())
                        .stream()
                        .map(EngineeringBomItem::getLineNumber)
                        .collect(Collectors.toSet());

        List<AddBomItemsBatchInput.Item> items = new ArrayList<>();
        Set<String> fileLineNumbers = new HashSet<>();

        for (int i = 0; i < parsed.rows().size(); i++) {
            Map<String, Object> row = parsed.rows().get(i);
            int rowNumber = i + 2;

            String lineNumber = toStringValue(row.get("line_number"));
            String childPartNumber = toStringValue(row.get("child_part_number"));
            String childRevisionCode = toStringValue(row.get("child_revision_code"));
            BigDecimal quantity = toBigDecimalValue(row.get("quantity"));

            String errorMessage = validate(
                    rowNumber, lineNumber, childPartNumber, childRevisionCode, quantity,
                    fileLineNumbers, existingLineNumbers
            );

            if (errorMessage != null) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, errorMessage);
            }

            Optional<PartRevision> childRevision = partRevisionRepository
                    .findByPartNumberAndRevisionCode(childPartNumber, childRevisionCode);
            if (childRevision.isEmpty()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "%d행: 부품 '%s' 리비전 '%s'을(를) 찾을 수 없습니다".formatted(rowNumber, childPartNumber, childRevisionCode));
            }

            items.add(new AddBomItemsBatchInput.Item(
                    childRevision.get().getId(),
                    lineNumber,
                    quantity,
                    Map.of()
            ));

            if (lineNumber != null) {
                fileLineNumbers.add(lineNumber);
            }
        }

        if (command.mode() == BomImportMode.REPLACE) {
            engineeringBomItemRepository.deleteByParentPartRevisionId(command.revisionId());
        }

        List<EngineeringBomItem> created = engineeringBomService.addBomItemsBatch(
                new AddBomItemsBatchInput(command.partId(), command.revisionId(), items)
        );

        return new CommitBomImportResult(
                created.stream().map(EngineeringBomItem::getId).toList(),
                new CommitBomImportResult.Summary(created.size(), command.mode())
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
            int rowNumber,
            String lineNumber,
            String childPartNumber,
            String childRevisionCode,
            BigDecimal quantity,
            Set<String> fileLineNumbers,
            Set<String> existingLineNumbers
    ) {
        if (lineNumber == null || lineNumber.isBlank()) {
            return "%d행: 줄 번호(line_number)는 필수입니다".formatted(rowNumber);
        }
        if (childPartNumber == null || childPartNumber.isBlank()) {
            return "%d행: 하위 부품 번호(child_part_number)는 필수입니다".formatted(rowNumber);
        }
        if (childRevisionCode == null || childRevisionCode.isBlank()) {
            return "%d행: 하위 리비전 코드(child_revision_code)는 필수입니다".formatted(rowNumber);
        }
        if (quantity == null) {
            return "%d행: 수량(quantity)은 필수입니다".formatted(rowNumber);
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return "%d행: 수량(quantity)은 0보다 커야 합니다".formatted(rowNumber);
        }
        if (fileLineNumbers.contains(lineNumber)) {
            return "%d행: 줄 번호 '%s'이(가) 파일 내에서 중복됩니다".formatted(rowNumber, lineNumber);
        }
        if (existingLineNumbers.contains(lineNumber)) {
            return "%d행: 줄 번호 '%s'이(가) 기존 BOM 항목과 중복됩니다".formatted(rowNumber, lineNumber);
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
