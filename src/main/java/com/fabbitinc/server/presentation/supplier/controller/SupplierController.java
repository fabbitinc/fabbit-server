package com.fabbitinc.server.presentation.supplier.controller;

import com.fabbitinc.server.application.supplier.dto.response.SupplierListResponse;
import com.fabbitinc.server.application.supplier.dto.response.SupplierSummaryResponse;
import com.fabbitinc.server.application.supplier.query.SupplierQuery;
import com.fabbitinc.server.application.supplier.query.condition.SupplierListCondition;
import com.fabbitinc.server.application.supplier.query.result.SupplierListResult;
import com.fabbitinc.server.application.supplier.query.result.SupplierSummaryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/suppliers")
@Tag(name = "suppliers", description = "공급사 조회 API")
public class SupplierController {

    private final SupplierQuery supplierQuery;

    @Operation(
            summary = "GET /api/v1/suppliers",
            description = "company_name 또는 code로 공급사를 검색해 페이징 목록을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping
    public SupplierListResponse listSuppliers(
            @Parameter(description = "공급사 검색어", example = "Samsung")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "페이지 오프셋", example = "0")
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다") int offset,
            @Parameter(description = "조회 건수", example = "20")
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 100, message = "limit은 100 이하여야 합니다") int limit
    ) {
        return toSupplierListResponse(supplierQuery.list(new SupplierListCondition(search, offset, limit)));
    }

    private SupplierListResponse toSupplierListResponse(SupplierListResult result) {
        return new SupplierListResponse(
                result.total(),
                result.offset(),
                result.limit(),
                result.items().stream()
                        .map(this::toSupplierSummaryResponse)
                        .toList()
        );
    }

    private SupplierSummaryResponse toSupplierSummaryResponse(SupplierSummaryResult result) {
        return new SupplierSummaryResponse(
                result.id(),
                result.companyName(),
                result.code(),
                result.country()
        );
    }
}
