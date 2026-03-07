# 제품 데이터 도메인 마이그레이션 추적

## 범위

- 구 FastAPI 서비스: `../server/app/modules/part/service.py`
- 구 FastAPI 서비스: `../server/app/modules/drawing/service.py`
- 구 FastAPI 서비스: `../server/app/modules/file/service.py`
- 구 FastAPI 서비스: `../server/app/modules/supplier/service.py`
- 신 Spring Boot 후보: `src/main/java/com/fabbitinc/server/application/part/**`, `drawing/**`, `file/**`, `supplier/**`
- 보조 확인: 구 프로젝트에는 `app/modules/*/usecase.py`가 없고 오케스트레이션은 `../server/app/use_cases/part/*`에 분산되어 있음

## 요약

- `part` 쓰기 로직은 Spring의 `PartService` + `*UseCase`로 거의 그대로 이행되었고, 핵심 시나리오 기준으로는 완성도가 높다.
- `file` 도메인은 presigned URL 발급, 업로드 완료, attachable 검증 같은 핵심 업로드 흐름은 이행되었고, `stale PENDING` 및 `expired soft-delete` 정리 배치도 추가되었다. 다만 orphan S3 정리와 실제 프로필 썸네일 변환은 아직 남아 있다.
- `drawing` 도메인은 등록 직후 비동기 변환이 실행되고, CAD는 QCAD CLI, PDF/이미지 후처리는 PDFBox 기반으로 `COMPLETED/FAILED` 전이와 PDF/썸네일 파일 생성까지 연결됐다. 다만 운영 배포 시 QCAD 바이너리를 이미지에 포함해야 한다.
- `supplier`는 구 서비스 계층 자체에 쓰기 로직이 없었고, 읽기 전용 조회는 Spring `SupplierQuery` + `SupplierController`로 이행된 상태다.

## 함수 매핑

| 구 함수 | 신 구현 | 상태 | 근거 |
| --- | --- | --- | --- |
| `part.get_or_raise` | `PartService.getPartOrThrow` | 완료 | 내부 헬퍼로 흡수됐고 동일하게 없을 때 `NOT_FOUND`를 던진다. |
| `part.attach_files` | `AttachPartFilesUseCase.execute` + `PartService.attachFiles` | 완료 | FastAPI의 `file_service.validate_attachable` + `part_service.attach_files` 조합이 Spring에서는 `PartService` 내부 검증 후 `File.assignOwner("part", partId)`로 직접 반영된다. 이벤트 기반에서 직접 할당으로 구현 위치만 바뀌었다. |
| `part.detach_file` | `DetachPartFileUseCase.execute` + `PartService.detachFile` | 완료 | 구 구현은 `FileDetached` 이벤트로 소프트 삭제를 위임했고, Spring은 연결된 파일을 직접 조회해 `softDelete()` 한다. 최종 효과는 동일하다. |
| `part.assign_drawing` | `PartService.assignDrawing` | 완료 | 기존과 동일하게 기존 도면이 있으면 해제 후 새 `drawingId`를 연결한다. |
| `part.unassign_drawing` | `PartService.unassignDrawing` | 완료 | 연결된 도면이 없으면 `NOT_FOUND`, 있으면 `drawingId` 반환 후 해제하는 시맨틱이 동일하다. |
| `part.update_owner` | `UpdatePartOwnerUseCase.execute` + `PartService.updateOwner` | 완료 | FastAPI의 sentinel 기반 PATCH 시맨틱이 Spring의 `ownerIdSet/ownerTeamIdSet` 플래그로 치환되어 의미가 유지된다. |
| `part.rename_category` | `RenameCategoryUseCase.execute` + `PartService.renameCategory` | 완료 | 카테고리 존재 검증, 동일명 차단, merge 시 기본 담당자 삭제, rename 시 기본 담당자 카테고리 이동 로직이 유지된다. |
| `part.upsert_default_owner` | `UpsertDefaultOwnerUseCase.execute` + `PartService.upsertDefaultOwner` | 완료 | 카테고리별 upsert가 유지되고, Spring 쪽은 최소 한 명/한 팀 필요 검증을 더 명시적으로 추가했다. |
| `part.delete_default_owner` | `DeleteDefaultOwnerUseCase.execute` + `PartService.deleteDefaultOwner` | 완료 | 삭제 건수 0일 때 `NOT_FOUND` 처리까지 동일하다. |
| `drawing.create_drawing` | `RegisterPartDrawingUseCase.execute` + `DrawingService.createDrawing` + `DrawingAsyncConversionService` + `DrawingConversionService` | 완료 | 업로드 완료 파일 검증, 확장자 검증, `PENDING` Drawing 생성, Part 연결 후 트랜잭션 커밋 뒤 비동기 변환을 실행한다. CAD는 QCAD `dwg2pdf`, PDF/이미지 후처리는 PDFBox로 처리하고 성공 시 PDF/썸네일 파일 레코드 생성 및 `COMPLETED`, 실패 시 `FAILED`를 반영한다. |
| `drawing.delete_drawing` | `DeletePartDrawingUseCase.execute` + `DrawingService.deleteDrawing` | 완료 | Part에서 도면을 분리한 뒤 Drawing을 soft delete 한다. FastAPI는 FileHandler 이벤트로 파일 정리를 위임했고, Spring은 `original/pdf/thumbnail` 키 기준으로 직접 soft delete 한다. |
| `drawing.to_register_response` | `RegisterPartDrawingUseCase` 결과 + `PartController` 응답 매핑 | 완료 | 등록 응답에 `drawingId`, `drawingNumber`, `name`, `conversionStatus`를 반환하는 흐름이 유지된다. |
| `drawing._validate_drawing_file` | `DrawingService.createDrawing` 내부 확장자 검증 | 완료 | 허용 확장자 검증이 서비스 내부로 이동했다. |
| `file.create_file` | `CreateFileUseCase.execute` + `FileService.createFile` | 완료 | UUIDv7 생성, `uploaded/` 경로 사용, presigned URL 발급 흐름이 유지된다. |
| `file.batch_create_files` | `BatchCreateFilesUseCase.execute` + `FileService.batchCreateFiles` | 완료 | `raw_data/` 경로의 배치 presigned URL 발급 로직이 유지된다. |
| `file.batch_complete_files` | `BatchCompleteFilesUseCase.execute` + `FileService.completeFiles` | 완료 | 파일 존재/이미 완료/S3 미존재를 개별 실패로 수집하고 성공 목록과 실패 목록을 함께 반환한다. |
| `file.complete_file` | `CompleteFileUseCase.execute` + `FileService.completeFile` | 완료 | `NOT_FOUND`, `CONFLICT`, `PRECONDITION_FAILED` 처리와 상태 전이가 유지된다. |
| `file.soft_delete_file` | `FileService.softDelete` | 완료 | 단건 soft delete 기능은 그대로 있다. |
| `file.soft_delete_files` | 대응 없음 | 누락 | 여러 파일을 한 번에 soft delete 하는 공개 서비스/유스케이스가 없다. 구 코드에서도 사용 흔적은 약하지만 기능 자체는 빠져 있다. |
| `file.cleanup_stale_files` | `FileCleanupScheduler.cleanupStalePendingFiles` + `FileCleanupService.cleanupStalePendingFiles` | 완료 | 스케줄러가 모든 테넌트를 순회하며 오래된 `PENDING` 파일을 스토리지 삭제 후 DB에서 물리 삭제한다. PostgreSQL advisory lock 기반 중복 실행 방지도 포함됐다. |
| `file.cleanup_orphan_files` | 대응 없음 | 누락 | S3에는 있고 DB에는 없는 orphan object 정리 기능이 Spring에 없다. `StoragePort`에도 삭제/목록 API가 없다. |
| `file.cleanup_deleted_files` | `FileCleanupScheduler.cleanupExpiredDeletedFiles` + `FileCleanupService.cleanupExpiredDeletedFiles` | 완료 | soft-deleted 후 보존기간이 지난 파일을 스토리지 삭제 후 DB에서 물리 삭제하는 배치가 추가됐다. |
| `file._cleanup_stale_batch` | `FileCleanupService.cleanupStalePendingFiles` | 완료 | 배치 크기 단위 반복 삭제가 서비스 내부 루프로 이전됐다. |
| `file._cleanup_deleted_batch` | `FileCleanupService.cleanupExpiredDeletedFiles` | 완료 | 배치 크기 단위 반복 삭제가 서비스 내부 루프로 이전됐다. |
| `file._delete_s3_files` | `StoragePort.deleteObject` + `S3StorageAdapter.deleteObject` | 완료 | 스토리지 삭제 API가 추가되어 stale/deleted 정리에서 실제 객체 삭제가 가능해졌다. |
| `file.convert_to_thumbnail` | `FileService.convertToThumbnail` | 부분 | FastAPI는 S3 다운로드, 이미지 변환, `.webp` 업로드, 원본 삭제, `file_size/content_type/file_key` 갱신까지 수행했다. Spring은 현재 `File.changeToThumbnailWebp()`로 메타데이터만 바꾸며 실제 스토리지 변환은 없다. |
| `file.get_uploaded_or_raise` | `DrawingService.createDrawing` 내부 검증으로 흡수 | 완료 | 독립 헬퍼는 사라졌지만, 업로드 완료 여부 검증은 도면 등록 경로 안에 포함됐다. |
| `file.get_files_by_owner` | `FileService.getFilesByOwner` | 완료 | 사용자/조직 프로필 이미지 삭제 유스케이스에서 실제 사용 중이다. |
| `file.validate_attachable` | `FileService.validateAttachable` | 완료 | 존재 여부, `UPLOADED`, 미연결(owner 없음) 검증이 그대로 유지된다. |
| `file._to_file_complete_response` | `FileService.toCompleteResponse` | 완료 | 완료 응답 DTO 매핑이 그대로 존재한다. |
| `supplier.service.py` 전체 | `SupplierQuery.list` + `SupplierController.listSuppliers` | 완료 | 구 서비스 파일 주석대로 쓰기 로직은 없고 읽기 전용은 `../server/app/queries/supplier/list_suppliers.py`에서 Spring Query 계층으로 옮겨졌다. |

## 핵심 갭

1. orphan S3 정리 부재
   - stale upload 정리와 soft-deleted 만료 정리는 Spring 스케줄러로 보완됐지만, S3에는 있고 DB에는 없는 orphan object 정리는 아직 없다.
   - 멀티테넌트 prefix 전수 순회가 필요해서 운영 비용과 오탐 리스크를 함께 검토해야 한다.

2. 썸네일 변환의 실제 스토리지 처리 부재
   - 프로필 이미지 경로는 Spring에서도 `validateAttachable -> convertToThumbnail -> setProfileImage` 순서를 유지한다.
   - 그러나 `convertToThumbnail`가 실제 이미지 변환 없이 메타데이터만 `.webp`로 바꾸므로, 현재 구현만으로는 저장소 객체와 DB 메타데이터가 어긋날 수 있다.

3. 다중 환경 배포용 QCAD 바이너리 포함 작업 필요
   - 도면 변환 코드는 `QCAD_PATH/dwg2pdf`를 전제로 구현됐다.
   - 로컬 개발은 맥북 QCAD 설치 경로를 지정하면 되지만, 배포 이미지는 Linux용 QCAD 바이너리를 포함하고 `QCAD_PATH`를 맞춰야 한다.

## 리스크

- 배포 환경에 QCAD 바이너리가 없으면 `dwg/dxf -> pdf` 변환은 `FAILED`로 떨어진다. 운영 이미지는 QCAD 포함이 전제다.
- 프로필 이미지 변경 시 DB의 `file_key/content_type`는 `.webp` 기준으로 바뀌지만 실제 오브젝트는 원본 포맷일 수 있다.
- orphan S3 정리가 없어 DB에는 없지만 버킷에는 남아 있는 객체는 계속 누적될 수 있다.
- `file.soft_delete_files`는 현재 직접 사용 흔적은 약하지만, 구 서비스 기능 기준으로는 배치 삭제 API가 사라진 상태다.
