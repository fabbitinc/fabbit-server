package com.fabbitinc.server.domain.aiusage.repository;

import com.fabbitinc.server.domain.aiusage.model.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {
}
