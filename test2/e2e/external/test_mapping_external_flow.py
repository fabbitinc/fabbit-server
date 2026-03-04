"""외부 연동 e2e: LLM 매핑 preview/validate."""

import httpx
import pytest
from fastapi.testclient import TestClient

pytestmark = [pytest.mark.e2e, pytest.mark.external, pytest.mark.costly]


class TestMappingExternalFlow:
    access_token: str = ""
    file_id: str = ""
    upload_url: str = ""

    def test_register(self, client: TestClient, unique_suffix: str):
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"mapping_ext_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "Mapping External 테스트",
                "org_name": f"MappingExtOrg_{unique_suffix}",
                "slug": f"mapping-ext-{unique_suffix}",
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code == 200, resp.text
        TestMappingExternalFlow.access_token = resp.json()["tokens"]["access_token"]

    def test_upload_csv(self, client: TestClient, fixtures_dir):
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        content = csv_path.read_bytes()

        create_resp = client.post(
            "/api/v1/files/upload",
            headers={"Authorization": f"Bearer {TestMappingExternalFlow.access_token}"},
            json={
                "original_name": "hierarchical_bom.csv",
                "content_type": "text/csv",
                "file_size": csv_path.stat().st_size,
            },
        )
        assert create_resp.status_code == 200, create_resp.text
        data = create_resp.json()
        TestMappingExternalFlow.file_id = data["file_id"]
        TestMappingExternalFlow.upload_url = data["upload_url"]

        with httpx.Client() as http:
            put_resp = http.put(
                data["upload_url"],
                content=content,
                headers={
                    "Content-Type": "text/csv",
                    "Content-Length": str(len(content)),
                },
            )
        assert put_resp.status_code == 200

        complete_resp = client.post(
            f"/api/v1/files/upload/{TestMappingExternalFlow.file_id}/complete",
            headers={"Authorization": f"Bearer {TestMappingExternalFlow.access_token}"},
        )
        assert complete_resp.status_code == 200, complete_resp.text

    def test_mapping_preview_real_llm(self, client: TestClient, use_llm: bool):
        if not use_llm:
            pytest.skip("--use-llm 없으면 external LLM 테스트를 실행하지 않습니다")

        resp = client.post(
            "/api/v1/mappings/preview",
            headers={"Authorization": f"Bearer {TestMappingExternalFlow.access_token}"},
            json={"file_id": TestMappingExternalFlow.file_id},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()["mapping"]
        assert len(data["property_mappings"]) > 0
        assert len(data["relation_mappings"]) > 0

        TestMappingExternalFlow._llm_mapping = data

    def test_mapping_validate_real_llm(self, client: TestClient, use_llm: bool):
        if not use_llm:
            pytest.skip("--use-llm 없으면 external LLM 테스트를 실행하지 않습니다")

        mapping = getattr(TestMappingExternalFlow, "_llm_mapping", None)
        assert mapping, "test_mapping_preview_real_llm가 선행되어야 합니다"

        resp = client.post(
            "/api/v1/mappings/validate",
            headers={"Authorization": f"Bearer {TestMappingExternalFlow.access_token}"},
            json={
                "file_id": TestMappingExternalFlow.file_id,
                "mapping": mapping,
            },
        )
        assert resp.status_code == 200, resp.text
        assert len(resp.json()["errors"]) == 0
