"""파일 업로드/매핑 e2e core 시나리오."""

import httpx
import pytest
from fastapi.testclient import TestClient

pytestmark = [pytest.mark.e2e]


class TestFileMappingFlow:
    access_token: str = ""
    slug: str = ""
    file_id: str = ""
    upload_url: str = ""
    mapping_id: str = ""

    def test_register(self, client: TestClient, unique_suffix: str):
        TestFileMappingFlow.slug = f"mapping-test-{unique_suffix}"
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"mapping_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "Mapping 테스트",
                "org_name": f"MappingOrg_{unique_suffix}",
                "slug": TestFileMappingFlow.slug,
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code == 200, resp.text
        TestFileMappingFlow.access_token = resp.json()["tokens"]["access_token"]

    def test_create_upload(self, client: TestClient, fixtures_dir):
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        file_size = csv_path.stat().st_size

        resp = client.post(
            "/api/v1/files/upload",
            headers={"Authorization": f"Bearer {TestFileMappingFlow.access_token}"},
            json={
                "original_name": "hierarchical_bom.csv",
                "content_type": "text/csv",
                "file_size": file_size,
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        TestFileMappingFlow.file_id = data["file_id"]
        TestFileMappingFlow.upload_url = data["upload_url"]

    def test_upload_to_s3(self, fixtures_dir):
        csv_path = fixtures_dir / "hierarchical_bom.csv"
        content = csv_path.read_bytes()

        with httpx.Client() as http:
            resp = http.put(
                TestFileMappingFlow.upload_url,
                content=content,
                headers={
                    "Content-Type": "text/csv",
                    "Content-Length": str(len(content)),
                },
            )
        assert resp.status_code == 200

    def test_complete_upload(self, client: TestClient):
        resp = client.post(
            f"/api/v1/files/upload/{TestFileMappingFlow.file_id}/complete",
            headers={"Authorization": f"Bearer {TestFileMappingFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        assert resp.json()["status"] == "UPLOADED"

    def test_confirm_mapping(
        self,
        client: TestClient,
        mapping_fixture: dict[str, object],
    ):
        resp = client.post(
            "/api/v1/mappings/confirm",
            headers={"Authorization": f"Bearer {TestFileMappingFlow.access_token}"},
            json={
                "file_id": TestFileMappingFlow.file_id,
                "name": "파일-매핑 e2e",
                "mapping": mapping_fixture,
            },
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        TestFileMappingFlow.mapping_id = data["id"]

    def test_list_mappings(self, client: TestClient):
        resp = client.get(
            "/api/v1/mappings",
            headers={"Authorization": f"Bearer {TestFileMappingFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert len(data["items"]) == 1
        assert data["items"][0]["id"] == TestFileMappingFlow.mapping_id

    def test_get_mapping(self, client: TestClient):
        resp = client.get(
            f"/api/v1/mappings/{TestFileMappingFlow.mapping_id}",
            headers={"Authorization": f"Bearer {TestFileMappingFlow.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        assert resp.json()["id"] == TestFileMappingFlow.mapping_id

    def test_update_mapping(self, client: TestClient, mapping_fixture: dict[str, object]):
        resp = client.put(
            f"/api/v1/mappings/{TestFileMappingFlow.mapping_id}",
            headers={"Authorization": f"Bearer {TestFileMappingFlow.access_token}"},
            json={
                "file_id": TestFileMappingFlow.file_id,
                "name": "수정된 매핑",
                "mapping": mapping_fixture,
            },
        )
        assert resp.status_code == 200, resp.text
        assert resp.json()["name"] == "수정된 매핑"

    def test_delete_mapping(self, client: TestClient):
        resp = client.delete(
            f"/api/v1/mappings/{TestFileMappingFlow.mapping_id}",
            headers={"Authorization": f"Bearer {TestFileMappingFlow.access_token}"},
        )
        assert resp.status_code == 204, resp.text

    def test_get_mapping_not_found(self, client: TestClient):
        resp = client.get(
            f"/api/v1/mappings/{TestFileMappingFlow.mapping_id}",
            headers={"Authorization": f"Bearer {TestFileMappingFlow.access_token}"},
        )
        assert resp.status_code == 404
