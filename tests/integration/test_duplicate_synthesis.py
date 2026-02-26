"""중복 합성 통합 테스트 — hierarchical_bom.csv를 3회 업로드/합성.

검증 항목:
  - LLM 매핑이 올바르게 생성되는지 (property_mappings, relation_mappings)
  - 상위 Part(ASM-001, ASM-002)가 정확히 1건씩만 존재하는지
  - 하위 Part 총 개수가 중복 없이 정확한지 (10건)
  - BOM 관계(children/parents)가 올바르게 매핑되었는지
  - 3회 반복 합성 후에도 데이터 정합성이 유지되는지

실행:
  uv run pytest tests/integration/test_duplicate_synthesis.py -v --use-llm
"""

import httpx
import pytest
from fastapi.testclient import TestClient


# ── hierarchical_bom.csv 기대 데이터 ──

# 고유 품번 10건
EXPECTED_PART_NUMBERS = {
    "ASM-001", "ASM-002",
    "PRT-001", "PRT-002", "PRT-003", "PRT-004",
    "PRT-005", "PRT-006", "PRT-007", "PRT-008",
}

# 상위(부모) Part: CONSISTS_OF 관계의 from 노드
PARENT_PARTS = {"ASM-001", "ASM-002", "PRT-001", "PRT-002"}

# 각 부모의 자식 관계
EXPECTED_CHILDREN = {
    "ASM-001": {"PRT-001", "PRT-002", "PRT-003", "PRT-004"},
    "ASM-002": {"PRT-008", "PRT-003"},
    "PRT-001": {"PRT-005", "PRT-006"},
    "PRT-002": {"PRT-007"},
}

# 합성 반복 횟수
REPEAT_COUNT = 3


class TestDuplicateSynthesis:
    """hierarchical_bom.csv 3회 중복 합성 — LLM 포함 통합 테스트."""

    access_token: str = ""
    mapping_id: str = ""

    # ── 셋업: 회원가입 ──

    def test_register(self, client: TestClient, unique_suffix: str):
        """회원가입 → 토큰 획득."""
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

    # ── LLM 매핑 ──

    def test_mapping_preview_with_llm(
        self, client: TestClient, use_llm: bool, fixtures_dir
    ):
        """LLM 매핑 프리뷰 → property_mappings / relation_mappings 검증."""
        if not use_llm:
            pytest.skip("LLM 비활성 (--use-llm 없음)")

        # 업로드
        file_id, _ = _upload_csv(
            client, TestDuplicateSynthesis.access_token, fixtures_dir
        )

        # LLM 매핑 프리뷰
        resp = client.post(
            "/api/v1/mappings/preview",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
            json={"file_id": file_id},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        mapping = data["mapping"]

        # 새 스키마 구조 확인
        assert "property_mappings" in mapping, (
            f"property_mappings 키 누락. 반환된 키: {list(mapping.keys())}"
        )
        assert "relation_mappings" in mapping, (
            f"relation_mappings 키 누락. 반환된 키: {list(mapping.keys())}"
        )
        assert len(mapping["property_mappings"]) > 0, "property_mappings가 비어있음"
        assert len(mapping["relation_mappings"]) > 0, "relation_mappings가 비어있음"

        # Part.part_number 매핑 존재 확인
        prop_targets = {pm["target_property"] for pm in mapping["property_mappings"]}
        assert "part_number" in prop_targets, (
            f"part_number 매핑 누락. 매핑된 타겟: {prop_targets}"
        )

        # CONSISTS_OF 관계 매핑 존재 확인
        rel_types = {rm["rel_type"] for rm in mapping["relation_mappings"]}
        assert "CONSISTS_OF" in rel_types, (
            f"CONSISTS_OF 관계 매핑 누락. 매핑된 관계: {rel_types}"
        )

        # CONSISTS_OF의 node_columns에 part_number가 있어야 함
        for rm in mapping["relation_mappings"]:
            if rm["rel_type"] == "CONSISTS_OF":
                assert "part_number" in rm["node_columns"], (
                    f"CONSISTS_OF에 part_number 노드 컬럼 누락: {rm['node_columns']}"
                )

        # 매핑 확정 (이후 합성에 사용)
        resp = client.post(
            "/api/v1/mappings/confirm",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
            json={
                "file_id": file_id,
                "name": "LLM 매핑 (중복 합성 테스트)",
                "mapping": mapping,
            },
        )
        assert resp.status_code == 200, resp.text
        TestDuplicateSynthesis.mapping_id = resp.json()["id"]

    def test_mapping_preview_consistency(
        self, client: TestClient, use_llm: bool, fixtures_dir
    ):
        """LLM 매핑 프리뷰 3회 반복 → 핵심 매핑 일관성 확인."""
        if not use_llm:
            pytest.skip("LLM 비활성 (--use-llm 없음)")

        file_id, _ = _upload_csv(
            client, TestDuplicateSynthesis.access_token, fixtures_dir
        )

        results = []
        for i in range(REPEAT_COUNT):
            resp = client.post(
                "/api/v1/mappings/preview",
                headers={
                    "Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"
                },
                json={"file_id": file_id},
            )
            assert resp.status_code == 200, f"프리뷰 #{i+1} 실패: {resp.text}"
            results.append(resp.json()["mapping"])

        # 핵심 매핑이 3회 모두 존재하는지 확인
        for i, mapping in enumerate(results):
            prop_targets = {
                pm["target_property"] for pm in mapping["property_mappings"]
            }
            rel_types = {rm["rel_type"] for rm in mapping["relation_mappings"]}

            assert "part_number" in prop_targets, (
                f"프리뷰 #{i+1}: part_number 매핑 누락"
            )
            assert "CONSISTS_OF" in rel_types, (
                f"프리뷰 #{i+1}: CONSISTS_OF 관계 누락"
            )

    # ── 3회 반복 합성 ──

    def test_repeat_synthesis_with_fixture(
        self, client: TestClient, use_llm: bool, mapping_fixture, fixtures_dir
    ):
        """fixture 매핑으로 3회 합성 → 데이터 정합성 검증."""
        # LLM 매핑이 확정되었으면 그것을 사용, 아니면 fixture 사용
        if not TestDuplicateSynthesis.mapping_id:
            # fixture 매핑 확정
            file_id, _ = _upload_csv(
                client, TestDuplicateSynthesis.access_token, fixtures_dir
            )
            resp = client.post(
                "/api/v1/mappings/confirm",
                headers={
                    "Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"
                },
                json={
                    "file_id": file_id,
                    "name": "fixture 매핑 (중복 합성 테스트)",
                    "mapping": mapping_fixture,
                },
            )
            assert resp.status_code == 200, resp.text
            TestDuplicateSynthesis.mapping_id = resp.json()["id"]

        # 3회 반복 업로드 + 합성
        for i in range(REPEAT_COUNT):
            file_id, _ = _upload_csv(
                client, TestDuplicateSynthesis.access_token, fixtures_dir
            )

            resp = client.post(
                "/api/v1/synthesis",
                headers={
                    "Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"
                },
                json={
                    "mapping_id": TestDuplicateSynthesis.mapping_id,
                    "uploads": [{"file_id": file_id}],
                },
            )
            assert resp.status_code == 200, f"합성 #{i+1} 시작 실패: {resp.text}"
            batch_data = resp.json()
            assert batch_data["accepted_count"] == 1
            job_id = batch_data["items"][0]["id"]

            # BackgroundTask는 TestClient에서 동기 실행 → 응답 시점에 완료
            resp = client.get(
                f"/api/v1/synthesis/{job_id}",
                headers={
                    "Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"
                },
            )
            assert resp.status_code == 200
            job_data = resp.json()
            assert job_data["status"] == "COMPLETED", (
                f"합성 #{i+1} 실패: status={job_data['status']}, "
                f"errors={job_data.get('errors')}"
            )

    # ── 정합성 검증 ──

    def test_part_count_after_repeat(self, client: TestClient):
        """3회 합성 후 Part 수 = 10건 (중복 없음)."""
        resp = client.get(
            "/api/v1/parts",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
            params={"limit": 50},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json()
        actual_pns = {item["part_number"] for item in data["items"]}

        assert data["total"] == len(EXPECTED_PART_NUMBERS), (
            f"고유 품번 {len(EXPECTED_PART_NUMBERS)}건 기대, "
            f"실제 {data['total']}건. 품번: {actual_pns}"
        )
        assert actual_pns == EXPECTED_PART_NUMBERS, (
            f"품번 불일치.\n기대: {EXPECTED_PART_NUMBERS}\n실제: {actual_pns}"
        )

        # 이후 테스트에서 사용할 part_number → id 맵 저장
        TestDuplicateSynthesis.part_id_map = {
            item["part_number"]: item["id"] for item in data["items"]
        }

    def test_parent_parts_exist(self, client: TestClient):
        """상위 Part(ASM-001, ASM-002)가 정확히 존재하고 속성이 올바른지 확인."""
        headers = {
            "Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"
        }
        pid = TestDuplicateSynthesis.part_id_map

        # ASM-001 상세
        resp = client.get(f"/api/v1/parts/{pid['ASM-001']}", headers=headers)
        assert resp.status_code == 200, resp.text
        asm001 = resp.json()
        assert asm001["part_number"] == "ASM-001"
        assert asm001["name"] == "메인 프레임 조립품"

        # ASM-002 상세
        resp = client.get(f"/api/v1/parts/{pid['ASM-002']}", headers=headers)
        assert resp.status_code == 200, resp.text
        asm002 = resp.json()
        assert asm002["part_number"] == "ASM-002"
        assert asm002["name"] == "서브 조립품"

    def test_asm001_children(self, client: TestClient):
        """ASM-001의 자식이 정확히 4건 (PRT-001~004)."""
        pid = TestDuplicateSynthesis.part_id_map
        resp = client.get(
            f"/api/v1/parts/{pid['ASM-001']}",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        child_pns = {c["part_number"] for c in data["children"]}

        assert child_pns == EXPECTED_CHILDREN["ASM-001"], (
            f"ASM-001 자식 불일치.\n기대: {EXPECTED_CHILDREN['ASM-001']}\n실제: {child_pns}"
        )

        # 수량 검증
        qty_map = {c["part_number"]: c["quantity"] for c in data["children"]}
        assert qty_map.get("PRT-001") == 2, f"PRT-001 수량 2 기대, 실제 {qty_map.get('PRT-001')}"
        assert qty_map.get("PRT-002") == 1, f"PRT-002 수량 1 기대, 실제 {qty_map.get('PRT-002')}"
        assert qty_map.get("PRT-003") == 4, f"PRT-003 수량 4 기대, 실제 {qty_map.get('PRT-003')}"
        assert qty_map.get("PRT-004") == 1, f"PRT-004 수량 1 기대, 실제 {qty_map.get('PRT-004')}"

    def test_asm002_children(self, client: TestClient):
        """ASM-002의 자식이 정확히 2건 (PRT-008, PRT-003)."""
        pid = TestDuplicateSynthesis.part_id_map
        resp = client.get(
            f"/api/v1/parts/{pid['ASM-002']}",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        child_pns = {c["part_number"] for c in data["children"]}

        assert child_pns == EXPECTED_CHILDREN["ASM-002"], (
            f"ASM-002 자식 불일치.\n기대: {EXPECTED_CHILDREN['ASM-002']}\n실제: {child_pns}"
        )

    def test_prt001_children(self, client: TestClient):
        """PRT-001의 자식이 정확히 2건 (PRT-005, PRT-006)."""
        pid = TestDuplicateSynthesis.part_id_map
        resp = client.get(
            f"/api/v1/parts/{pid['PRT-001']}",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        child_pns = {c["part_number"] for c in data["children"]}

        assert child_pns == EXPECTED_CHILDREN["PRT-001"], (
            f"PRT-001 자식 불일치.\n기대: {EXPECTED_CHILDREN['PRT-001']}\n실제: {child_pns}"
        )

    def test_prt001_has_parent_asm001(self, client: TestClient):
        """PRT-001의 부모가 ASM-001."""
        pid = TestDuplicateSynthesis.part_id_map
        resp = client.get(
            f"/api/v1/parts/{pid['PRT-001']}",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        parent_pns = {p["part_number"] for p in data["parents"]}

        assert "ASM-001" in parent_pns, (
            f"PRT-001의 부모에 ASM-001이 없음. 실제 부모: {parent_pns}"
        )

    def test_prt003_has_two_parents(self, client: TestClient):
        """PRT-003(볼베어링)은 ASM-001과 ASM-002 두 곳의 자식."""
        pid = TestDuplicateSynthesis.part_id_map
        resp = client.get(
            f"/api/v1/parts/{pid['PRT-003']}",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        parent_pns = {p["part_number"] for p in data["parents"]}

        assert parent_pns == {"ASM-001", "ASM-002"}, (
            f"PRT-003 부모 불일치.\n기대: {{'ASM-001', 'ASM-002'}}\n실제: {parent_pns}"
        )

        # 부모별 수량 확인
        qty_map = {p["part_number"]: p["quantity"] for p in data["parents"]}
        assert qty_map.get("ASM-001") == 4, (
            f"PRT-003←ASM-001 수량 4 기대, 실제 {qty_map.get('ASM-001')}"
        )
        assert qty_map.get("ASM-002") == 2, (
            f"PRT-003←ASM-002 수량 2 기대, 실제 {qty_map.get('ASM-002')}"
        )

    def test_bom_tree_structure(self, client: TestClient):
        """ASM-001 BOM 트리 — 3단계 구조 확인."""
        pid = TestDuplicateSynthesis.part_id_map
        resp = client.get(
            f"/api/v1/parts/{pid['ASM-001']}/bom",
            headers={"Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"},
        )
        assert resp.status_code == 200, resp.text
        root = resp.json()["root"]
        assert root["part_number"] == "ASM-001"

        # 1단계: ASM-001 → 4 children
        child_pns = {c["part_number"] for c in root["children"]}
        assert child_pns == EXPECTED_CHILDREN["ASM-001"]

        # 2단계: PRT-001 → PRT-005, PRT-006
        prt001 = next(c for c in root["children"] if c["part_number"] == "PRT-001")
        grandchild_pns = {c["part_number"] for c in prt001["children"]}
        assert grandchild_pns == EXPECTED_CHILDREN["PRT-001"], (
            f"PRT-001 하위 불일치.\n기대: {EXPECTED_CHILDREN['PRT-001']}\n실제: {grandchild_pns}"
        )

        # 2단계: PRT-002 → PRT-007
        prt002 = next(c for c in root["children"] if c["part_number"] == "PRT-002")
        grandchild_pns_002 = {c["part_number"] for c in prt002["children"]}
        assert grandchild_pns_002 == EXPECTED_CHILDREN["PRT-002"]

    def test_leaf_parts_have_no_children(self, client: TestClient):
        """리프 Part(PRT-004~008)는 자식이 없어야 함."""
        headers = {
            "Authorization": f"Bearer {TestDuplicateSynthesis.access_token}"
        }
        pid = TestDuplicateSynthesis.part_id_map
        leaf_parts = EXPECTED_PART_NUMBERS - PARENT_PARTS
        for pn in sorted(leaf_parts):
            resp = client.get(f"/api/v1/parts/{pid[pn]}", headers=headers)
            assert resp.status_code == 200
            data = resp.json()
            assert len(data["children"]) == 0, (
                f"리프 Part {pn}에 자식이 존재: "
                f"{[c['part_number'] for c in data['children']]}"
            )


# ── 헬퍼 ──


def _upload_csv(
    client: TestClient, token: str, fixtures_dir
) -> tuple[str, str]:
    """CSV 업로드 → (file_id, file_key) 반환."""
    csv_path = fixtures_dir / "hierarchical_bom.csv"
    file_size = csv_path.stat().st_size
    content = csv_path.read_bytes()

    # presigned URL 발급
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
    upload_url = data["upload_url"]

    # MinIO PUT
    with httpx.Client() as http:
        resp = http.put(
            upload_url,
            content=content,
            headers={
                "Content-Type": "text/csv",
                "Content-Length": str(len(content)),
            },
        )
    assert resp.status_code == 200, f"MinIO PUT 실패: {resp.status_code}"

    # 업로드 완료
    resp = client.post(
        f"/api/v1/files/upload/{file_id}/complete",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 200, resp.text

    return file_id, data["file_key"]
