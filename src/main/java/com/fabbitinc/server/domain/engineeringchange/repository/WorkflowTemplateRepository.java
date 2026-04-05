package com.fabbitinc.server.domain.engineeringchange.repository;

import com.fabbitinc.server.domain.engineeringchange.model.WorkflowTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {

    List<WorkflowTemplate> findAllByOrderByCreatedAtDesc();
}
