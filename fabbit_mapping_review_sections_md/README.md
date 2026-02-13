# Fabbit 매핑 설계 리뷰(섹션별 Markdown)

이 압축파일은 아래 섹션별 문서로 구성됩니다.

- 1_root_causes.md: ext로 빠지는 근본 원인 분해
- 2_candidate_design.md: 하드코딩 없이 후보 제안/검증(ontology-driven) 설계
- 3_phase1_editability.md: Phase 1 제약 유지하며 사용자 편집 가능성 확대
- 4_api_schema_draft.md: candidate suggestions 포함 API 응답 스키마 초안
- 5_validate_severity_policy.md: validate severity 정책 권장안
- 6_rollout_plan.md: 단계적 롤아웃 계획
- 7_test_strategy.md: 테스트 전략(Deterministic vs LLM E2E)
- references.md: 참고 링크

## 사용 흐름(권장)
1) validate API에서 issues + suggestions + patch를 함께 반환
2) UI에서 suggestion을 선택하면 patch를 적용
3) 즉시 re-validate
4) confirm 저장은 WARNING까지 허용, synthesis는 ERROR 0일 때만 실행
