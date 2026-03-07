# AI/그래프 도메인 마이그레이션 추적

## 범위

- 구 FastAPI 분석 대상
  - `../server/app/modules/activation/service.py`
  - `../server/app/modules/ontology/service.py`
  - `../server/app/modules/mapping/service.py`
  - `../server/app/modules/synthesis/service.py`
  - 합성 실제 실행 로직 확인을 위해 `../server/app/modules/synthesis/pipeline.py`도 함께 검토
- 신 Spring Boot 비교 대상
  - `src/main/java/com/fabbitinc/server/application/activation/**`
  - `src/main/java/com/fabbitinc/server/application/ontology/**`
  - `src/main/java/com/fabbitinc/server/application/mapping/**`
  - `src/main/java/com/fabbitinc/server/application/synthesis/**`
  - 관련 `presentation/**`, `domain/**`

## 요약

- 구 FastAPI 범위에는 `usecase.py` 파일이 없습니다. 현재 Spring Boot 쪽은 유스케이스/쿼리/서비스로 분해되어 있습니다.
- 매핑 생성, 정규화, 저장, 조회 API는 전반적으로 잘 이전되었습니다.
- 활성화(Activation) 도메인은 `health_check`, `starters`는 대응되지만, 자연어 그래프 질의는 LLM 기반 계획 실행에서 단순 키워드 조회로 축소되어 핵심 기능이 부분 이전 상태입니다.
- 합성(Synthesis)은 배치 시작/상태 조회는 이전되었지만, 실제 적재 엔진이 구 Python 파이프라인보다 좁습니다. 현재 구현은 `Part`, `Supplier`, `CONSISTS_OF`, `SUPPLIED_BY` 중심이며 `Drawing`, `DEFINED_BY`, `HAS_ITEM`, 기타 그래프 노드/관계 폴백이 빠져 있습니다.

## 함수 매핑

| 구 함수 | 신 구현 | 상태 | 근거 |
| --- | --- | --- | --- |
| `activation.health_check` | `application/activation/service/ActivationService.healthCheck` + `HealthCheckUseCase` | 완료 | 노드/관계 수 집계, orphan part, 미도면, 미공급사, 불완전 BOM 이슈 계산이 모두 존재합니다. 구현은 그래프 DB 직접 조회 대신 RDB 집계로 바뀌었지만 기능 의도는 유지됩니다. |
| `activation.query_graph` | `application/activation/service/ActivationService.queryGraph` + `QueryGraphUseCase` | 부분 | 구 버전은 LLM 기반 JSON query plan, SQL/graph/hybrid 실행, zero-result retry, 결과 요약, 확장 속성 힌트, 읽기 전용 검증을 수행했습니다. 현재는 `도면 미연결`, `공급사별 부품 수`, `프로젝트별 부품 수`, 일반 키워드 검색 4가지 휴리스틱 분기만 있어 질의 범위가 크게 축소되었습니다. |
| `activation.get_starters` | `application/activation/query/ActivationQuery.lookup` | 완료 | 정적 추천 질문 목록을 반환하는 역할이 그대로 존재합니다. 다만 일부 추천 질문은 현재 `queryGraph`가 실제로 처리하지 못할 가능성이 있습니다. |
| `ontology.generate_mapping` | `application/mapping/usecase/PreviewMappingUseCase` + `MappingLlmGenerationSupport.generate` + `MappingGenerationSupport.generate` | 완료 | LLM 기반 매핑 생성이 `mapping` 도메인으로 이동했고, 실패 시 휴리스틱 폴백까지 추가되었습니다. rootless relation, `_ext_` 확장 속성, 관계 속성 분리 규칙도 프롬프트/보조 로직에 남아 있습니다. |
| `ontology.normalize_mapping` | `application/mapping/support/MappingNormalizationSupport.normalize` | 완료 | 표준 Part 속성 검증, `_ext_` 정규화, 잘못된 관계 제거, 관계 속성 타입 보정, rootless relation 허용 규칙이 유지됩니다. |
| `mapping._determine_scope` | `application/mapping/service/MappingService.determineScope` | 완료 | relation 존재 여부와 merge key 충족 여부로 `PART_LIST` / `ROOT_BOM` / `FULL_BOM`을 판정하는 규칙이 동일합니다. |
| `mapping.get_uploaded_file_or_raise` | `application/mapping/service/MappingService.getUploadedFileOrThrow` | 완료 | 파일 존재 여부와 `UPLOADED` 상태 사전조건 검증이 동일합니다. |
| `mapping.resolve_target_sheet` | `application/mapping/service/MappingService.loadPreviewTargets` / `loadHeadersAndRows` | 완료 | 요청 시트 우선, 미지정 시 첫 시트 또는 단일 비엑셀 입력을 선택하는 흐름이 유지됩니다. |
| `mapping.load_headers_and_rows` | `application/mapping/service/MappingService.loadHeadersAndRows` + `SpreadsheetParserSupport.parse` | 완료 | Excel/CSV 파싱과 헤더/샘플 행 추출이 이전되었습니다. |
| `mapping.create_mapping` | `application/mapping/service/MappingService.createMapping` + `ConfirmMappingUseCase` | 완료 | 중복 이름 검사, 업로드 파일 검증, 원본 헤더 저장, scope 판정, revision 1 생성이 유지됩니다. |
| `mapping.update_mapping` | `application/mapping/service/MappingService.updateMapping` + `UpdateMappingUseCase` | 완료 | 비활성 매핑 수정 금지, 이름 변경 충돌 검사, 새 revision 생성, scope 재계산이 유지됩니다. |
| `mapping.deactivate_mapping` | `application/mapping/service/MappingService.deactivateMapping` + `DeactivateMappingUseCase` | 완료 | soft delete 성격의 비활성화가 유지됩니다. |
| `mapping.load_preview_targets` | `application/mapping/service/MappingService.loadPreviewTargets` + `PreviewMappingUseCase` | 완료 | 다중 시트 순회와 sheet 단위 skip 수집이 유지됩니다. |
| `mapping.validate_against_rows` | `application/mapping/support/MappingValidationSupport.validateAgainstRows` + `ValidateMappingUseCase` | 완료 | 누락 컬럼, merge key, 숫자 파싱 경고, disabled column count 계산이 유지됩니다. |
| `mapping.parse_sheet_preview` | `application/mapping/service/MappingService.loadHeadersAndRows` + `PreviewMappingUseCase` | 완료 | 시트별 헤더/샘플 행 파싱과 preview 응답 조립이 이전되었습니다. |
| `synthesis.start_synthesis` | `application/synthesis/service/SynthesisService.startSynthesis` + `StartSynthesisUseCase` | 부분 | 매핑/리비전 조회, 업로드 상태 검증, project 소속 검증, batch/job 생성, after-commit 비동기 실행은 이전되었습니다. 다만 구 버전은 rootless relation에 필요한 라벨 키를 모두 검증했지만 현재는 `ROOT_BOM 여부`만 검사해 `root_context` 세부 키 누락을 잡지 못합니다. |
| `synthesis.run_synthesis` | `application/synthesis/service/SynthesisExecutionService.runJob` + `SynthesisAsyncExecutionService` | 부분 | 구 파이프라인은 `Part`, `Drawing`, `Supplier`, `DEFINED_BY`, `SUPPLIED_BY`, `CONSISTS_OF`, 기타 그래프 노드/관계 폴백까지 처리했습니다. 현재는 `Part`, `Supplier`, `CONSISTS_OF`, `SUPPLIED_BY`만 실제 적재하며 `Drawing` 업서트, `DEFINED_BY`, `HAS_ITEM`, 기타 그래프 노드/관계 생성이 없습니다. BOM/공급사 확장 속성도 저장하지 않고 `"{}"`로 고정합니다. |

## 핵심 갭

1. Activation 자연어 질의 축소
   - 구 FastAPI는 LLM이 질의를 `sql` / `graph` / `hybrid` plan으로 바꾸고, 실행 결과를 다시 요약했습니다.
   - 현재 Spring 구현은 제한된 키워드 매칭만 수행하므로 복합 질의, 확장 속성 질의, 그래프 관계 탐색 대부분이 사라졌습니다.

2. Activation 추천 질문과 실제 실행기 불일치
   - `ActivationQuery.lookup`는 BOM 구조, 공급사-부품 매핑, 단가 상위 부품 등 다양한 질문을 노출합니다.
   - 그러나 `ActivationService.queryGraph`는 위 질문 대부분을 일반 키워드 검색으로만 처리하거나 아예 정답 수준의 결과를 만들지 못합니다.

3. Synthesis root context 검증 약화
   - 구 버전은 rootless relation에서 필요한 라벨 집합을 계산해 `root_context` 필수 키 누락을 막았습니다.
   - 현재는 `ROOT_BOM이면 비어 있지 않은지만` 검사하므로 `Supplier`, `Drawing` 등 필요한 키가 빠져도 실행이 진행되고, 이후 관계가 조용히 누락될 수 있습니다.

4. Synthesis 실행 범위 축소
   - 구 파이프라인은 `Drawing`/`Supplier` 노드 업서트, `DEFINED_BY`/`SUPPLIED_BY`/`CONSISTS_OF` dual-write, `HAS_ITEM` 포함 기타 관계의 graph fallback을 지원했습니다.
   - 현재 런타임은 `Drawing`, `Project`, 기타 ontology node/relationship를 실제로 만들지 않습니다. `MappingGenerationSupport`는 `DEFINED_BY`, `HAS_ITEM`까지 생성할 수 있는데 실행기가 이를 소비하지 못합니다.

5. 관계 확장 속성 보존 누락
   - 구 파이프라인은 BOM과 공급사 관계에 `quantity` 외 추가 관계 속성을 `extended_properties`로 넘겼습니다.
   - 현재는 `BomLink.connect(..., "{}")`, `PartSupplier.link(..., "{}")`로 저장되어 `sequence`, `reference_designator`, `find_number` 같은 관계 속성이 유실됩니다.

## 리스크

- 매핑 미리보기는 `DEFINED_BY`, `HAS_ITEM`, rootless supplier/drawing 관계를 정상 제안할 수 있지만, 실제 합성 결과에는 반영되지 않아 사용자 입장에서 “미리보기는 성공했는데 그래프에는 안 들어간다”는 불일치가 발생할 수 있습니다.
- Activation 도메인은 API 표면은 유지됐지만, 사용자 체감 핵심 기능인 자연어 탐색 정확도가 구 버전보다 크게 낮아질 가능성이 높습니다.
- 현재 상태만 보면 AI/그래프 도메인은 CRUD/저장 파이프라인은 상당수 이전되었지만, “그래프 기반 탐색”과 “온톨로지 전체를 사용하는 합성”은 아직 완전 이전으로 보기 어렵습니다.
