# 7. 테스트 전략(Deterministic vs LLM E2E)

## A) 항상 실행(Deterministic) — CI 필수
1) 컬럼 프로파일링 테스트
- 통화/단위/숫자 타입 추정이 규칙대로 동작하는지

2) 후보 생성/랭킹 안정성 테스트
- 동일 입력 → 동일 후보/점수(랜덤 금지)
- golden set 기반 top-1/top-k 회귀

3) validator severity 테스트
- 케이스별 ERROR/WARNING/INFO가 의도대로 나오는지
- 특히 “관계 속성은 있는데 endpoint 없음”은 반드시 ERROR 처리(정책에 따라)

4) JSON Patch 적용 테스트(RFC 6902)
- patch 적용 후 normalized_mapping이 기대 구조인지
- invalid patch는 안전하게 거부되는지
- RFC 6902: https://datatracker.ietf.org/doc/html/rfc6902

## B) 주기 실행(LLM E2E) — 야간/주간, 비용 통제
1) 고정 데이터셋 + 고정 프롬프트 + 결과 스냅샷
- ext_rate 감소
- SUPPLIED_BY 누락률 감소
- suggestion 수락률(사용자 적용률)

2) 드리프트 감지
- 모델/프롬프트 변경 시 결과 분포 급변 알림

3) 리플레이 기반 회귀
- 과거 LLM 응답을 저장(또는 해시/요약)하여 회귀에 활용
- 개인정보/기밀 데이터는 마스킹/비식별화 정책 적용
