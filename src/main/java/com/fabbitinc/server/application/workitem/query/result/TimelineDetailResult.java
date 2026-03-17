package com.fabbitinc.server.application.workitem.query.result;

import java.util.List;
import java.util.Map;

public record TimelineDetailResult(
        Map<String, TimelineValueChangeResult> changes,
        List<TimelineRefResult> refs,
        List<TimelineRefResult> added,
        List<TimelineRefResult> removed
) {
}
