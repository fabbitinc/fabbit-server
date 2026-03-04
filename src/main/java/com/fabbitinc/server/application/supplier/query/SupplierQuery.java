package com.fabbitinc.server.application.supplier.query;

import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.supplier.dto.response.SupplierListResponse;
import com.fabbitinc.server.application.supplier.dto.response.SupplierSummaryResponse;
import com.fabbitinc.server.domain.supplier.model.Supplier;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SupplierQuery {

    private final AuthTokenParser authTokenParser;
    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public SupplierListResponse listSuppliers(
            String authorizationHeader,
            String search,
            int offset,
            int limit
    ) {
        authTokenParser.requireAuth(authorizationHeader);
        String normalizedSearch = normalizeSearch(search);

        List<Supplier> suppliers = supplierRepository.listSuppliersPaginated(normalizedSearch, offset, limit);
        long total = supplierRepository.countSuppliers(normalizedSearch);

        List<SupplierSummaryResponse> items = suppliers.stream()
                .map(supplier -> new SupplierSummaryResponse(
                        supplier.getId(),
                        supplier.getCompanyName(),
                        supplier.getCode(),
                        supplier.getCountry()
                ))
                .toList();

        return new SupplierListResponse(total, offset, limit, items);
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
