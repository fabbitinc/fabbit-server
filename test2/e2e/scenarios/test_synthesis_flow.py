"""합성/조회/활성화 e2e 시나리오.

- 핵심 비즈니스 플로우는 항상 실행
- LLM 질의 API는 `--use-llm`일 때만 실행
"""

import uuid

import httpx
import pytest
from fastapi.testclient import TestClient

pytestmark = [pytest.mark.e2e]


class TestSynthesisFlow:
    access_token: str = ""
    refresh_token: str = ""
    slug: str = ""
    file_id: str = ""
    upload_url: str = ""
    mapping_id: str = ""
    synthesis_job_id: str = ""
    batch_file_ids: list[str] = []
    batch_upload_urls: list[str] = []
    batch_id: str = ""

    def test_register(self, client: TestClient, unique_suffix: str):
        TestSynthesisFlow.slug = f"synth-test-{unique_suffix}"
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"synth_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "Synthesis 테스트",
                "org_name": f"SynthOrg_{unique_suffix}",
                "slug": TestSynthesisFlow.slug,
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        TestSynthesisFlow.access_token = data["tokens"]["access_token"]
        TestSynthesisFlow.refresh_token = data["tokens"]["refresh_token"]

    def test_ontology_schema(self, client: TestClient):
        resp = client.get(
            "/api/v1/ontology/schema",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["node_labels"]) > 0
        assert len(data["relationship_types"]) > 0

    def test_create_upload(self, client: TestClient, fixtures_dir):
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        resp = client.post(
            "/api/v1/files/upload",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
            json={
                "original_name": "hierarchical_bom.csv",
                "content_type": "text/csv",
                "file_size": csv_path.stat().st_size,
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        TestSynthesisFlow.file_id = data["file_id"]
        TestSynthesisFlow.upload_url = data["upload_url"]

    def test_upload_to_s3(self, fixtures_dir):
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        content = csv_path.read_bytes()
        with httpx.Client() as http:
            resp = http.put(
                TestSynthesisFlow.upload_url,
                content=content,
                headers={
                    "Content-Type": "text/csv",
                    "Content-Length": str(len(content)),
                },
            )
        assert resp.status_code == 200

    def test_complete_upload(self, client: TestClient):
        resp = client.post(
            f"/api/v1/files/upload/{TestSynthesisFlow.file_id}/complete",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "UPLOADED"

    def test_confirm_mapping(self, client: TestClient, mapping_fixture: dict[str, object]):
        resp = client.post(
            "/api/v1/mappings/confirm",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
            json={
                "file_id": TestSynthesisFlow.file_id,
                "name": "합성 e2e 매핑",
                "mapping": mapping_fixture,
            },
        )
        assert resp.status_code == 200, resp.text
        TestSynthesisFlow.mapping_id = resp.json()["id"]

    def test_start_synthesis(self, client: TestClient):
        resp = client.post(
            "/api/v1/synthesis",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
            json={
                "mapping_id": TestSynthesisFlow.mapping_id,
                "uploads": [{"file_id": TestSynthesisFlow.file_id}],
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["accepted_count"] == 1
        TestSynthesisFlow.synthesis_job_id = data["items"][0]["id"]

    def test_synthesis_completed(self, client: TestClient):
        resp = client.get(
            f"/api/v1/synthesis/{TestSynthesisFlow.synthesis_job_id}",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert data["status"] == "COMPLETED"
        assert data["total_rows"] > 0

    def test_dashboard_and_part_read(self, client: TestClient):
        stats = client.get(
            "/api/v1/dashboard/stats",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert stats.status_code == 200, stats.text
        assert stats.json()["parts"]["total"] > 0

        parts = client.get(
            "/api/v1/parts",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert parts.status_code == 200, parts.text
        items = parts.json()["items"]
        assert len(items) > 0
        part_number_map = {item["part_number"]: item["id"] for item in items}
        TestSynthesisFlow.part_id = part_number_map["PRT-001"]
        TestSynthesisFlow.asm001_id = part_number_map["ASM-001"]

    def test_bom_and_export(self, client: TestClient):
        bom = client.get(
            f"/api/v1/parts/{TestSynthesisFlow.asm001_id}/bom",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert bom.status_code == 200, bom.text
        root = bom.json()["root"]
        assert root["part_number"] == "ASM-001"

        export_parts = client.get(
            "/api/v1/parts/export",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert export_parts.status_code == 200
        assert "spreadsheet" in export_parts.headers["content-type"]

        export_bom = client.get(
            f"/api/v1/parts/{TestSynthesisFlow.asm001_id}/bom/export",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert export_bom.status_code == 200
        assert "spreadsheet" in export_bom.headers["content-type"]

    def test_batch_upload_and_synthesis(self, client: TestClient, fixtures_dir):
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        file_size = csv_path.stat().st_size

        create_resp = client.post(
            "/api/v1/files/upload/batch",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
            json={
                "items": [
                    {
                        "original_name": "batch_bom_1.csv",
                        "content_type": "text/csv",
                        "file_size": file_size,
                        "owner_type": "part",
                        "owner_id": TestSynthesisFlow.part_id,
                    },
                    {
                        "original_name": "batch_bom_2.csv",
                        "content_type": "text/csv",
                        "file_size": file_size,
                        "owner_type": "part",
                        "owner_id": TestSynthesisFlow.part_id,
                    },
                ]
            },
        )
        assert create_resp.status_code == 200, create_resp.text
        items = create_resp.json()["items"]
        TestSynthesisFlow.batch_file_ids = [item["file_id"] for item in items]
        TestSynthesisFlow.batch_upload_urls = [item["upload_url"] for item in items]

        content = csv_path.read_bytes()
        with httpx.Client() as http:
            for url in TestSynthesisFlow.batch_upload_urls:
                put_resp = http.put(
                    url,
                    content=content,
                    headers={
                        "Content-Type": "text/csv",
                        "Content-Length": str(len(content)),
                    },
                )
                assert put_resp.status_code == 200

        complete_resp = client.post(
            "/api/v1/files/upload/batch/complete",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
            json={"file_ids": TestSynthesisFlow.batch_file_ids},
        )
        assert complete_resp.status_code == 200

        synth_resp = client.post(
            "/api/v1/synthesis",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
            json={
                "mapping_id": TestSynthesisFlow.mapping_id,
                "uploads": [{"file_id": uid} for uid in TestSynthesisFlow.batch_file_ids],
            },
        )
        assert synth_resp.status_code == 200
        batch_data = synth_resp.json()
        assert batch_data["batch_id"]
        TestSynthesisFlow.batch_id = batch_data["batch_id"]

        status_resp = client.get(
            f"/api/v1/synthesis/batches/{TestSynthesisFlow.batch_id}",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert status_resp.status_code == 200

    def test_attach_and_detach_file(self, client: TestClient, fixtures_dir):
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        file_size = csv_path.stat().st_size

        create_resp = client.post(
            "/api/v1/files/upload",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
            json={
                "original_name": "attach_test.csv",
                "content_type": "text/csv",
                "file_size": file_size,
            },
        )
        assert create_resp.status_code == 200
        upload_info = create_resp.json()
        attach_file_id = upload_info["file_id"]

        with httpx.Client() as http:
            put_resp = http.put(
                upload_info["upload_url"],
                content=csv_path.read_bytes(),
                headers={
                    "Content-Type": "text/csv",
                    "Content-Length": str(file_size),
                },
            )
        assert put_resp.status_code == 200

        done_resp = client.post(
            f"/api/v1/files/upload/{attach_file_id}/complete",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert done_resp.status_code == 200

        attach_resp = client.post(
            f"/api/v1/parts/{TestSynthesisFlow.part_id}/files",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
            json={"file_ids": [attach_file_id]},
        )
        assert attach_resp.status_code == 200

        detach_resp = client.delete(
            f"/api/v1/parts/{TestSynthesisFlow.part_id}/files/{attach_file_id}",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert detach_resp.status_code == 204

    def test_activation(self, client: TestClient):
        health = client.post(
            "/api/v1/activation/health-check",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert health.status_code == 200, health.text

        starters = client.get(
            "/api/v1/activation/starters",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
        )
        assert starters.status_code == 200
        assert len(starters.json()["starters"]) > 0

    @pytest.mark.llm_api
    def test_activation_query(self, client: TestClient, use_llm: bool):
        if not use_llm:
            pytest.skip("LLM 비활성 (--use-llm 없음)")

        resp = client.post(
            "/api/v1/activation/query",
            headers={"Authorization": f"Bearer {TestSynthesisFlow.access_token}"},
            json={"question": "전체 부품 목록을 보여줘"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert isinstance(data["answer"], str)

    def test_not_found_cases(self, client: TestClient):
        headers = {"Authorization": f"Bearer {TestSynthesisFlow.access_token}"}

        resp = client.get(f"/api/v1/synthesis/{uuid.uuid4()}", headers=headers)
        assert resp.status_code == 404

        resp = client.get(f"/api/v1/drawings/analyses/{uuid.uuid4()}", headers=headers)
        assert resp.status_code == 404

        resp = client.get(f"/api/v1/drawings/synthesis/{uuid.uuid4()}", headers=headers)
        assert resp.status_code == 404
