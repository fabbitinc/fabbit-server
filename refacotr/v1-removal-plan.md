# V1 제거 및 V2 승격 계획

## 목표

- `mapping`, `synthesis`의 최종 운영 경로를 V2 구현으로 통일한다.
- 최종 상태에서는 코드와 파일명에 `V1` 표기를 남기지 않는다.
- 최종 상태에서는 V2 구현이 기존 canonical 이름을 차지한다.

## 제거하면 안 되는 것

### 1. 최종 비즈니스 테이블과 도메인 모델

- 아래는 V1/V2 구분 없이 실제 제품 데이터를 담는 최종 모델이므로 제거 대상이 아니다.
- 예: `parts`, `suppliers`, `drawings`, `projects`, `bom_links`, `part_suppliers`, `project_parts`

### 2. 아직 V2가 직접 참조하는 V1/shared 클래스

- 아래 파일들은 현재 V2가 직접 import 하고 있으므로, 공용 위치로 먼저 이동하기 전에는 제거하면 안 된다.
- [src/main/java/com/fabbitinc/server/application/mapping/service/MappingService.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping/service/MappingService.java)
- [src/main/java/com/fabbitinc/server/application/mapping/support/SpreadsheetParserSupport.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping/support/SpreadsheetParserSupport.java)
- [src/main/java/com/fabbitinc/server/application/mapping/support/ExtendedPropertySupport.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping/support/ExtendedPropertySupport.java)
- [src/main/java/com/fabbitinc/server/application/mapping/usecase/result/SkippedSheetResult.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping/usecase/result/SkippedSheetResult.java)
- [src/main/java/com/fabbitinc/server/application/mapping/usecase/result/MappingImpactSummaryResult.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping/usecase/result/MappingImpactSummaryResult.java)
- [src/main/java/com/fabbitinc/server/application/mapping/usecase/result/MappingValidationIssueResult.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping/usecase/result/MappingValidationIssueResult.java)
- [src/main/java/com/fabbitinc/server/application/mapping/dto/response/SkippedSheetResponse.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping/dto/response/SkippedSheetResponse.java)
- [src/main/java/com/fabbitinc/server/application/mapping/dto/response/MappingImpactSummaryResponse.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping/dto/response/MappingImpactSummaryResponse.java)
- [src/main/java/com/fabbitinc/server/application/mapping/dto/response/ValidationIssueResponse.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping/dto/response/ValidationIssueResponse.java)
- [src/main/java/com/fabbitinc/server/application/mapping/dto/response/ValidationSeverity.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping/dto/response/ValidationSeverity.java)
- [src/main/java/com/fabbitinc/server/application/synthesis/support/SynthesisResponseMapper.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/synthesis/support/SynthesisResponseMapper.java)
- [src/main/java/com/fabbitinc/server/application/synthesis/dto/request/SynthesisStartRequest.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/synthesis/dto/request/SynthesisStartRequest.java)
- [src/main/java/com/fabbitinc/server/application/synthesis/dto/response/SynthesisBatchFailure.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/synthesis/dto/response/SynthesisBatchFailure.java)
- [src/main/java/com/fabbitinc/server/application/synthesis/dto/response/SynthesisBatchStartResponse.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/synthesis/dto/response/SynthesisBatchStartResponse.java)
- [src/main/java/com/fabbitinc/server/presentation/synthesis/dto/response/SynthesisBatchStatusResponse.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/presentation/synthesis/dto/response/SynthesisBatchStatusResponse.java)
- [src/main/java/com/fabbitinc/server/presentation/synthesis/dto/response/SynthesisJobResponse.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/presentation/synthesis/dto/response/SynthesisJobResponse.java)
- [src/main/java/com/fabbitinc/server/presentation/synthesis/dto/response/SynthesisListResponse.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/presentation/synthesis/dto/response/SynthesisListResponse.java)
- [src/main/java/com/fabbitinc/server/domain/synthesis/model/SynthesisJobStatus.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/domain/synthesis/model/SynthesisJobStatus.java)

## 먼저 해야 할 것

1. V2가 참조하는 V1/shared 클래스를 중립 패키지로 먼저 분리한다.
2. V2 코드에서 `application.mapping.*`, `application.synthesis.*`, `presentation.synthesis.dto.response.*`, `domain.synthesis.model.SynthesisJobStatus` 의존을 제거한다.
3. `/api/v2/mappings`, `/api/v2/synthesis`가 실사용 경로로 충분히 검증됐는지 확인한다.
4. 운영 데이터 기준으로 `mapping_records`/`mapping_revisions`, `synthesis_batches`/`synthesis_jobs`를 더 유지할지, `_v2` 테이블로 완전 전환할지 결정한다.

## 제거 대상

### 1. API 경로 전환 후 제거할 것

- [src/main/java/com/fabbitinc/server/presentation/mapping/controller/MappingController.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/presentation/mapping/controller/MappingController.java)
- [src/main/java/com/fabbitinc/server/presentation/synthesis/controller/SynthesisController.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/presentation/synthesis/controller/SynthesisController.java)
- [src/main/java/com/fabbitinc/server/application/mapping](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/mapping)
- [src/main/java/com/fabbitinc/server/application/synthesis](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/application/synthesis)
- [src/main/java/com/fabbitinc/server/domain/mapping](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/domain/mapping)
- [src/main/java/com/fabbitinc/server/domain/synthesis](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/domain/synthesis)
- [src/main/resources/prompts/mapping/system.st](/Users/seongha.moon/code/projects/fabbit/server2/src/main/resources/prompts/mapping/system.st)
- [src/main/resources/prompts/mapping/user.st](/Users/seongha.moon/code/projects/fabbit/server2/src/main/resources/prompts/mapping/user.st)

### 2. 데이터 정리 후 제거할 것

- V1 매핑 저장 테이블
  - `mapping_records`
  - `mapping_revisions`
- V1 합성 저장 테이블
  - `synthesis_batches`
  - `synthesis_jobs`

## 최종적으로 V2를 V1 자리로 이동하는 방법

### 1. URL

- `POST /api/v2/mappings/*` -> `POST /api/v1/mappings/*`
- `GET /api/v2/mappings/*` -> `GET /api/v1/mappings/*`
- `PUT /api/v2/mappings/*` -> `PUT /api/v1/mappings/*`
- `DELETE /api/v2/mappings/*` -> `DELETE /api/v1/mappings/*`
- `POST /api/v2/synthesis` -> `POST /api/v1/synthesis`
- `GET /api/v2/synthesis/*` -> `GET /api/v1/synthesis/*`

### 2. 파일명과 패키지명

- [src/main/java/com/fabbitinc/server/presentation/mapping/controller/MappingV2Controller.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/presentation/mapping/controller/MappingV2Controller.java) -> `MappingController`
- [src/main/java/com/fabbitinc/server/presentation/synthesis/controller/SynthesisV2Controller.java](/Users/seongha.moon/code/projects/fabbit/server2/src/main/java/com/fabbitinc/server/presentation/synthesis/controller/SynthesisV2Controller.java) -> `SynthesisController`
- `application.mappingv2.*` -> `application.mapping.*`
- `application.synthesisv2.*` -> `application.synthesis.*`
- `domain.mappingv2.*` -> `domain.mapping.*`
- `domain.synthesisv2.*` -> `domain.synthesis.*`
- [src/main/resources/prompts/mapping/v2/system.st](/Users/seongha.moon/code/projects/fabbit/server2/src/main/resources/prompts/mapping/v2/system.st) -> `prompts/mapping/system.st`
- [src/main/resources/prompts/mapping/v2/user.st](/Users/seongha.moon/code/projects/fabbit/server2/src/main/resources/prompts/mapping/v2/user.st) -> `prompts/mapping/user.st`

### 3. DB 테이블명

- 코드만 canonical 이름으로 바꾸고 테이블은 `_v2`를 유지할 수도 있다.
- 다만 최종 정리까지 하려면 아래도 canonical 이름으로 맞추는 편이 좋다.
  - `mapping_v2_records` -> `mapping_records`
  - `mapping_v2_revisions` -> `mapping_revisions`
  - `synthesis_v2_batches` -> `synthesis_batches`
  - `synthesis_v2_jobs` -> `synthesis_jobs`
- 이 테이블 rename은 운영 데이터 이관 계획과 함께 별도 마이그레이션으로 처리해야 한다.

## 최종 완료 기준

- V2 패키지에서 V1 패키지를 import 하지 않는다.
- `/api/v2/mappings`, `/api/v2/synthesis` 경로가 코드에 남아 있지 않다.
- 파일명과 클래스명에 `V2` 표기가 남아 있지 않다.
- V1 매핑/합성 테이블에 더 이상 쓰기 트래픽이 없다.
- V1 컨트롤러, V1 application/domain 패키지, V1 프롬프트 파일을 제거해도 컴파일과 테스트가 통과한다.
