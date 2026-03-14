package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowPolicy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRevisionWorkflowPolicyRepository extends JpaRepository<PartRevisionWorkflowPolicy, UUID> {

    Optional<PartRevisionWorkflowPolicy> findByPolicyKey(String policyKey);
}
