package com.fabbitinc.server.domain.aiusage.repository;

import com.fabbitinc.server.domain.aiusage.model.AiUsageLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {
}
