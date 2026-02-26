"""전체 API 통합 테스트.

회원가입 → 업로드 → 매핑 → 합성 → 조회 → 프로젝트 → 배치 → 활성화 → AI 질의 → 인증

전제 조건:
  - docker compose up -d (PostgreSQL/AGE + MinIO)
  - uv run alembic upgrade head

실행:
  make test              # fixture 매핑 (LLM 없이, 빠름)
  make test-e2e          # 실제 LLM 호출 포함 (매핑 미리보기, AI 질의)
"""

import uuid

import httpx
import pytest
from fastapi.testclient import TestClient


class TestCRUDFlow:
    """전체 API 파이프라인 통합 테스트.

    클래스 변수로 테스트 간 상태를 공유하며,
    pytest는 파일 내 정의 순서대로 실행합니다.
    """

    access_token: str = ""
    refresh_token: str = ""
    slug: str = ""
    file_id: str = ""
    upload_url: str = ""
    file_key: str = ""
    mapping_id: str = ""
    synthesis_job_id: str = ""
    part_id: str = ""
    batch_file_ids: list[str] = []
    batch_upload_urls: list[str] = []
    batch_id: str = ""

    # ── 공개 API ──

    def test_health(self, client: TestClient):
        """GET /health → 서버 정상 동작 확인."""
        resp = client.get("/health")
        assert resp.status_code == 200
        assert resp.json()["status"] == "ok"

    def test_plans(self, client: TestClient):
        """GET /auth/plans → 플랜 목록 조회."""
        resp = client.get("/api/v1/auth/plans")
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) >= 1
        assert data[0]["plan_type"]
        assert data[0]["display_name"]

    # ── 인증 ──

    def test_register(self, client: TestClient, unique_suffix: str):
        """회원가입 → access_token, refresh_token 획득."""
        TestCRUDFlow.slug = f"crud-test-{unique_suffix}"
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"crud_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "CRUD 테스트",
                "org_name": f"CRUDOrg_{unique_suffix}",
                "slug": TestCRUDFlow.slug,
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["tokens"]["access_token"]
        assert data["tokens"]["refresh_token"]
        assert data["organization"]["slug"] == TestCRUDFlow.slug

        TestCRUDFlow.access_token = data["tokens"]["access_token"]
        TestCRUDFlow.refresh_token = data["tokens"]["refresh_token"]

    def test_check_email_taken(self, client: TestClient, unique_suffix: str):
        """GET /auth/check-email → 등록된 이메일 중복 확인."""
        resp = client.get(
            "/api/v1/auth/check-email",
            params={"email": f"crud_{unique_suffix}@test.com"},
        )
        assert resp.status_code == 200, resp.text
        assert resp.json()["available"] is False

    def test_check_email_available(self, client: TestClient, unique_suffix: str):
        """GET /auth/check-email → 미등록 이메일 확인."""
        resp = client.get(
            "/api/v1/auth/check-email",
            params={"email": f"unused_{unique_suffix}@test.com"},
        )
        assert resp.status_code == 200, resp.text
        assert resp.json()["available"] is True

    def test_check_slug_taken(self, client: TestClient):
        """GET /auth/check-slug → 등록된 slug 중복 확인."""
        resp = client.get(
            "/api/v1/auth/check-slug",
            params={"slug": TestCRUDFlow.slug},
        )
        assert resp.status_code == 200, resp.text
        assert resp.json()["available"] is False

    def test_check_slug_available(self, client: TestClient, unique_suffix: str):
        """GET /auth/check-slug → 미사용 slug 확인."""
        resp = client.get(
            "/api/v1/auth/check-slug",
            params={"slug": f"unused-{unique_suffix}"},
        )
        assert resp.status_code == 200, resp.text
        assert resp.json()["available"] is True

    def test_site(self, client: TestClient):
        """GET /auth/site → 워크스페이스 정보 조회."""
        resp = client.get(
            "/api/v1/auth/site",
            headers={"Origin": f"http://{TestCRUDFlow.slug}.lvh.me"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["slug"] == TestCRUDFlow.slug

    def test_login(self, client: TestClient, unique_suffix: str):
        """POST /auth/login → 로그인 + 토큰 획득."""
        resp = client.post(
            "/api/v1/auth/login",
            json={
                "email": f"crud_{unique_suffix}@test.com",
                "password": "TestPass1234",
            },
            headers={"Origin": f"http://{TestCRUDFlow.slug}.lvh.me"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["tokens"]["access_token"]

        # login 토큰으로 교체
        TestCRUDFlow.access_token = data["tokens"]["access_token"]
        TestCRUDFlow.refresh_token = data["tokens"]["refresh_token"]

    def test_onboarding_complete(self, client: TestClient):
        """POST /auth/onboarding/complete → 온보딩 완료."""
        resp = client.post(
            "/api/v1/auth/onboarding/complete",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["onboarded_at"] is not None

    def test_ontology_schema(self, client: TestClient):
        """GET /ontology/schema → 온톨로지 스키마 조회 (인증 필수)."""
        resp = client.get(
            "/api/v1/ontology/schema",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["name"]
        assert len(data["node_labels"]) > 0
        assert len(data["relationship_types"]) > 0

    def test_me(self, client: TestClient):
        """GET /auth/me → 유저/조직 정보 확인."""
        resp = client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["user"]["email"].startswith("crud_")
        assert len(data["memberships"]) >= 1
        assert data["memberships"][0]["organization"]["slug"] == TestCRUDFlow.slug

    # ── 업로드 ──

    def test_create_upload(self, client: TestClient, fixtures_dir):
        """POST /files/upload → presigned URL 발급."""
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        file_size = csv_path.stat().st_size

        resp = client.post(
            "/api/v1/files/upload",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={
                "original_name": "hierarchical_bom.csv",
                "content_type": "text/csv",
                "file_size": file_size,
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["file_id"]
        assert data["upload_url"]

        TestCRUDFlow.file_id = data["file_id"]
        TestCRUDFlow.upload_url = data["upload_url"]
        TestCRUDFlow.file_key = data["file_key"]

    def test_upload_to_s3(self, fixtures_dir):
        """PUT presigned URL → MinIO에 CSV 파일 업로드."""
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        content = csv_path.read_bytes()

        with httpx.Client() as http:
            resp = http.put(
                TestCRUDFlow.upload_url,
                content=content,
                headers={
                    "Content-Type": "text/csv",
                    "Content-Length": str(len(content)),
                },
            )
        assert resp.status_code == 200, f"MinIO PUT 실패: {resp.status_code}"

    def test_complete_upload(self, client: TestClient):
        """POST /files/upload/{id}/complete → status=UPLOADED."""
        resp = client.post(
            f"/api/v1/files/upload/{TestCRUDFlow.file_id}/complete",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        assert resp.json()["status"] == "UPLOADED"

    # ── 매핑 ──

    def test_mapping_preview(self, client: TestClient, use_llm: bool):
        """POST /mappings/preview → LLM 매핑 미리보기. (--use-llm 전용)"""
        if not use_llm:
            pytest.skip("LLM 비활성 (--use-llm 없음)")

        resp = client.post(
            "/api/v1/mappings/preview",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={"file_id": TestCRUDFlow.file_id},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert len(data["mapping"]["property_mappings"]) > 0
        assert len(data["mapping"]["relation_mappings"]) > 0

        # LLM 결과를 매핑 fixture 대신 사용
        TestCRUDFlow._llm_mapping = data["mapping"]

    def test_mapping_validate(self, client: TestClient, use_llm: bool):
        """POST /mappings/validate → 매핑 검증. (--use-llm 전용)"""
        if not use_llm:
            pytest.skip("LLM 비활성 (--use-llm 없음)")

        mapping = getattr(TestCRUDFlow, "_llm_mapping", None)
        assert mapping, "test_mapping_preview가 선행되어야 합니다"

        resp = client.post(
            "/api/v1/mappings/validate",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={
                "file_id": TestCRUDFlow.file_id,
                "mapping": mapping,
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert len(data["errors"]) == 0, f"매핑 검증 에러: {data['errors']}"

        # 정규화된 매핑으로 교체
        TestCRUDFlow._llm_mapping = data["normalized_mapping"]

    def test_confirm_mapping(
        self,
        client: TestClient,
        use_llm: bool,
        mapping_fixture: dict[str, object],
    ):
        """POST /mappings/confirm → 매핑 확정."""
        # --use-llm이면 LLM 결과, 아니면 fixture 사용
        mapping = (
            getattr(TestCRUDFlow, "_llm_mapping", mapping_fixture)
            if use_llm
            else mapping_fixture
        )

        resp = client.post(
            "/api/v1/mappings/confirm",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={
                "file_id": TestCRUDFlow.file_id,
                "name": "통합 테스트 매핑",
                "mapping": mapping,
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["id"]
        assert data["file_id"] == TestCRUDFlow.file_id

        TestCRUDFlow.mapping_id = data["id"]

    def test_list_mappings(self, client: TestClient):
        """GET /mappings → 매핑 목록 조회."""
        resp = client.get(
            "/api/v1/mappings",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert len(data["items"]) == 1, f"매핑 1건 기대, 실제 {len(data['items'])}건"
        assert data["items"][0]["id"] == TestCRUDFlow.mapping_id

    def test_get_mapping(self, client: TestClient):
        """GET /mappings/{mapping_id} → 매핑 상세 조회."""
        resp = client.get(
            f"/api/v1/mappings/{TestCRUDFlow.mapping_id}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["id"] == TestCRUDFlow.mapping_id
        assert data["name"] == "통합 테스트 매핑"

    # ── 합성 ──

    def test_start_synthesis(self, client: TestClient):
        """POST /synthesis → 합성 시작 (통합 배치 응답).

        TestClient에서 BackgroundTask는 동기적으로 실행되므로,
        응답 반환 시점에 합성이 이미 완료됩니다.
        """
        resp = client.post(
            "/api/v1/synthesis",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={
                "mapping_id": TestCRUDFlow.mapping_id,
                "uploads": [{"file_id": TestCRUDFlow.file_id}],
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["batch_id"]
        assert data["accepted_count"] == 1
        assert len(data["items"]) == 1

        TestCRUDFlow.synthesis_job_id = data["items"][0]["id"]

    def test_synthesis_completed(self, client: TestClient):
        """GET /synthesis/{id} → status=COMPLETED 확인."""
        resp = client.get(
            f"/api/v1/synthesis/{TestCRUDFlow.synthesis_job_id}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["status"] == "COMPLETED", (
            f"합성 상태: {data['status']}, errors: {data.get('errors')}"
        )
        assert data["total_rows"] > 0
        assert data["nodes_created"] > 0

    def test_list_synthesis(self, client: TestClient):
        """GET /synthesis → 합성 작업 목록 조회."""
        resp = client.get(
            "/api/v1/synthesis",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert len(data["items"]) == 1, f"합성 1건 기대, 실제 {len(data['items'])}건"
        assert data["items"][0]["id"] == TestCRUDFlow.synthesis_job_id

    # ── 합성 후 대시보드/검색 ──

    def test_dashboard_stats(self, client: TestClient):
        """GET /dashboard/stats → 합성 후 Part/Supplier 수 확인."""
        resp = client.get(
            "/api/v1/dashboard/stats",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["parts"]["total"] > 0
        assert data["last_synthesis"] is not None

    def test_ontology_nodes_search(self, client: TestClient):
        """GET /ontology/nodes/search → Part 노드 검색."""
        resp = client.get(
            "/api/v1/ontology/nodes/search",
            params={"label": "Part", "search": "ASM"},
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert len(data["items"]) > 0

    # ── 조회 ──

    def test_list_parts(self, client: TestClient):
        """GET /parts → 합성된 Part 존재 확인."""
        resp = client.get(
            "/api/v1/parts",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["total"] == 10, f"고유 품번 10건 기대, 실제 {data['total']}건"
        part_numbers = {item["part_number"] for item in data["items"]}
        assert "ASM-001" in part_numbers
        assert "PRT-001" in part_numbers

        # 이후 테스트에서 사용할 part_id 저장
        part_id_map = {item["part_number"]: item["id"] for item in data["items"]}
        TestCRUDFlow.part_id = part_id_map["PRT-001"]
        TestCRUDFlow.asm001_id = part_id_map["ASM-001"]

    def test_part_detail(self, client: TestClient):
        """GET /parts/{part_id} → Part 상세 + 관계 확인."""
        resp = client.get(
            f"/api/v1/parts/{TestCRUDFlow.asm001_id}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["part_number"] == "ASM-001"
        assert data["name"] == "메인 프레임 조립품"
        assert len(data["children"]) >= 4

    def test_bom_tree(self, client: TestClient):
        """GET /parts/{part_id}/bom → BOM 트리 구조 확인."""
        resp = client.get(
            f"/api/v1/parts/{TestCRUDFlow.asm001_id}/bom",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        root = data["root"]
        assert root["part_number"] == "ASM-001"
        assert len(root["children"]) >= 4
        child_pns = {c["part_number"] for c in root["children"]}
        assert "PRT-001" in child_pns
        assert "PRT-002" in child_pns
        # grandchildren 확인
        prt001 = next(c for c in root["children"] if c["part_number"] == "PRT-001")
        prt001_child_pns = {c["part_number"] for c in prt001["children"]}
        assert "PRT-005" in prt001_child_pns
        assert "PRT-006" in prt001_child_pns

    def test_filter_options(self, client: TestClient):
        """GET /parts/filter-options → 필터 옵션 조회."""
        resp = client.get(
            "/api/v1/parts/filter-options",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text

    def test_export_parts(self, client: TestClient):
        """GET /parts/export → Excel 내보내기 (200 + content-type)."""
        resp = client.get(
            "/api/v1/parts/export",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        assert "spreadsheet" in resp.headers["content-type"]

    def test_export_bom(self, client: TestClient):
        """GET /parts/{part_id}/bom/export → BOM Excel 내보내기."""
        resp = client.get(
            f"/api/v1/parts/{TestCRUDFlow.asm001_id}/bom/export",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        assert "spreadsheet" in resp.headers["content-type"]

    def test_list_suppliers(self, client: TestClient):
        """GET /suppliers → 공급사 목록 조회."""
        resp = client.get(
            "/api/v1/suppliers",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert "items" in data

    # ── 배치 업로드 + 합성 ──

    def test_batch_upload(self, client: TestClient, fixtures_dir):
        """POST /files/upload/batch → 배치 presigned URL 발급."""
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        file_size = csv_path.stat().st_size

        resp = client.post(
            "/api/v1/files/upload/batch",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={
                "items": [
                    {
                        "original_name": "batch_bom_1.csv",
                        "content_type": "text/csv",
                        "file_size": file_size,
                        "owner_type": "part",
                        "owner_id": TestCRUDFlow.part_id,
                    },
                    {
                        "original_name": "batch_bom_2.csv",
                        "content_type": "text/csv",
                        "file_size": file_size,
                        "owner_type": "part",
                        "owner_id": TestCRUDFlow.part_id,
                    },
                ]
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert len(data["items"]) == 2

        TestCRUDFlow.batch_file_ids = [item["file_id"] for item in data["items"]]
        TestCRUDFlow.batch_upload_urls = [item["upload_url"] for item in data["items"]]

    def test_batch_upload_to_s3(self, fixtures_dir):
        """PUT presigned URLs → MinIO에 배치 CSV 업로드."""
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        content = csv_path.read_bytes()

        with httpx.Client() as http:
            for url in TestCRUDFlow.batch_upload_urls:
                resp = http.put(
                    url,
                    content=content,
                    headers={
                        "Content-Type": "text/csv",
                        "Content-Length": str(len(content)),
                    },
                )
                assert resp.status_code == 200, f"MinIO PUT 실패: {resp.status_code}"

    def test_batch_complete(self, client: TestClient):
        """POST /files/upload/batch/complete → 배치 업로드 완료."""
        resp = client.post(
            "/api/v1/files/upload/batch/complete",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={"file_ids": TestCRUDFlow.batch_file_ids},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert len(data["items"]) == 2
        for item in data["items"]:
            assert item["status"] == "UPLOADED"

    def test_start_synthesis_batch(self, client: TestClient):
        """POST /synthesis → 배치 합성 시작."""
        resp = client.post(
            "/api/v1/synthesis",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={
                "mapping_id": TestCRUDFlow.mapping_id,
                "uploads": [{"file_id": uid} for uid in TestCRUDFlow.batch_file_ids],
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["batch_id"]
        assert data["accepted_count"] >= 1

        TestCRUDFlow.batch_id = data["batch_id"]

    def test_synthesis_batch_status(self, client: TestClient):
        """GET /synthesis/batches/{batch_id} → 배치 합성 상태 확인."""
        resp = client.get(
            f"/api/v1/synthesis/batches/{TestCRUDFlow.batch_id}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["batch_id"] == TestCRUDFlow.batch_id

    # ── 파일 연결/해제 ──

    def test_create_attach_file(self, client: TestClient, fixtures_dir):
        """파일 업로드 → complete → Part에 연결할 파일 준비."""
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        file_size = csv_path.stat().st_size

        # presigned URL 발급
        resp = client.post(
            "/api/v1/files/upload",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={
                "original_name": "attach_test.csv",
                "content_type": "text/csv",
                "file_size": file_size,
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        attach_file_id = data["file_id"]
        upload_url = data["upload_url"]

        # S3 업로드
        with httpx.Client() as http:
            put_resp = http.put(
                upload_url,
                content=csv_path.read_bytes(),
                headers={
                    "Content-Type": "text/csv",
                    "Content-Length": str(file_size),
                },
            )
        assert put_resp.status_code == 200

        # 업로드 완료
        resp = client.post(
            f"/api/v1/files/upload/{attach_file_id}/complete",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text

        TestCRUDFlow._attach_file_id = attach_file_id

    def test_attach_file_to_part(self, client: TestClient):
        """POST /parts/{part_id}/files → Part에 파일 연결."""
        attach_file_id = getattr(TestCRUDFlow, "_attach_file_id", None)
        assert attach_file_id, "test_create_attach_file가 선행되어야 합니다"
        resp = client.post(
            f"/api/v1/parts/{TestCRUDFlow.part_id}/files",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={"file_ids": [attach_file_id]},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert len(data) >= 1

    def test_detach_file_from_part(self, client: TestClient):
        """DELETE /parts/{part_id}/files/{file_id} → Part에서 파일 제거."""
        attach_file_id = getattr(TestCRUDFlow, "_attach_file_id", None)
        assert attach_file_id, "test_attach_file_to_part가 선행되어야 합니다"
        resp = client.delete(
            f"/api/v1/parts/{TestCRUDFlow.part_id}/files/{attach_file_id}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 204, resp.text

    # ── 매핑 수정 ──

    def test_update_mapping(
        self,
        client: TestClient,
        mapping_fixture: dict[str, object],
    ):
        """PUT /mappings/{mapping_id} → 매핑 이름 수정."""
        resp = client.put(
            f"/api/v1/mappings/{TestCRUDFlow.mapping_id}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={
                "file_id": TestCRUDFlow.file_id,
                "name": "수정된 매핑",
                "mapping": mapping_fixture,
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["name"] == "수정된 매핑"

    # ── 정리 ──

    def test_delete_mapping(self, client: TestClient):
        """DELETE /mappings/{mapping_id} → 매핑 삭제."""
        resp = client.delete(
            f"/api/v1/mappings/{TestCRUDFlow.mapping_id}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 204, resp.text

    # ── 활성화 ──

    def test_activation_health_check(self, client: TestClient):
        """POST /activation/health-check → 그래프 상태 확인."""
        resp = client.post(
            "/api/v1/activation/health-check",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["total_nodes"] > 0
        assert data["total_relationships"] > 0

    def test_activation_starters(self, client: TestClient):
        """GET /activation/starters → 추천 질문 목록."""
        resp = client.get(
            "/api/v1/activation/starters",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert len(data["starters"]) > 0

    # ── AI 질의 (--use-llm 전용, 결과 출력만) ──

    def test_ai_query_parts(self, client: TestClient, use_llm: bool):
        """POST /activation/query → 전체 부품 목록 질의. (--use-llm 전용)"""
        if not use_llm:
            pytest.skip("LLM 비활성 (--use-llm 없음)")

        resp = client.post(
            "/api/v1/activation/query",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={"question": "전체 부품 목록을 보여줘"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        print(f"\n{'=' * 60}")
        print("[AI 질의] 전체 부품 목록")
        print(f"  결과: {len(data['results'])}건")
        print(f"  답변: {data['answer'][:300]}")
        print(f"{'=' * 60}")

    def test_ai_query_bom(self, client: TestClient, use_llm: bool):
        """POST /activation/query → BOM 관계 질의. (--use-llm 전용)"""
        if not use_llm:
            pytest.skip("LLM 비활성 (--use-llm 없음)")

        resp = client.post(
            "/api/v1/activation/query",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={
                "question": "상위 부품과 하위 부품의 CONSISTS_OF 관계를 모두 보여줘. "
                "상위품번, 하위품번, 수량을 포함해서"
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        print(f"\n{'=' * 60}")
        print("[AI 질의] BOM 관계")
        print(f"  결과: {len(data['results'])}건")
        print(f"  답변: {data['answer'][:300]}")
        print(f"{'=' * 60}")

    # ── 인증 후속 ──

    def test_token_refresh(self, client: TestClient):
        """POST /auth/refresh → 새 토큰 발급."""
        resp = client.post(
            "/api/v1/auth/refresh",
            json={"refresh_token": TestCRUDFlow.refresh_token},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["access_token"]
        assert data["refresh_token"]

        TestCRUDFlow.access_token = data["access_token"]
        TestCRUDFlow.refresh_token = data["refresh_token"]

    def test_logout(self, client: TestClient):
        """POST /auth/logout → 204 반환."""
        resp = client.post(
            "/api/v1/auth/logout",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
            json={"refresh_token": TestCRUDFlow.refresh_token},
        )
        assert resp.status_code == 204, resp.text

    # ── 예외 테스트 ──

    def test_get_part_not_found(self, client: TestClient):
        """GET /parts/{non-existent} → 404."""
        resp = client.get(
            f"/api/v1/parts/{uuid.uuid4()}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 404, resp.text

    def test_get_mapping_not_found(self, client: TestClient):
        """GET /mappings/{non-existent} → 404."""
        resp = client.get(
            f"/api/v1/mappings/{uuid.uuid4()}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 404, resp.text

    def test_get_synthesis_not_found(self, client: TestClient):
        """GET /synthesis/{non-existent} → 404."""
        resp = client.get(
            f"/api/v1/synthesis/{uuid.uuid4()}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 404, resp.text

    def test_get_drawing_analysis_not_found(self, client: TestClient):
        """GET /drawings/analyses/{non-existent} → 404."""
        resp = client.get(
            f"/api/v1/drawings/analyses/{uuid.uuid4()}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 404, resp.text

    def test_get_drawing_synthesis_not_found(self, client: TestClient):
        """GET /drawings/synthesis/{non-existent} → 404."""
        resp = client.get(
            f"/api/v1/drawings/synthesis/{uuid.uuid4()}",
            headers={"Authorization": f"Bearer {TestCRUDFlow.access_token}"},
        )
        assert resp.status_code == 404, resp.text

    def test_register_duplicate_email(self, client: TestClient, unique_suffix: str):
        """POST /auth/register → 중복 이메일로 가입 시 에러."""
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"crud_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "중복 테스트",
                "org_name": "DupOrg",
                "slug": f"dup-email-{unique_suffix}",
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code in (400, 409), (
            f"기대: 400 또는 409, 실제: {resp.status_code}"
        )

    def test_register_duplicate_slug(self, client: TestClient, unique_suffix: str):
        """POST /auth/register → 중복 slug로 가입 시 에러."""
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"newuser_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "중복 테스트",
                "org_name": "DupSlugOrg",
                "slug": TestCRUDFlow.slug,
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code in (400, 409), (
            f"기대: 400 또는 409, 실제: {resp.status_code}"
        )

    def test_login_wrong_password(self, client: TestClient, unique_suffix: str):
        """POST /auth/login → 잘못된 비밀번호로 로그인 시 401."""
        resp = client.post(
            "/api/v1/auth/login",
            json={
                "email": f"crud_{unique_suffix}@test.com",
                "password": "WrongPassword",
            },
            headers={"Origin": f"http://{TestCRUDFlow.slug}.lvh.me"},
        )
        assert resp.status_code == 401, f"기대: 401, 실제: {resp.status_code}"

    def test_parts_without_auth(self, client: TestClient):
        """GET /parts → 인증 없이 요청 시 401."""
        resp = client.get("/api/v1/parts")
        assert resp.status_code == 401, f"기대: 401, 실제: {resp.status_code}"
