package com.fabbitinc.server.presentation.mapping.mapper;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import com.fabbitinc.server.application.mapping.model.PropertyMappingDto;
import com.fabbitinc.server.application.mapping.model.RelationMappingDto;
import com.fabbitinc.server.application.mapping.query.result.MappingBodyResult;
import com.fabbitinc.server.application.mapping.query.result.MappingListResult;
import com.fabbitinc.server.application.mapping.query.result.MappingResult;
import com.fabbitinc.server.application.mapping.query.result.PropertyMappingResult;
import com.fabbitinc.server.application.mapping.query.result.RelationMappingResult;
import com.fabbitinc.server.application.mapping.usecase.result.MappingImpactSummaryResult;
import com.fabbitinc.server.application.mapping.usecase.result.MappingValidationIssueResult;
import com.fabbitinc.server.application.mapping.usecase.result.PreviewMappingResult;
import com.fabbitinc.server.application.mapping.usecase.result.PreviewSheetResult;
import com.fabbitinc.server.application.mapping.usecase.result.SavedMappingResult;
import com.fabbitinc.server.application.mapping.usecase.result.SkippedSheetResult;
import com.fabbitinc.server.application.mapping.usecase.result.ValidatedMappingResult;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingImpactSummaryResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingListResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingPreviewResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.MappingValidateResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.SheetPreviewResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.SkippedSheetResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.ValidationIssueResponse;
import com.fabbitinc.server.presentation.mapping.dto.response.ValidationSeverity;
import java.util.List;

public final class MappingResponseMapper {

    private MappingResponseMapper() {
    }

    public static MappingListResponse toMappingListResponse(MappingListResult result) {
        return new MappingListResponse(
                result.items().stream()
                        .map(MappingResponseMapper::toMappingResponse)
                        .toList()
        );
    }

    public static MappingResponse toMappingResponse(MappingResult result) {
        return new MappingResponse(
                result.id(),
                result.fileId(),
                result.name(),
                result.sheetName(),
                result.originalHeaders(),
                result.mappedHeaders(),
                toMappingResultDto(result.mapping()),
                result.scope(),
                result.active(),
                result.usageCount(),
                result.version(),
                result.createdAt()
        );
    }

    public static MappingResponse toMappingResponse(SavedMappingResult result) {
        return new MappingResponse(
                result.id(),
                result.fileId(),
                result.name(),
                result.sheetName(),
                result.originalHeaders(),
                result.mappedHeaders(),
                result.mapping(),
                result.scope(),
                result.active(),
                result.usageCount(),
                result.version(),
                result.createdAt()
        );
    }

    public static MappingPreviewResponse toMappingPreviewResponse(PreviewMappingResult result) {
        return new MappingPreviewResponse(
                result.headers(),
                result.sampleRows(),
                result.mapping(),
                result.sheets().stream()
                        .map(MappingResponseMapper::toSheetPreviewResponse)
                        .toList(),
                result.skippedSheets().stream()
                        .map(MappingResponseMapper::toSkippedSheetResponse)
                        .toList()
        );
    }

    public static MappingValidateResponse toMappingValidateResponse(ValidatedMappingResult result) {
        return new MappingValidateResponse(
                result.normalizedMapping(),
                result.errors().stream()
                        .map(MappingResponseMapper::toValidationIssueResponse)
                        .toList(),
                result.warnings().stream()
                        .map(MappingResponseMapper::toValidationIssueResponse)
                        .toList(),
                toMappingImpactSummaryResponse(result.impactSummary())
        );
    }

    private static SheetPreviewResponse toSheetPreviewResponse(PreviewSheetResult result) {
        return new SheetPreviewResponse(
                result.sheetName(),
                result.headers(),
                result.sampleRows(),
                result.mapping()
        );
    }

    private static SkippedSheetResponse toSkippedSheetResponse(SkippedSheetResult result) {
        return new SkippedSheetResponse(result.sheetName(), result.reason());
    }

    private static ValidationIssueResponse toValidationIssueResponse(MappingValidationIssueResult result) {
        return new ValidationIssueResponse(
                result.code(),
                ValidationSeverity.from(result.severity()),
                result.message(),
                result.path(),
                result.dismissedReason()
        );
    }

    private static MappingImpactSummaryResponse toMappingImpactSummaryResponse(MappingImpactSummaryResult result) {
        return new MappingImpactSummaryResponse(result.disabledColumnCount());
    }

    private static MappingResultDto toMappingResultDto(MappingBodyResult result) {
        if (result == null) {
            return new MappingResultDto(List.of(), List.of());
        }
        return new MappingResultDto(
                result.propertyMappings().stream()
                        .map(MappingResponseMapper::toPropertyMappingDto)
                        .toList(),
                result.relationMappings().stream()
                        .map(MappingResponseMapper::toRelationMappingDto)
                        .toList()
        );
    }

    private static PropertyMappingDto toPropertyMappingDto(PropertyMappingResult result) {
        return new PropertyMappingDto(
                result.sourceColumn(),
                result.targetProperty(),
                result.suggestedExtendedProperty(),
                result.dataType(),
                result.confidence(),
                result.reason(),
                result.isExtended()
        );
    }

    private static RelationMappingDto toRelationMappingDto(RelationMappingResult result) {
        return new RelationMappingDto(
                result.relType(),
                result.targetLabel(),
                result.nodeColumns(),
                result.relColumns(),
                result.relColumnTypes(),
                result.confidence(),
                result.reason()
        );
    }
}
