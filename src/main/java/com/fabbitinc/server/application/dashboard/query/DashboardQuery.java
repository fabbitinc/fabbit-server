package com.fabbitinc.server.application.dashboard.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.dashboard.query.condition.DashboardStatsCondition;
import com.fabbitinc.server.application.dashboard.query.result.DashboardBomStatsResult;
import com.fabbitinc.server.application.dashboard.query.result.DashboardLastSynthesisResult;
import com.fabbitinc.server.application.dashboard.query.result.DashboardPartStatsResult;
import com.fabbitinc.server.application.dashboard.query.result.DashboardStatsResult;
import com.fabbitinc.server.domain.part.repository.BomLinkRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRepository partRepository;
    private final BomLinkRepository bomLinkRepository;
    private final EntityManager entityManager;

    public DashboardStatsResult get(DashboardStatsCondition condition) {
        currentAuthProvider.getCurrentAuth();

        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        int totalParts = safeToInt(partRepository.count());
        int addedThisWeek = safeToInt(partRepository.countByCreatedAtGreaterThanEqual(since));
        int totalBomLinks = safeToInt(bomLinkRepository.count());

        DashboardLastSynthesisResult lastSynthesis = findLatestSynthesisJob()
                .map(this::toLastSynthesisResponse)
                .orElse(null);

        return new DashboardStatsResult(
                new DashboardPartStatsResult(totalParts, addedThisWeek),
                new DashboardBomStatsResult(totalBomLinks),
                lastSynthesis
        );
    }

    private DashboardLastSynthesisResult toLastSynthesisResponse(SynthesisJob job) {
        return new DashboardLastSynthesisResult(
                job.getId(),
                job.getStatus(),
                job.getCompletedAt(),
                job.getNodesCreated(),
                job.getRelationshipsCreated()
        );
    }

    private int safeToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

    private java.util.Optional<SynthesisJob> findLatestSynthesisJob() {
        PathBuilder<SynthesisJob> job = new PathBuilder<>(SynthesisJob.class, "synthesisJob");
        SynthesisJob latest = queryFactory()
                .selectFrom(job)
                .orderBy(job.getDateTime("completedAt", Instant.class).desc().nullsLast())
                .limit(1)
                .fetchOne();
        return java.util.Optional.ofNullable(latest);
    }

    private JPAQueryFactory queryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
