package com.fabbitinc.server.application.activation.query;

import com.fabbitinc.server.application.activation.dto.response.StarterQuestionResponse;
import com.fabbitinc.server.application.activation.dto.response.StartersResponse;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ActivationQuery {

    private final AuthTokenParser authTokenParser;

    @Transactional(readOnly = true)
    public StartersResponse getStarters(String authorizationHeader) {
        authTokenParser.requireAuth(authorizationHeader);

        return new StartersResponse(List.of(
                new StarterQuestionResponse(
                        "전체 부품 목록을 보여줘",
                        "등록된 모든 부품의 품번과 품명을 조회합니다."
                ),
                new StarterQuestionResponse(
                        "BOM 구조를 보여줘. 상위 부품과 하위 부품의 관계를 알고 싶어",
                        "CONSISTS_OF 관계를 통해 BOM 트리 구조를 탐색합니다."
                ),
                new StarterQuestionResponse(
                        "공급사별로 납품하는 부품 목록을 보여줘",
                        "SUPPLIED_BY 관계를 통해 공급사-부품 매핑을 조회합니다."
                ),
                new StarterQuestionResponse(
                        "도면이 연결되지 않은 부품이 있어?",
                        "DEFINED_BY 관계가 없는 부품을 찾아 데이터 품질을 점검합니다."
                ),
                new StarterQuestionResponse(
                        "단가가 가장 높은 상위 5개 부품을 보여줘",
                        "SUPPLIED_BY 관계의 unit_cost 속성으로 고가 품목을 파악합니다."
                ),
                new StarterQuestionResponse(
                        "프로젝트별 부품 수를 알려줘",
                        "HAS_ITEM 관계를 집계하여 프로젝트 규모를 파악합니다."
                )
        ));
    }
}
