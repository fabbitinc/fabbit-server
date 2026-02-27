"""합성 시작 서비스 회귀 테스트."""

import types
import unittest
import uuid
from datetime import datetime, timezone
from unittest.mock import Mock, patch

import pandas as pd

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.modules.mapping.constants import MappingScope
from app.modules.part import repository as part_repo
from app.modules.synthesis.schemas import (
    SynthesisStartRequest,
    SynthesisUploadItem,
)
from app.modules.synthesis import pipeline, service
from app.queries.synthesis import get_synthesis_batch as get_synthesis_batch_query


class _FakeSession:
    def __init__(self) -> None:
        self.commit_count = 0
        self.rollback_count = 0
        self.close_count = 0
        self.refreshed: list[object] = []
        # UnitOfWork._collect_aggregate_events()가 참조하는 속성
        self.new: set = set()
        self.dirty: set = set()
        self.deleted: set = set()
        self.identity_map: dict = {}

    def commit(self) -> None:
        self.commit_count += 1

    def flush(self) -> None:
        return None

    def rollback(self) -> None:
        self.rollback_count += 1

    def close(self) -> None:
        self.close_count += 1

    def refresh(self, obj: object) -> None:
        self.refreshed.append(obj)


def _make_mapping_record(scope: str = MappingScope.PART_LIST):
    return types.SimpleNamespace(id=uuid.uuid4(), scope=scope)


def _make_revision(sheet_name: str | None = "Sheet1", mapping: dict | None = None):
    return types.SimpleNamespace(
        id=uuid.uuid4(),
        sheet_name=sheet_name,
        mapping=mapping or {
            "property_mappings": [],
            "relation_mappings": [],
        },
    )


def _make_upload(upload_id, owner_type="project", owner_id=None, status="UPLOADED"):
    return types.SimpleNamespace(
        id=upload_id,
        owner_type=owner_type,
        owner_id=owner_id,
        status=status,
        file_key=f"tenants/org/raw_data/{upload_id}.xlsx",
        original_name=f"{upload_id}.xlsx",
    )


class _FakeJob:
    """SynthesisJob 도메인 메서드를 구현하는 테스트용 대역."""

    def __init__(self, mapping_id, file_id) -> None:
        self.id = uuid.uuid4()
        self.mapping_id = mapping_id
        self.file_id = file_id
        self.batch_id = None
        self.status = "PENDING"
        self.total_rows = 0
        self.processed_rows = 0
        self.nodes_created = 0
        self.relationships_created = 0
        self.errors: list = []
        self.started_at = None
        self.completed_at = None
        self.created_at = datetime.now(timezone.utc)

    def assign_batch(self, batch_id) -> None:
        self.batch_id = batch_id

    def start_processing(self) -> None:
        self.status = "PROCESSING"
        self.started_at = datetime.now(timezone.utc)

    def set_total_rows(self, total_rows) -> None:
        self.total_rows = total_rows

    def update_progress(self, *, processed_rows, nodes_created, relationships_created, errors) -> None:
        self.processed_rows = processed_rows
        self.nodes_created = nodes_created
        self.relationships_created = relationships_created
        self.errors = errors[:100]

    def complete(self) -> None:
        self.status = "COMPLETED"
        self.completed_at = datetime.now(timezone.utc)

    def complete_empty(self) -> None:
        self.total_rows = 0
        self.status = "COMPLETED"
        self.completed_at = datetime.now(timezone.utc)

    def fail(self, errors) -> None:
        self.status = "FAILED"
        self.errors = errors[:100]
        self.completed_at = datetime.now(timezone.utc)


def _make_job(mapping_id, upload_id):
    return _FakeJob(mapping_id, upload_id)


class SynthesisStartServiceTests(unittest.TestCase):
    def setUp(self) -> None:
        self.auth = AuthContext(
            user_id=uuid.uuid4(),
            email="user@example.com",
            org_id=uuid.uuid4(),
            role="ADMIN",
        )

    def test_start_synthesis_single_upload(self) -> None:
        """단건 업로드 — mapping_id 지정."""
        db = _FakeSession()
        upload_id = uuid.uuid4()
        mapping_record = _make_mapping_record()
        req = SynthesisStartRequest(
            mapping_id=mapping_record.id,
            uploads=[SynthesisUploadItem(file_id=upload_id)],
        )
        revision = _make_revision()
        upload = _make_upload(upload_id)
        job = _make_job(mapping_record.id, upload_id)
        batch = types.SimpleNamespace(
            id=uuid.uuid4(), requested_count=1, accepted_count=1
        )
        add_background_task = Mock()

        with (
            patch(
                "app.modules.synthesis.service.repo.get_mapping_by_id",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_revision",
                return_value=revision,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_file_by_id",
                return_value=upload,
            ),
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_job",
                return_value=job,
            ) as create_job,
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_batch",
                return_value=batch,
            ),
            patch(
                "app.modules.synthesis.service.repo.increment_mapping_usage"
            ) as inc_usage,
            patch(
                "app.modules.synthesis.service.generate_uuid7",
                return_value=batch.id,
            ),
            patch(
                "app.modules.synthesis.service.org_id_to_schema",
                return_value="tenant_org",
            ),
        ):
            result = service.start_synthesis(db, self.auth, req, add_background_task)

        self.assertEqual(result.accepted_count, 1)
        self.assertEqual(result.batch_id, batch.id)
        create_job.assert_called_once()
        self.assertEqual(create_job.call_args.kwargs["mapping_id"], mapping_record.id)
        inc_usage.assert_called_once_with(db, mapping_record, revision, 1)
        add_background_task.assert_called_once()

    def test_start_synthesis_raises_when_mapping_not_found(self) -> None:
        db = _FakeSession()
        req = SynthesisStartRequest(
            mapping_id=uuid.uuid4(),
            uploads=[SynthesisUploadItem(file_id=uuid.uuid4())],
        )

        with patch(
            "app.modules.synthesis.service.repo.get_mapping_by_id", return_value=None
        ):
            with self.assertRaises(AppError) as ctx:
                service.start_synthesis(db, self.auth, req, Mock())

        self.assertEqual(ctx.exception.code, "NOT_FOUND")

    def test_start_synthesis_collects_incomplete_upload_as_failed(self) -> None:
        db = _FakeSession()
        upload_id = uuid.uuid4()
        mapping_record = _make_mapping_record()
        req = SynthesisStartRequest(
            mapping_id=mapping_record.id,
            uploads=[SynthesisUploadItem(file_id=upload_id)],
        )
        revision = _make_revision(sheet_name=None)
        upload = _make_upload(upload_id, status="PENDING")
        batch = types.SimpleNamespace(
            id=uuid.uuid4(), requested_count=1, accepted_count=0
        )

        with (
            patch(
                "app.modules.synthesis.service.repo.get_mapping_by_id",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_revision",
                return_value=revision,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_file_by_id",
                return_value=upload,
            ),
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_batch",
                return_value=batch,
            ),
            patch(
                "app.modules.synthesis.service.generate_uuid7",
                return_value=batch.id,
            ),
            patch(
                "app.modules.synthesis.service.org_id_to_schema",
                return_value="tenant_org",
            ),
        ):
            result = service.start_synthesis(db, self.auth, req, Mock())

        self.assertEqual(result.accepted_count, 0)
        self.assertEqual(len(result.failed), 1)
        self.assertIn("완료되지 않은", result.failed[0].reason)

    def test_start_synthesis_batch_creates_jobs_and_batch(self) -> None:
        db = _FakeSession()
        project_id = uuid.uuid4()
        upload_id_1 = uuid.uuid4()
        upload_id_2 = uuid.uuid4()
        mapping_record = _make_mapping_record()
        req = SynthesisStartRequest(
            mapping_id=mapping_record.id,
            project_id=project_id,
            uploads=[
                SynthesisUploadItem(file_id=upload_id_1),
                SynthesisUploadItem(file_id=upload_id_2),
            ],
        )

        revision = _make_revision()
        upload_1 = _make_upload(upload_id_1, owner_id=project_id)
        upload_2 = _make_upload(upload_id_2, owner_id=project_id)
        batch = types.SimpleNamespace(
            id=uuid.uuid4(), requested_count=2, accepted_count=2
        )
        jobs = [
            _make_job(mapping_record.id, upload_id_1),
            _make_job(mapping_record.id, upload_id_2),
        ]
        add_background_task = Mock()

        def _get_upload(_db, uid):
            if uid == upload_id_1:
                return upload_1
            if uid == upload_id_2:
                return upload_2
            return None

        with (
            patch(
                "app.modules.synthesis.service.repo.get_mapping_by_id",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_project_by_id",
                return_value=types.SimpleNamespace(id=project_id),
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_revision",
                return_value=revision,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_file_by_id",
                side_effect=_get_upload,
            ),
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_job",
                side_effect=jobs,
            ),
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_batch",
                return_value=batch,
            ),
            patch(
                "app.modules.synthesis.service.repo.increment_mapping_usage"
            ) as inc_usage,
            patch(
                "app.modules.synthesis.service.generate_uuid7",
                return_value=batch.id,
            ),
            patch(
                "app.modules.synthesis.service.org_id_to_schema",
                return_value="tenant_org",
            ),
        ):
            res = service.start_synthesis(db, self.auth, req, add_background_task)

        self.assertEqual(res.accepted_count, 2)
        self.assertEqual(len(res.items), 2)
        self.assertEqual(len(res.failed), 0)
        # commit은 use_case 레이어(@transactional)에서 처리
        self.assertEqual(add_background_task.call_count, 2)
        inc_usage.assert_called_once_with(db, mapping_record, revision, 2)

    def test_start_synthesis_batch_collects_failed_uploads(self) -> None:
        db = _FakeSession()
        project_id = uuid.uuid4()
        ok_upload_id = uuid.uuid4()
        bad_upload_id = uuid.uuid4()
        mapping_record = _make_mapping_record()
        req = SynthesisStartRequest(
            mapping_id=mapping_record.id,
            project_id=project_id,
            uploads=[
                SynthesisUploadItem(file_id=ok_upload_id),
                SynthesisUploadItem(file_id=bad_upload_id),
            ],
        )

        revision = _make_revision(sheet_name=None)
        ok_upload = _make_upload(ok_upload_id, owner_id=project_id)
        batch = types.SimpleNamespace(
            id=uuid.uuid4(), requested_count=2, accepted_count=1
        )
        job = _make_job(mapping_record.id, ok_upload_id)

        def _get_upload(_db, uid):
            if uid == ok_upload_id:
                return ok_upload
            return None

        with (
            patch(
                "app.modules.synthesis.service.repo.get_mapping_by_id",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_project_by_id",
                return_value=types.SimpleNamespace(id=project_id),
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_revision",
                return_value=revision,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_file_by_id",
                side_effect=_get_upload,
            ),
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_job",
                return_value=job,
            ),
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_batch",
                return_value=batch,
            ),
            patch("app.modules.synthesis.service.repo.increment_mapping_usage"),
            patch(
                "app.modules.synthesis.service.generate_uuid7",
                return_value=batch.id,
            ),
            patch(
                "app.modules.synthesis.service.org_id_to_schema",
                return_value="tenant_org",
            ),
        ):
            res = service.start_synthesis(db, self.auth, req, Mock())

        self.assertEqual(res.accepted_count, 1)
        self.assertEqual(len(res.failed), 1)
        self.assertEqual(res.failed[0].file_id, bad_upload_id)

    def test_start_synthesis_rejects_upload_from_other_project(self) -> None:
        db = _FakeSession()
        project_id = uuid.uuid4()
        other_project_id = uuid.uuid4()
        upload_id = uuid.uuid4()
        mapping_record = _make_mapping_record()
        req = SynthesisStartRequest(
            mapping_id=mapping_record.id,
            project_id=project_id,
            uploads=[SynthesisUploadItem(file_id=upload_id)],
        )

        revision = _make_revision(sheet_name=None)
        upload = _make_upload(upload_id, owner_id=other_project_id)
        batch = types.SimpleNamespace(
            id=uuid.uuid4(), requested_count=1, accepted_count=0
        )

        with (
            patch(
                "app.modules.synthesis.service.repo.get_mapping_by_id",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_project_by_id",
                return_value=types.SimpleNamespace(id=project_id),
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_revision",
                return_value=revision,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_file_by_id",
                return_value=upload,
            ),
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_batch",
                return_value=batch,
            ),
            patch(
                "app.modules.synthesis.service.generate_uuid7", return_value=batch.id
            ),
            patch(
                "app.modules.synthesis.service.org_id_to_schema",
                return_value="tenant_org",
            ),
        ):
            res = service.start_synthesis(db, self.auth, req, Mock())

        self.assertEqual(res.accepted_count, 0)
        self.assertEqual(len(res.failed), 1)
        self.assertEqual(
            res.failed[0].reason, "해당 프로젝트에 속하지 않은 파일입니다"
        )

    # ── root_context 검증 테스트 ──

    def test_root_bom_requires_root_context(self) -> None:
        """ROOT_BOM scope에서 root_context 없으면 MISSING_ROOT_CONTEXT 에러."""
        db = _FakeSession()
        mapping_record = _make_mapping_record(scope=MappingScope.ROOT_BOM)
        req = SynthesisStartRequest(
            mapping_id=mapping_record.id,
            uploads=[SynthesisUploadItem(file_id=uuid.uuid4())],
        )
        # rootless CONSISTS_OF → required_labels={"Part"}
        revision = _make_revision(mapping={
            "property_mappings": [],
            "relation_mappings": [{
                "rel_type": "CONSISTS_OF",
                "target_label": "Part",
                "node_columns": {},
                "rel_columns": {"quantity": "수량"},
                "rel_column_types": {"quantity": "integer"},
            }],
        })

        with (
            patch(
                "app.modules.synthesis.service.repo.get_mapping_by_id",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_revision",
                return_value=revision,
            ),
        ):
            with self.assertRaises(AppError) as ctx:
                service.start_synthesis(db, self.auth, req, Mock())

        self.assertEqual(ctx.exception.code, "MISSING_ROOT_CONTEXT")

    def test_root_bom_with_root_context_succeeds(self) -> None:
        """ROOT_BOM scope에서 root_context 있으면 정상 처리."""
        db = _FakeSession()
        upload_id = uuid.uuid4()
        mapping_record = _make_mapping_record(scope=MappingScope.ROOT_BOM)
        req = SynthesisStartRequest(
            mapping_id=mapping_record.id,
            uploads=[
                SynthesisUploadItem(
                    file_id=upload_id,
                    root_context={"Part": "ASM-001"},
                ),
            ],
        )
        # rootless CONSISTS_OF → required_labels={"Part"}
        revision = _make_revision(mapping={
            "property_mappings": [],
            "relation_mappings": [{
                "rel_type": "CONSISTS_OF",
                "target_label": "Part",
                "node_columns": {},
                "rel_columns": {"quantity": "수량"},
                "rel_column_types": {"quantity": "integer"},
            }],
        })
        upload = _make_upload(upload_id)
        job = _make_job(mapping_record.id, upload_id)
        batch = types.SimpleNamespace(
            id=uuid.uuid4(), requested_count=1, accepted_count=1
        )

        with (
            patch(
                "app.modules.synthesis.service.repo.get_mapping_by_id",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_revision",
                return_value=revision,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_file_by_id",
                return_value=upload,
            ),
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_job",
                return_value=job,
            ),
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_batch",
                return_value=batch,
            ),
            patch("app.modules.synthesis.service.repo.increment_mapping_usage"),
            patch(
                "app.modules.synthesis.service.generate_uuid7",
                return_value=batch.id,
            ),
            patch(
                "app.modules.synthesis.service.org_id_to_schema",
                return_value="tenant_org",
            ),
        ):
            res = service.start_synthesis(db, self.auth, req, Mock())

        self.assertEqual(res.accepted_count, 1)

    def test_part_list_rejects_unexpected_root_context(self) -> None:
        """PART_LIST scope에서 root_context 있으면 UNEXPECTED_ROOT_CONTEXT 에러."""
        db = _FakeSession()
        mapping_record = _make_mapping_record(scope=MappingScope.PART_LIST)
        req = SynthesisStartRequest(
            mapping_id=mapping_record.id,
            uploads=[
                SynthesisUploadItem(
                    file_id=uuid.uuid4(),
                    root_context={"Part": "ASM-001"},
                ),
            ],
        )
        revision = _make_revision()

        with (
            patch(
                "app.modules.synthesis.service.repo.get_mapping_by_id",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_revision",
                return_value=revision,
            ),
        ):
            with self.assertRaises(AppError) as ctx:
                service.start_synthesis(db, self.auth, req, Mock())

        self.assertEqual(ctx.exception.code, "UNEXPECTED_ROOT_CONTEXT")

    def test_full_bom_rejects_unexpected_root_context(self) -> None:
        """FULL_BOM scope에서 root_context 있으면 UNEXPECTED_ROOT_CONTEXT 에러."""
        db = _FakeSession()
        mapping_record = _make_mapping_record(scope=MappingScope.FULL_BOM)
        req = SynthesisStartRequest(
            mapping_id=mapping_record.id,
            uploads=[
                SynthesisUploadItem(
                    file_id=uuid.uuid4(),
                    root_context={"Part": "ASM-001"},
                ),
            ],
        )
        revision = _make_revision()

        with (
            patch(
                "app.modules.synthesis.service.repo.get_mapping_by_id",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_revision",
                return_value=revision,
            ),
        ):
            with self.assertRaises(AppError) as ctx:
                service.start_synthesis(db, self.auth, req, Mock())

        self.assertEqual(ctx.exception.code, "UNEXPECTED_ROOT_CONTEXT")

    # ── 배치 상태 조회 ──

    def test_get_synthesis_batch_returns_aggregated_progress(self) -> None:
        db = _FakeSession()
        batch_id = uuid.uuid4()
        batch = types.SimpleNamespace(
            id=batch_id,
            requested_count=3,
            accepted_count=2,
            failed_uploads=[
                {"file_id": str(uuid.uuid4()), "reason": "파일을 찾을 수 없습니다"}
            ],
            created_at=datetime.now(timezone.utc),
        )
        jobs = [
            types.SimpleNamespace(
                id=uuid.uuid4(),
                file_id=uuid.uuid4(),
                status="COMPLETED",
                total_rows=10,
                processed_rows=10,
                nodes_created=5,
                relationships_created=4,
                errors=[],
                started_at=None,
                completed_at=None,
            ),
            types.SimpleNamespace(
                id=uuid.uuid4(),
                file_id=uuid.uuid4(),
                status="FAILED",
                total_rows=8,
                processed_rows=6,
                nodes_created=3,
                relationships_created=2,
                errors=["e1"],
                started_at=None,
                completed_at=None,
            ),
        ]

        with (
            patch(
                "app.queries.synthesis.get_synthesis_batch.repo.get_synthesis_batch_by_id",
                return_value=batch,
            ),
            patch(
                "app.queries.synthesis.get_synthesis_batch.repo.list_synthesis_jobs_by_batch_id",
                return_value=jobs,
            ),
        ):
            res = get_synthesis_batch_query(db, batch_id)

        self.assertEqual(res.accepted_count, 2)
        self.assertEqual(res.completed_count, 1)
        self.assertEqual(res.failed_job_count, 1)
        self.assertEqual(res.failed_count, 1)
        self.assertEqual(res.status, "COMPLETED_WITH_ERRORS")

    # ── _run_synthesis 백그라운드 태스크 ──

    def test_run_synthesis_skips_missing_bom_part_in_service(self) -> None:
        db = _FakeSession()
        job_id = uuid.uuid4()
        job = _FakeJob(mapping_id=uuid.uuid4(), file_id=uuid.uuid4())
        job.id = job_id

        bom_entries = [
            {"parent_pn": "ASM-001", "child_pn": "COMP-001", "quantity": 1},
            {"parent_pn": "ASM-001", "child_pn": "COMP-002", "quantity": 2},
        ]

        with (
            patch(
                "app.modules.synthesis.pipeline.create_tenant_session", return_value=db
            ),
            patch(
                "app.modules.synthesis.pipeline.repo.get_synthesis_job_required",
                return_value=job,
            ),
            patch("app.modules.synthesis.pipeline._s3.get_object", return_value=b"x"),
            patch("app.modules.synthesis.pipeline.get_sheet_names", return_value=[]),
            patch(
                "app.modules.synthesis.pipeline.read_to_dataframe",
                return_value=pd.DataFrame([{"col": "v"}]),
            ),
            patch(
                "app.modules.synthesis.pipeline._extract_row_part",
                return_value=("COMP-001", {"part_number": "COMP-001"}),
            ),
            patch(
                "app.modules.synthesis.pipeline._extract_related_parts",
                return_value={},
            ),
            patch(
                "app.modules.synthesis.pipeline._extract_bom_data",
                return_value=bom_entries,
            ),
            patch("app.modules.synthesis.pipeline._process_row_nodes", return_value=[]),
            patch(
                "app.modules.synthesis.pipeline._process_row_relationships",
                return_value=[],
            ),
            patch("app.modules.synthesis.pipeline.part_repo.upsert_part"),
            patch("app.modules.synthesis.pipeline.repo.execute_graph_cyphers"),
            patch(
                "app.modules.synthesis.pipeline.part_repo.upsert_bom_link",
                side_effect=[
                    None,
                    part_repo.MissingPartForBomError("ASM-001", "COMP-002"),
                ],
            ) as upsert_bom_link,
            patch("app.modules.synthesis.pipeline.logger.warning") as warning,
        ):
            pipeline.run_synthesis(
                job_id=job_id,
                schema_name="tenant_x",
                graph_name="tenant_x",
                file_key="k",
                filename="a.csv",
                sheet_name=None,
                mapping_json={
                    "property_mappings": [],
                    "relation_mappings": [],
                },
            )

        self.assertEqual(upsert_bom_link.call_count, 2)
        self.assertEqual(job.relationships_created, 1)
        self.assertEqual(job.errors, [])
        self.assertEqual(job.status, "COMPLETED")
        self.assertEqual(db.close_count, 1)
        self.assertTrue(
            any("합성 BOM 링크 스킵" in str(c) for c in warning.call_args_list)
        )


if __name__ == "__main__":
    unittest.main()
