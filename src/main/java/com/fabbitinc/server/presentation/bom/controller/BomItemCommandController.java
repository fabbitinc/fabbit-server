package com.fabbitinc.server.presentation.bom.controller;

import com.fabbitinc.server.application.bom.usecase.AddBomItemUseCase;
import com.fabbitinc.server.application.bom.usecase.AddBomItemsBatchUseCase;
import com.fabbitinc.server.application.bom.usecase.DeleteBomItemUseCase;
import com.fabbitinc.server.application.bom.usecase.UpdateBomItemUseCase;
import com.fabbitinc.server.application.bom.usecase.command.AddBomItemCommand;
import com.fabbitinc.server.application.bom.usecase.command.AddBomItemsBatchCommand;
import com.fabbitinc.server.application.bom.usecase.command.DeleteBomItemCommand;
import com.fabbitinc.server.application.bom.usecase.command.UpdateBomItemCommand;
import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.PartBomCondition;
import com.fabbitinc.server.application.part.query.result.PartBomResult;
import com.fabbitinc.server.presentation.bom.request.AddBomItemRequest;
import com.fabbitinc.server.presentation.bom.request.AddBomItemsBatchRequest;
import com.fabbitinc.server.presentation.bom.request.UpdateBomItemRequest;
import com.fabbitinc.server.presentation.part.response.BomChildResponse;
import com.fabbitinc.server.presentation.part.response.PartBomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "bom-item-commands", description = "BOM 항목 생성/수정/삭제 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "리소스 충돌")
})
public class BomItemCommandController {

    private final PartQuery partQuery;
    private final AddBomItemUseCase addBomItemUseCase;
    private final UpdateBomItemUseCase updateBomItemUseCase;
    private final DeleteBomItemUseCase deleteBomItemUseCase;
    private final AddBomItemsBatchUseCase addBomItemsBatchUseCase;

    @Operation(operationId = "bomItemCommandAddBomItem", summary = "BOM 항목을 추가합니다", description = "DRAFT 상태 리비전에 BOM 항목을 추가합니다")
    @PostMapping("/{partId}/revisions/{revisionId}/bom-items")
    @ResponseStatus(HttpStatus.CREATED)
    public PartBomResponse addBomItem(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody AddBomItemRequest request
    ) {
        addBomItemUseCase.execute(new AddBomItemCommand(
                partId,
                revisionId,
                request.childPartRevisionId(),
                request.lineNumber(),
                request.quantity(),
                request.extendedProperties()
        ));
        return toBomResponse(partQuery.get(new PartBomCondition(partId, revisionId)));
    }

    @Operation(operationId = "bomItemCommandUpdateBomItem", summary = "BOM 항목을 수정합니다", description = "DRAFT 상태 리비전의 BOM 항목을 수정합니다")
    @PatchMapping("/{partId}/revisions/{revisionId}/bom-items/{bomItemId}")
    public PartBomResponse updateBomItem(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @PathVariable UUID bomItemId,
            @Valid @RequestBody UpdateBomItemRequest request
    ) {
        updateBomItemUseCase.execute(new UpdateBomItemCommand(
                partId,
                revisionId,
                bomItemId,
                request.getChildPartRevisionId(),
                request.isChildPartRevisionIdSet(),
                request.getLineNumber(),
                request.isLineNumberSet(),
                request.getQuantity(),
                request.isQuantitySet(),
                request.getExtendedProperties(),
                request.isExtendedPropertiesSet()
        ));
        return toBomResponse(partQuery.get(new PartBomCondition(partId, revisionId)));
    }

    @Operation(operationId = "bomItemCommandDeleteBomItem", summary = "BOM 항목을 삭제합니다", description = "DRAFT 상태 리비전의 BOM 항목을 삭제합니다")
    @DeleteMapping("/{partId}/revisions/{revisionId}/bom-items/{bomItemId}")
    public PartBomResponse deleteBomItem(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @PathVariable UUID bomItemId
    ) {
        deleteBomItemUseCase.execute(new DeleteBomItemCommand(partId, revisionId, bomItemId));
        return toBomResponse(partQuery.get(new PartBomCondition(partId, revisionId)));
    }

    @Operation(operationId = "bomItemCommandAddBomItemsBatch", summary = "BOM 항목을 일괄 추가합니다", description = "DRAFT 상태 리비전에 여러 BOM 항목을 한 번에 추가합니다")
    @PostMapping("/{partId}/revisions/{revisionId}/bom-items/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public PartBomResponse addBomItemsBatch(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody AddBomItemsBatchRequest request
    ) {
        addBomItemsBatchUseCase.execute(new AddBomItemsBatchCommand(
                partId,
                revisionId,
                request.items().stream()
                        .map(item -> new AddBomItemsBatchCommand.Item(
                                item.childPartRevisionId(),
                                item.lineNumber(),
                                item.quantity(),
                                item.extendedProperties()
                        ))
                        .toList()
        ));
        return toBomResponse(partQuery.get(new PartBomCondition(partId, revisionId)));
    }

    private PartBomResponse toBomResponse(PartBomResult result) {
        return new PartBomResponse(
                result.children().stream()
                        .map(item -> new BomChildResponse(
                                item.bomItemId(),
                                item.partId(),
                                item.revisionId(),
                                item.partNumber(),
                                item.name(),
                                item.revisionCode(),
                                item.revisionStatus(),
                                item.lineNumber(),
                                item.quantity(),
                                item.extendedProperties()
                        ))
                        .toList()
        );
    }
}
