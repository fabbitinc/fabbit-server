package com.fabbitinc.server.application.project.api;

import com.fabbitinc.server.application.project.dto.response.PartProjectSummaryResponse;
import com.fabbitinc.server.application.project.dto.response.PartProjectsResponse;
import com.fabbitinc.server.application.project.query.ProjectQuery;
import com.fabbitinc.server.application.project.query.condition.PartProjectsCondition;
import com.fabbitinc.server.application.project.query.result.PartProjectsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProjectApi {

    private final ProjectQuery projectQuery;

    public PartProjectsResult listPartProjects(UUID partId) {
        return projectQuery.listPartProjects(new PartProjectsCondition(partId));
    }

    public PartProjectsResponse getPartProjects(UUID partId) {
        var result = listPartProjects(partId);
        return new PartProjectsResponse(
                result.total(),
                result.items().stream()
                        .map(item -> new PartProjectSummaryResponse(item.id(), item.name(), item.description()))
                        .toList()
        );
    }

    public long countPartProjects(UUID partId) {
        return listPartProjects(partId).total();
    }
}
