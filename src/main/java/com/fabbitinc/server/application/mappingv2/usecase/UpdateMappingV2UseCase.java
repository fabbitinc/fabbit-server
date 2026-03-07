package com.fabbitinc.server.application.mappingv2.usecase;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.mapping.service.MappingService;
import com.fabbitinc.server.application.mapping.support.SpreadsheetParserSupport;
import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.service.MappingV2Service;
import com.fabbitinc.server.application.mappingv2.service.input.UpdateMappingV2Input;
import com.fabbitinc.server.application.mappingv2.service.output.SavedMappingV2Output;
import com.fabbitinc.server.application.mappingv2.support.MappingV2NormalizationSupport;
import com.fabbitinc.server.application.mappingv2.support.MappingV2ValidationSupport;
import com.fabbitinc.server.application.mappingv2.usecase.command.UpdateMappingV2Command;
import com.fabbitinc.server.application.mappingv2.usecase.result.SavedMappingV2Result;
import com.fabbitinc.server.application.mappingv2.usecase.support.SavedMappingV2ResultMapper;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdateMappingV2UseCase {

    private final MappingService mappingService;
    private final MappingV2Service mappingV2Service;
    private final MappingV2NormalizationSupport mappingV2NormalizationSupport;
    private final MappingV2ValidationSupport mappingV2ValidationSupport;
    private final SavedMappingV2ResultMapper savedMappingV2ResultMapper;

    public SavedMappingV2Result execute(UpdateMappingV2Command command) {
        var file = mappingService.getUploadedFileOrThrow(command.fileId());
        SpreadsheetParserSupport.ParsedSheet parsed = mappingService.loadHeadersAndRows(file, command.sheetName(), 30);

        MappingV2ResultDto normalized = mappingV2NormalizationSupport.normalize(command.mapping());
        MappingV2ValidationSupport.ValidationResult validation = mappingV2ValidationSupport.validateAgainstRows(
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

        SavedMappingV2Output output = mappingV2Service.updateMapping(command.mappingId(), new UpdateMappingV2Input(
                command.name(),
                command.fileId(),
                command.sheetName(),
                parsed.headers(),
                normalized
        ));
        return savedMappingV2ResultMapper.toResult(output.record(), output.revision());
    }
}
