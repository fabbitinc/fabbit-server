package com.fabbitinc.server.domain.synthesis.repository;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SynthesisJobRepository extends JpaRepository<SynthesisJob, UUID> {

    List<SynthesisJob> findByBatchIdOrderByCreatedAtAsc(UUID batchId);

    List<SynthesisJob> findAllByOrderByCreatedAtDesc();

    @Query(
            value = """
                    select *
                    from synthesis_jobs
                    order by completed_at desc nulls last
                    limit 1
                    """,
            nativeQuery = true
    )
    List<SynthesisJob> findLatestOne();

    default Optional<SynthesisJob> findLatest() {
        List<SynthesisJob> jobs = findLatestOne();
        if (jobs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(jobs.getFirst());
    }
}
