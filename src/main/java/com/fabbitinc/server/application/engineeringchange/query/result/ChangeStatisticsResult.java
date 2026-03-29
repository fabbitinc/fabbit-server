package com.fabbitinc.server.application.engineeringchange.query.result;

import java.util.List;
import java.util.UUID;

public record ChangeStatisticsResult(
        int totalReleasedCount,
        int monthlyReleasedCount,
        Double averageApprovalDaysOrNull,
        List<TopChangedPart> topChangedParts
) {

    public record TopChangedPart(
            UUID partId,
            String partNumber,
            String partName,
            int changeCount
    ) {
    }
}
