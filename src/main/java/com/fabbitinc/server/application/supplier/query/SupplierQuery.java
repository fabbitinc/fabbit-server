package com.fabbitinc.server.application.supplier.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.supplier.query.condition.SupplierListCondition;
import com.fabbitinc.server.application.supplier.query.result.SupplierListResult;
import com.fabbitinc.server.application.supplier.query.result.SupplierSummaryResult;
import com.fabbitinc.server.domain.supplier.model.Supplier;
import com.fabbitinc.server.domain.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final SupplierRepository supplierRepository;

    public SupplierListResult list(SupplierListCondition condition) {
        currentAuthProvider.getCurrentAuth();
        String normalizedSearch = normalizeSearch(condition.search());

        List<Supplier> suppliers = supplierRepository.listSuppliersPaginated(
                normalizedSearch,
                condition.offset(),
                condition.limit()
        );
        long total = supplierRepository.countSuppliers(normalizedSearch);

        List<SupplierSummaryResult> items = suppliers.stream()
                .map(supplier -> new SupplierSummaryResult(
                        supplier.getId(),
                        supplier.getCompanyName(),
                        supplier.getCode(),
                        supplier.getCountry()
                ))
                .toList();

        return new SupplierListResult(total, condition.offset(), condition.limit(), items);
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
