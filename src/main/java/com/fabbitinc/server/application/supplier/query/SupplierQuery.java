package com.fabbitinc.server.application.supplier.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.supplier.query.condition.SupplierListCondition;
import com.fabbitinc.server.application.supplier.query.result.SupplierListResult;
import com.fabbitinc.server.application.supplier.query.result.SupplierSummaryResult;
import com.fabbitinc.server.domain.supplier.model.Supplier;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final EntityManager entityManager;

    public SupplierListResult list(SupplierListCondition condition) {
        currentAuthProvider.getCurrentAuth();
        String normalizedSearch = normalizeSearch(condition.search());
        PathBuilder<Supplier> supplier = new PathBuilder<>(Supplier.class, "supplier");
        var supplierIdExpr = supplier.get("id", UUID.class);
        var companyNameExpr = supplier.getString("companyName");
        var codeExpr = supplier.getString("code");

        BooleanBuilder predicate = new BooleanBuilder();
        if (normalizedSearch != null) {
            predicate.and(companyNameExpr.containsIgnoreCase(normalizedSearch)
                    .or(codeExpr.containsIgnoreCase(normalizedSearch)));
        }

        Long totalCount = queryFactory()
                .select(supplierIdExpr.count())
                .from(supplier)
                .where(predicate)
                .fetchOne();
        long total = totalCount == null ? 0L : totalCount;

        List<Supplier> suppliers = queryFactory()
                .selectFrom(supplier)
                .where(predicate)
                .orderBy(companyNameExpr.asc())
                .offset(condition.offset())
                .limit(condition.limit())
                .fetch();

        List<SupplierSummaryResult> items = suppliers.stream()
                .map(item -> new SupplierSummaryResult(
                        item.getId(),
                        item.getCompanyName(),
                        item.getCode(),
                        item.getCountry()
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

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
