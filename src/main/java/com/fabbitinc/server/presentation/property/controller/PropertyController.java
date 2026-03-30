package com.fabbitinc.server.presentation.property.controller;

import com.fabbitinc.server.application.property.query.PropertyQuery;
import com.fabbitinc.server.application.property.query.condition.PropertyMetaCondition;
import com.fabbitinc.server.application.property.query.condition.PropertyMetaListCondition;
import com.fabbitinc.server.application.property.query.result.PropertyMetaListResult;
import com.fabbitinc.server.application.property.query.result.PropertyMetaResult;
import com.fabbitinc.server.application.property.query.result.PropertyOptionResult;
import com.fabbitinc.server.application.property.usecase.CreatePropertyDefinitionUseCase;
import com.fabbitinc.server.application.property.usecase.DeletePropertyDefinitionUseCase;
import com.fabbitinc.server.application.property.usecase.ReorderPropertyUseCase;
import com.fabbitinc.server.application.property.usecase.UpdatePropertyDefinitionUseCase;
import com.fabbitinc.server.application.property.usecase.command.CreatePropertyDefinitionCommand;
import com.fabbitinc.server.application.property.usecase.command.DeletePropertyDefinitionCommand;
import com.fabbitinc.server.application.property.usecase.command.PropertyOptionCommandItem;
import com.fabbitinc.server.application.property.usecase.command.ReorderPropertyCommand;
import com.fabbitinc.server.application.property.usecase.command.ReorderPropertyCommandItem;
import com.fabbitinc.server.application.property.usecase.command.UpdatePropertyDefinitionCommand;
import com.fabbitinc.server.application.property.usecase.result.CreatePropertyDefinitionResult;
import com.fabbitinc.server.application.property.usecase.result.UpdatePropertyDefinitionResult;
import com.fabbitinc.server.presentation.property.request.CreatePropertyDefinitionRequest;
import com.fabbitinc.server.presentation.property.request.PropertyOptionRequest;
import com.fabbitinc.server.presentation.property.request.ReorderPropertyRequest;
import com.fabbitinc.server.presentation.property.request.UpdatePropertyDefinitionRequest;
import com.fabbitinc.server.presentation.property.response.PropertyMetaListResponse;
import com.fabbitinc.server.presentation.property.response.PropertyMetaResponse;
import com.fabbitinc.server.presentation.property.response.PropertyOptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties")
@Tag(name = "properties", description = "통합 property catalog 관리 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PropertyController {

    private final PropertyQuery propertyQuery;
    private final CreatePropertyDefinitionUseCase createPropertyDefinitionUseCase;
    private final DeletePropertyDefinitionUseCase deletePropertyDefinitionUseCase;
    private final ReorderPropertyUseCase reorderPropertyUseCase;
    private final UpdatePropertyDefinitionUseCase updatePropertyDefinitionUseCase;

    @Operation(operationId = "propertyListMeta", summary = "속성 메타 목록을 조회합니다", description = "시스템 속성과 커스텀 속성을 통합한 최종 property catalog 목록을 조회합니다")
    @GetMapping("/meta")
    public PropertyMetaListResponse listMeta(
            @Parameter(description = "속성 소유 타입", example = "PART")
            @RequestParam("owner_type") String ownerType,
            @Parameter(description = "비활성 속성 포함 여부", example = "false")
            @RequestParam(value = "include_inactive", defaultValue = "false") boolean includeInactive
    ) {
        return toPropertyMetaListResponse(propertyQuery.list(new PropertyMetaListCondition(ownerType, includeInactive)));
    }

    @Operation(operationId = "propertyCreatePropertyDefinition", summary = "커스텀 속성을 생성합니다", description = "조직 관리자 권한으로 커스텀 속성 정의를 생성합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "409", description = "리소스 충돌"),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/definitions")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyMetaResponse createPropertyDefinition(@Valid @RequestBody CreatePropertyDefinitionRequest request) {
        CreatePropertyDefinitionResult result = createPropertyDefinitionUseCase.execute(
                new CreatePropertyDefinitionCommand(
                        request.ownerType(),
                        request.displayName(),
                        request.description(),
                        request.valueType(),
                        request.optionMode(),
                        toOptionItems(request.options()),
                        request.displayOrder() == null ? 0 : request.displayOrder(),
                        Boolean.TRUE.equals(request.required())
                )
        );
        return toPropertyMetaResponse(propertyQuery.get(new PropertyMetaCondition(
                result.ownerType(),
                result.propertyKey(),
                true
        )));
    }

    @Operation(operationId = "propertyUpdatePropertyDefinition", summary = "속성을 수정합니다", description = "조직 관리자 권한으로 시스템/커스텀 속성 정의를 부분 수정합니다")
    @PatchMapping("/definitions/{ownerType}/{propertyKey}")
    public PropertyMetaResponse updatePropertyDefinition(
            @Parameter(description = "속성 소유 타입", example = "PART")
            @PathVariable String ownerType,
            @Parameter(description = "수정할 속성 key", example = "material")
            @PathVariable String propertyKey,
            @Valid @RequestBody UpdatePropertyDefinitionRequest request
    ) {
        UpdatePropertyDefinitionResult result = updatePropertyDefinitionUseCase.execute(
                new UpdatePropertyDefinitionCommand(
                        ownerType,
                        propertyKey,
                        request.getDisplayName(),
                        request.isDisplayNameSet(),
                        request.getDescription(),
                        request.isDescriptionSet(),
                        request.getValueType(),
                        request.isValueTypeSet(),
                        request.getOptionMode(),
                        request.isOptionModeSet(),
                        toOptionItems(request.getOptions()),
                        request.isOptionsSet(),
                        request.getDisplayOrder(),
                        request.isDisplayOrderSet(),
                        request.getRequired(),
                        request.isRequiredSet(),
                        request.getActive(),
                        request.isActiveSet()
                )
        );
        return toPropertyMetaResponse(propertyQuery.get(new PropertyMetaCondition(
                result.ownerType(),
                result.propertyKey(),
                true
        )));
    }

    @Operation(operationId = "propertyDeletePropertyDefinition", summary = "속성을 삭제합니다", description = "조직 관리자 권한으로 커스텀 속성 정의를 삭제합니다. 시스템 속성은 삭제할 수 없습니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "409", description = "사용 중인 속성이라 삭제 불가")
    })
    @DeleteMapping("/definitions/{ownerType}/{propertyKey}")
    public ResponseEntity<Void> deletePropertyDefinition(
            @Parameter(description = "속성 소유 타입", example = "PART")
            @PathVariable String ownerType,
            @Parameter(description = "삭제할 속성 key", example = "019d0000-0000-7000-8000-000000000001")
            @PathVariable String propertyKey
    ) {
        deletePropertyDefinitionUseCase.execute(new DeletePropertyDefinitionCommand(ownerType, propertyKey));
        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "propertyReorder", summary = "속성 순서를 변경합니다", description = "조직 관리자 권한으로 속성의 최종 순서를 한 번에 변경합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "순서 변경 성공")
    })
    @PatchMapping("/order")
    public ResponseEntity<Void> reorder(
            @Parameter(description = "속성 순서 변경 요청")
            @Valid @RequestBody ReorderPropertyRequest request
    ) {
        reorderPropertyUseCase.execute(new ReorderPropertyCommand(
                request.ownerType(),
                request.properties().stream()
                        .map(property -> new ReorderPropertyCommandItem(
                                property.propertyKey(),
                                Boolean.TRUE.equals(property.system())
                        ))
                        .toList()
        ));
        return ResponseEntity.noContent().build();
    }

    private PropertyMetaListResponse toPropertyMetaListResponse(PropertyMetaListResult result) {
        return new PropertyMetaListResponse(
                result.total(),
                result.items().stream().map(this::toPropertyMetaResponse).toList()
        );
    }

    private PropertyMetaResponse toPropertyMetaResponse(PropertyMetaResult result) {
        return new PropertyMetaResponse(
                result.definitionId(),
                result.ownerType(),
                result.propertyKey(),
                result.system(),
                result.partSystemPropertyKind(),
                result.activeConfigurable(),
                result.columnName(),
                result.displayName(),
                result.description(),
                result.valueType(),
                result.optionMode(),
                result.options().stream().map(this::toPropertyOptionResponse).toList(),
                result.displayOrder(),
                result.required(),
                result.active()
        );
    }

    private PropertyOptionResponse toPropertyOptionResponse(PropertyOptionResult result) {
        return new PropertyOptionResponse(
                result.value(),
                result.label(),
                result.displayOrder(),
                result.active()
        );
    }

    private List<PropertyOptionCommandItem> toOptionItems(List<PropertyOptionRequest> requests) {
        if (requests == null) {
            return null;
        }
        return requests.stream()
                .map(request -> new PropertyOptionCommandItem(
                        request.value(),
                        request.label(),
                        request.displayOrder(),
                        request.active()
                ))
                .toList();
    }
}
