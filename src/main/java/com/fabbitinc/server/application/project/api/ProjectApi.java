package com.fabbitinc.server.application.project.api;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.project.query.ProjectQuery;
import com.fabbitinc.server.application.project.query.condition.PartProjectsCondition;
import com.fabbitinc.server.application.project.query.result.PartProjectsResult;
import com.fabbitinc.server.domain.project.model.ProjectPart;
import com.fabbitinc.server.domain.project.repository.ProjectPartRepository;
import com.fabbitinc.server.domain.project.repository.ProjectRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectApi {

    private final ProjectQuery projectQuery;
    private final ProjectRepository projectRepository;
    private final ProjectPartRepository projectPartRepository;

    public PartProjectsResult listPartProjects(UUID partId) {
        return projectQuery.listPartProjects(new PartProjectsCondition(partId));
    }

    public long countPartProjects(UUID partId) {
        return listPartProjects(partId).total();
    }

    public void validateProjectId(UUID projectId) {
        if (projectRepository.findByIdAndDeletedFalse(projectId).isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND, "Project '" + projectId + "'을(를) 찾을 수 없습니다");
        }
    }

    public Set<UUID> getProjectPartIds(UUID projectId) {
        return projectPartRepository.findByProjectId(projectId).stream()
                .map(ProjectPart::getPartId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
