"""외부 연동 e2e: LLM 질의 API."""

import httpx
import pytest
from fastapi.testclient import TestClient

pytestmark = [pytest.mark.e2e, pytest.mark.external, pytest.mark.costly]


class TestActivationExternalFlow:
    access_token: str = ""
    file_id: str = ""
    upload_url: str = ""
    mapping_id: str = ""

    def test_register(self, client: TestClient, unique_suffix: str):
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"activation_ext_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "Activation External 테스트",
                "org_name": f"ActivationExtOrg_{unique_suffix}",
                "slug": f"activation-ext-{unique_suffix}",
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code == 200, resp.text
        TestActivationExternalFlow.access_token = resp.json()["tokens"]["access_token"]

    def test_prepare_synthesis_data(
        self,
        client: TestClient,
        fixtures_dir,
        mapping_fixture: dict[str, object],
    ):
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        content = csv_path.read_bytes()

        create_resp = client.post(
            "/api/v1/files/upload",
            headers={"Authorization": f"Bearer {TestActivationExternalFlow.access_token}"},
            json={
                "original_name": "activation_ext.csv",
                "content_type": "text/csv",
                "file_size": csv_path.stat().st_size,
            },
        )
        assert create_resp.status_code == 200, create_resp.text
        data = create_resp.json()
        TestActivationExternalFlow.file_id = data["file_id"]

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
            f"/api/v1/files/upload/{TestActivationExternalFlow.file_id}/complete",
            headers={"Authorization": f"Bearer {TestActivationExternalFlow.access_token}"},
        )
        assert complete_resp.status_code == 200, complete_resp.text

        mapping_resp = client.post(
            "/api/v1/mappings/confirm",
            headers={"Authorization": f"Bearer {TestActivationExternalFlow.access_token}"},
            json={
                "file_id": TestActivationExternalFlow.file_id,
                "name": "activation external 매핑",
                "mapping": mapping_fixture,
            },
        )
        assert mapping_resp.status_code == 200, mapping_resp.text
        TestActivationExternalFlow.mapping_id = mapping_resp.json()["id"]

        synthesis_resp = client.post(
            "/api/v1/synthesis",
            headers={"Authorization": f"Bearer {TestActivationExternalFlow.access_token}"},
            json={
                "mapping_id": TestActivationExternalFlow.mapping_id,
                "uploads": [{"file_id": TestActivationExternalFlow.file_id}],
            },
        )
        assert synthesis_resp.status_code == 200, synthesis_resp.text
        assert synthesis_resp.json()["accepted_count"] == 1

    def test_activation_query_real_llm(self, client: TestClient, use_llm: bool):
        if not use_llm:
            pytest.skip("--use-llm 없으면 external LLM 테스트를 실행하지 않습니다")

        resp = client.post(
            "/api/v1/activation/query",
            headers={"Authorization": f"Bearer {TestActivationExternalFlow.access_token}"},
            json={"question": "전체 부품 목록을 보여줘"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert isinstance(data["answer"], str)
