package com.fabbitinc.server.application.project.api;

import com.fabbitinc.server.application.project.query.ProjectQuery;
import com.fabbitinc.server.application.project.query.condition.PartProjectsCondition;
import com.fabbitinc.server.application.project.query.result.PartProjectsResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectApi {

    private final ProjectQuery projectQuery;

    public PartProjectsResult listPartProjects(UUID partId) {
        return projectQuery.listPartProjects(new PartProjectsCondition(partId));
    }

    public long countPartProjects(UUID partId) {
        return listPartProjects(partId).total();
    }
}
