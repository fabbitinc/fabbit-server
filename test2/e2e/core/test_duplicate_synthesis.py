"""중복 합성 e2e 시나리오.

legacy LLM 검증 목적이 아니라, 반복 합성 후 비즈니스 정합성(중복 방지/관계 일관성)을 검증한다.
"""

import httpx
import pytest
from fastapi.testclient import TestClient

pytestmark = [pytest.mark.e2e]

EXPECTED_PART_NUMBERS = {
    "ASM-001", "ASM-002",
    "PRT-001", "PRT-002", "PRT-003", "PRT-004",
    "PRT-005", "PRT-006", "PRT-007", "PRT-008",
}

EXPECTED_CHILDREN = {
    "ASM-001": {"PRT-001", "PRT-002", "PRT-003", "PRT-004"},
    "ASM-002": {"PRT-008", "PRT-003"},
    "PRT-001": {"PRT-005", "PRT-006"},
    "PRT-002": {"PRT-007"},
}

REPEAT_COUNT = 3


class TestDuplicateSynthesis:
    access_token: str = ""
    mapping_id: str = ""

    def test_register(self, client: TestClient, unique_suffix: str):
        resp = client.post(
            "/api/v1/auth/register",
            json={
                "email": f"dup_{unique_suffix}@test.com",
                "password": "TestPass1234",
                "full_name": "중복 합성 테스트",
                "org_name": f"DupOrg_{unique_suffix}",
                "slug": f"dup-test-{unique_suffix}",
                "plan_type": "STARTER",
            },
        )
        assert resp.status_code == 200, resp.text
        TestDuplicateSynthesis.access_token = resp.json()["tokens"]["access_token"]

    def test_confirm_fixture_mapping(
        self,
        client: TestClient,
        mapping_fixture: dict[str, object],
        fixtures_dir,
    ):
        file_id, _ = _upload_csv(client, TestDuplicateSynthesis.access_token, fixtures_dir)
        resp = client.post(
            "/api/v1/mappings/confirm",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
            json={
                "file_id": file_id,
                "name": "fixture 매핑 (중복 합성)",
                "mapping": mapping_fixture,
            },
        )
        assert resp.status_code == 200, resp.text
        TestDuplicateSynthesis.mapping_id = resp.json()["id"]

    def test_repeat_synthesis(self, client: TestClient, fixtures_dir):
        for i in range(REPEAT_COUNT):
            file_id, _ = _upload_csv(client, TestDuplicateSynthesis.access_token, fixtures_dir)
            resp = client.post(
                "/api/v1/synthesis",
                headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
                json={
                    "mapping_id": TestDuplicateSynthesis.mapping_id,
                    "uploads": [{"file_id": file_id}],
                },
            )
            assert resp.status_code == 200, f"합성 #{i+1} 시작 실패: {resp.text}"
            job_id = resp.json()["items"][0]["id"]

            status = client.get(
                f"/api/v1/synthesis/{job_id}",
                headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
            )
            assert status.status_code == 200
            assert status.json()["status"] == "COMPLETED"

    def test_part_count_after_repeat(self, client: TestClient):
        resp = client.get(
            "/api/v1/parts",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
            params={"limit": 50},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()

        actual_pns = {item["part_number"] for item in data["items"]}
        assert data["total"] == len(EXPECTED_PART_NUMBERS)
        assert actual_pns == EXPECTED_PART_NUMBERS

        TestDuplicateSynthesis.part_id_map = {
            item["part_number"]: item["id"] for item in data["items"]
        }

    def test_bom_relationships(self, client: TestClient):
        pid = TestDuplicateSynthesis.part_id_map
        headers = {"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"}

        asm001 = client.get(f"/api/v1/parts/{pid['ASM-001']}", headers=headers)
        assert asm001.status_code == 200
        children_asm001 = {c["part_number"] for c in asm001.json()["children"]}
        assert children_asm001 == EXPECTED_CHILDREN["ASM-001"]

        asm002 = client.get(f"/api/v1/parts/{pid['ASM-002']}", headers=headers)
        assert asm002.status_code == 200
        children_asm002 = {c["part_number"] for c in asm002.json()["children"]}
        assert children_asm002 == EXPECTED_CHILDREN["ASM-002"]

        prt001 = client.get(f"/api/v1/parts/{pid['PRT-001']}", headers=headers)
        assert prt001.status_code == 200
        children_prt001 = {c["part_number"] for c in prt001.json()["children"]}
        assert children_prt001 == EXPECTED_CHILDREN["PRT-001"]


def _upload_csv(client: TestClient, token: str, fixtures_dir) -> tuple[str, str]:
    """CSV 업로드 후 (file_id, file_key) 반환."""
    csv_path = fixtures_dir / "hierarchical_bom.csv"
    file_size = csv_path.stat().st_size
    content = csv_path.read_bytes()

    resp = client.post(
        "/api/v1/files/upload",
        headers={"Authorization": f"Bearer {token}"},
        json={
            "original_name": "hierarchical_bom.csv",
            "content_type": "text/csv",
            "file_size": file_size,
        },
    )
    assert resp.status_code == 200, resp.text
    data = resp.json()
    file_id = data["file_id"]

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
        f"/api/v1/files/upload/{file_id}/complete",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert complete_resp.status_code == 200, complete_resp.text

    return file_id, data["file_key"]
