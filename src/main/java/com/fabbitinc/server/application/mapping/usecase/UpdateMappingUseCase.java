package com.fabbitinc.server.application.mapping.usecase;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.service.input.UpdateMappingInput;
import com.fabbitinc.server.application.mapping.service.output.SavedMappingOutput;
import com.fabbitinc.server.application.mapping.support.MappingNormalizationSupport;
import com.fabbitinc.server.application.mapping.support.MappingValidationSupport;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mapping.usecase.command.UpdateMappingCommand;
import com.fabbitinc.server.application.mapping.usecase.result.SavedMappingResult;
import com.fabbitinc.server.application.mapping.usecase.support.SavedMappingResultMapper;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdateMappingUseCase {

    private final MappingService mappingService;
    private final MappingNormalizationSupport mappingNormalizationSupport;
    private final MappingValidationSupport mappingValidationSupport;
    private final SavedMappingResultMapper savedMappingResultMapper;

    public SavedMappingResult execute(UpdateMappingCommand command) {
        var file = mappingService.getUploadedFileOrThrow(command.fileId());
        SpreadsheetParserSupport.ParsedSheet parsed = mappingService.loadHeadersAndRows(file, command.sheetName(), 30);

        MappingResultDto normalized = mappingNormalizationSupport.normalize(command.mapping());
        MappingValidationSupport.ValidationResult validation = mappingValidationSupport.validateAgainstRows(
                parsed.headers(),
                parsed.rows(),
                normalized
        );

        if (!validation.errors().isEmpty()) {
            String detail = validation.errors().stream()
                    .limit(3)
                    .map(issue -> issue.message())
                    .collect(Collectors.joining("; "));
            throw new AppException(ErrorCode.VALIDATION_ERROR, "매핑 검증에 실패했습니다: " + detail);
        }

        SavedMappingOutput output = mappingService.updateMapping(
                command.mappingId(),
                new UpdateMappingInput(command.name(), command.fileId(), command.sheetName(), normalized)
        );
        return savedMappingResultMapper.toResult(output.record(), output.revision());
    }
}
