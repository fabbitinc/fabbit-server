package com.fabbitinc.server.application.dashboard.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.dashboard.dto.response.BomStatsResponse;
import com.fabbitinc.server.application.dashboard.dto.response.DashboardStatsResponse;
import com.fabbitinc.server.application.dashboard.dto.response.LastSynthesisResponse;
import com.fabbitinc.server.application.dashboard.dto.response.PartStatsResponse;
import com.fabbitinc.server.domain.part.repository.BomLinkRepository;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import com.fabbitinc.server.domain.synthesis.repository.SynthesisJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class DashboardQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final PartRepository partRepository;
    private final BomLinkRepository bomLinkRepository;
    private final SynthesisJobRepository synthesisJobRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        currentAuthProvider.getCurrentAuth();

        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        int totalParts = safeToInt(partRepository.count());
        int addedThisWeek = safeToInt(partRepository.countByCreatedAtGreaterThanEqual(since));
        int totalBomLinks = safeToInt(bomLinkRepository.count());

        LastSynthesisResponse lastSynthesis = synthesisJobRepository.findLatest()
                .map(this::toLastSynthesisResponse)
                .orElse(null);

        return new DashboardStatsResponse(
                new PartStatsResponse(totalParts, addedThisWeek),
                new BomStatsResponse(totalBomLinks),
                lastSynthesis
        );
    }

    private LastSynthesisResponse toLastSynthesisResponse(SynthesisJob job) {
        return new LastSynthesisResponse(
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
}
