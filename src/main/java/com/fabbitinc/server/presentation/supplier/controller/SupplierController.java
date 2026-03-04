package com.fabbitinc.server.presentation.supplier.controller;

import com.fabbitinc.server.application.supplier.dto.response.SupplierListResponse;
import com.fabbitinc.server.application.supplier.query.SupplierQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
    @GetMapping
    public SupplierListResponse listSuppliers(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다")
            int offset,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 100, message = "limit은 100 이하여야 합니다")
            int limit
    ) {
        return supplierQuery.listSuppliers(authorizationHeader, search, offset, limit);
    }
}
