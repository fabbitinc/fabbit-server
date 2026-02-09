# 📤 Step 3: Raw 데이터 업로드 (Cloudflare R2 + Presigned URL)

본 문서는 대용량 제조 데이터를 서버 부하 없이 안전하게 업로드하고 관리하는 절차를 정의합니다.

---

## 1. 업로드 아키텍처 (Upload Architecture)

1. **URL 요청**: 클라이언트가 서버에 `GET /uploads/presigned-url` 요청 (파일명, 용량 포함).
2. **URL 발급**: 서버는 R2를 대상으로 한 **Presigned URL**을 생성하여 반환.
3. **직접 업로드**: 클라이언트가 해당 URL을 통해 **R2로 직접 파일을 전송**.
4. **업로드 완료 보고**: 클라이언트가 서버에 `POST /uploads/complete` 호출.
5. **비동기 분석**: 서버는 R2에서 파일을 스트리밍하여 **Step 4 (AI 매핑)**를 위한 데이터 추출 시작.

---

## 2. 화면 구성 및 사용자 경험 (UI/UX)

### 2.1 파일 드롭존 (Center)

- **멀티 파일 큐**: 여러 파일을 올리면 각각의 프로그레스 바가 노출됨.
- **업로드 즉시 분석**: 파일이 R2로 올라가는 동안 서버는 이미 올라간 파일의 헤더를 먼저 읽어 분석을 준비함 (Parallel Processing).

---

## 3. 기술적 상세 (Technical Details)

### 3.1 파일 관리 (Storage Path)

- 경로 구조: `tenants/{org_id}/raw_data/{file_uuid}/{file_name}`
- 테넌트 간 데이터 격리를 스토리지 계층에서도 보장함.

### 3.2 서버의 분석 방식 (Server-side Parsing)

- 서버는 파일을 로컬 디스크에 영구 저장하지 않음.
- `boto3`의 `get_object().get('Body')` 스트림을 사용하여 메모리에서 필요한 부분(헤더, 상위 10개 행)만 읽고 즉시 소멸.

---

## 4. 향후 과제

- [ ] 업로드 실패 시 자동 재시도 로직 (Multipart Upload 활용)
- [ ] R2 Lifecycle 설정을 통해 임시 파일 자동 삭제 로직
