package com.fabbitinc.server.presentation.engineeringchange.controller;

import com.fabbitinc.server.application.engineeringchange.query.ChangeFeedQuery;
import com.fabbitinc.server.application.engineeringchange.query.condition.ChangeFeedCondition;
import com.fabbitinc.server.application.engineeringchange.query.result.ChangeFeedResult;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.ChangeFeedResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.ChangeFeedResponse.ChangeFeedItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/change-feed")
@Tag(name = "change-feed", description = "변경 피드 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class ChangeFeedController {

    private final ChangeFeedQuery changeFeedQuery;

    @Operation(
            summary = "변경 피드를 조회합니다",
            description = "릴리즈된 엔지니어링 변경 목록을 최신 순으로 조회합니다. partId를 지정하면 해당 파트에 영향을 준 변경만 반환합니다."
    )
    @GetMapping
    public ChangeFeedResponse list(
            @Parameter(description = "특정 파트에 대한 변경만 조회할 경우 파트 식별자")
            @RequestParam(required = false) UUID partId,
            @Parameter(description = "건너뛸 항목 수", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "조회할 항목 수", example = "20")
            @RequestParam(defaultValue = "20") int limit
    ) {
        ChangeFeedCondition condition = new ChangeFeedCondition(partId, offset, limit);
        ChangeFeedResult result = changeFeedQuery.listChangeFeed(condition);

        return new ChangeFeedResponse(
                result.items().stream()
                        .map(item -> new ChangeFeedItemResponse(
                                item.ecId(),
                                item.ecNumber(),
                                item.title(),
                                item.affectedPartNumbers(),
                                item.affectedPartCount(),
                                item.releasedAt(),
                                item.releasedById(),
                                item.releasedByName(),
                                item.sourceIssueNumber()
                        ))
                        .toList()
        );
    }
}
