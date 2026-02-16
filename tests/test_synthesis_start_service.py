"""합성 시작/배치 서비스 회귀 테스트."""

import types
import unittest
import uuid
from datetime import datetime, timezone
from unittest.mock import Mock, patch

import pandas as pd

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.modules.part import repository as part_repo
from app.modules.synthesis.schemas import (
    SynthesisBatchStartRequest,
    SynthesisStartRequest,
)
from app.modules.synthesis import service


class _FakeSession:
    def __init__(self) -> None:
        self.commit_count = 0
        self.rollback_count = 0
        self.close_count = 0
        self.refreshed: list[object] = []

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


class SynthesisStartServiceTests(unittest.TestCase):
    def setUp(self) -> None:
        self.auth = AuthContext(
            account_id=uuid.uuid4(),
            email="user@example.com",
            org_id=uuid.uuid4(),
        )

    def test_start_synthesis_uses_org_latest_mapping(self) -> None:
        db = _FakeSession()
        req = SynthesisStartRequest(upload_id=uuid.uuid4())
        mapping_record = types.SimpleNamespace(
            id=uuid.uuid4(),
            upload_id=uuid.uuid4(),
            sheet_name="Sheet1",
            mapping={
                "column_mappings": [],
                "relation_mappings": [],
                "extended_properties": [],
            },
        )
        upload = types.SimpleNamespace(
            id=req.upload_id,
            status="UPLOADED",
            file_key="tenants/org/raw_data/file.xlsx",
            original_name="file.xlsx",
        )
        job = types.SimpleNamespace(
            id=uuid.uuid4(),
            mapping_id=mapping_record.id,
            upload_id=req.upload_id,
            status="PENDING",
            total_rows=0,
            processed_rows=0,
            nodes_created=0,
            relationships_created=0,
            errors=[],
            started_at=None,
            completed_at=None,
            created_at=datetime.now(timezone.utc),
        )
        add_background_task = Mock()

        with (
            patch(
                "app.modules.synthesis.service.repo.get_latest_mapping",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_upload_by_id",
                return_value=upload,
            ),
            patch(
                "app.modules.synthesis.service.repo.create_synthesis_job",
                return_value=job,
            ) as create_job,
            patch(
                "app.modules.synthesis.service.repo.increment_mapping_usage"
            ) as inc_usage,
            patch(
                "app.modules.synthesis.service.generate_uuid7",
                return_value=uuid.uuid4(),
            ),
            patch(
                "app.modules.synthesis.service.org_id_to_schema",
                return_value="tenant_org",
            ),
            patch("app.modules.synthesis.service._to_job_response", return_value="ok"),
        ):
            result = service.start_synthesis(db, self.auth, req, add_background_task)

        self.assertEqual(result, "ok")
        self.assertEqual(db.commit_count, 1)
        self.assertEqual(db.refreshed, [job])
        create_job.assert_called_once()
        self.assertEqual(create_job.call_args.kwargs["mapping_id"], mapping_record.id)
        self.assertEqual(create_job.call_args.kwargs["upload_id"], upload.id)
        inc_usage.assert_called_once_with(db, mapping_record)
        add_background_task.assert_called_once()

    def test_start_synthesis_raises_when_latest_mapping_missing(self) -> None:
        db = _FakeSession()
        req = SynthesisStartRequest(upload_id=uuid.uuid4())

        with patch(
            "app.modules.synthesis.service.repo.get_latest_mapping", return_value=None
        ):
            with self.assertRaises(AppError) as ctx:
                service.start_synthesis(db, self.auth, req, Mock())

        self.assertEqual(ctx.exception.code, "NOT_FOUND")

    def test_start_synthesis_raises_when_upload_not_completed(self) -> None:
        db = _FakeSession()
        req = SynthesisStartRequest(upload_id=uuid.uuid4())
        mapping_record = types.SimpleNamespace(
            id=uuid.uuid4(),
            upload_id=uuid.uuid4(),
            sheet_name=None,
            mapping={
                "column_mappings": [],
                "relation_mappings": [],
                "extended_properties": [],
            },
        )
        upload = types.SimpleNamespace(
            id=req.upload_id,
            status="PENDING",
            file_key="tenants/org/raw_data/file.xlsx",
            original_name="file.xlsx",
        )

        with (
            patch(
                "app.modules.synthesis.service.repo.get_latest_mapping",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_upload_by_id",
                return_value=upload,
            ),
        ):
            with self.assertRaises(AppError) as ctx:
                service.start_synthesis(db, self.auth, req, Mock())

        self.assertEqual(ctx.exception.code, "PRECONDITION_FAILED")

    def test_start_synthesis_batch_creates_jobs_and_batch(self) -> None:
        db = _FakeSession()
        project_id = uuid.uuid4()
        upload_id_1 = uuid.uuid4()
        upload_id_2 = uuid.uuid4()
        req = SynthesisBatchStartRequest(
            upload_ids=[upload_id_1, upload_id_2],
            mapping_id=None,
        )

        mapping_record = types.SimpleNamespace(
            id=uuid.uuid4(),
            sheet_name="Sheet1",
            mapping={
                "column_mappings": [],
                "relation_mappings": [],
                "extended_properties": [],
            },
        )
        upload_1 = types.SimpleNamespace(
            id=upload_id_1,
            owner_type="project",
            owner_id=project_id,
            status="UPLOADED",
            file_key="k1",
            original_name="a.xlsx",
        )
        upload_2 = types.SimpleNamespace(
            id=upload_id_2,
            owner_type="project",
            owner_id=project_id,
            status="UPLOADED",
            file_key="k2",
            original_name="b.xlsx",
        )
        batch = types.SimpleNamespace(
            id=uuid.uuid4(),
            requested_count=2,
            accepted_count=2,
        )
        jobs = [
            types.SimpleNamespace(
                id=uuid.uuid4(),
                mapping_id=mapping_record.id,
                upload_id=upload_id_1,
                status="PENDING",
                total_rows=0,
                processed_rows=0,
                nodes_created=0,
                relationships_created=0,
                errors=[],
                started_at=None,
                completed_at=None,
                created_at=datetime.now(timezone.utc),
            ),
            types.SimpleNamespace(
                id=uuid.uuid4(),
                mapping_id=mapping_record.id,
                upload_id=upload_id_2,
                status="PENDING",
                total_rows=0,
                processed_rows=0,
                nodes_created=0,
                relationships_created=0,
                errors=[],
                started_at=None,
                completed_at=None,
                created_at=datetime.now(timezone.utc),
            ),
        ]
        add_background_task = Mock()

        def _get_upload(_db, upload_id):
            if upload_id == upload_id_1:
                return upload_1
            if upload_id == upload_id_2:
                return upload_2
            return None

        with (
            patch(
                "app.modules.synthesis.service.repo.get_project_by_id",
                return_value=types.SimpleNamespace(id=project_id),
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_mapping_by_project",
                return_value=mapping_record,
            ) as get_project_mapping,
            patch(
                "app.modules.synthesis.service.repo.get_latest_mapping",
                return_value=None,
            ) as get_org_mapping,
            patch(
                "app.modules.synthesis.service.repo.get_upload_by_id",
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
                side_effect=[uuid.uuid4(), uuid.uuid4(), batch.id],
            ),
            patch(
                "app.modules.synthesis.service.org_id_to_schema",
                return_value="tenant_org",
            ),
        ):
            res = service.start_synthesis_batch(
                db,
                self.auth,
                project_id,
                req,
                add_background_task,
            )

        self.assertEqual(res.accepted_count, 2)
        self.assertEqual(len(res.items), 2)
        self.assertEqual(len(res.failed), 0)
        self.assertEqual(db.commit_count, 1)
        self.assertEqual(add_background_task.call_count, 2)
        inc_usage.assert_called_once_with(db, mapping_record, 2)
        get_project_mapping.assert_called_once_with(db, project_id)
        get_org_mapping.assert_not_called()

    def test_start_synthesis_batch_collects_failed_uploads(self) -> None:
        db = _FakeSession()
        project_id = uuid.uuid4()
        ok_upload_id = uuid.uuid4()
        bad_upload_id = uuid.uuid4()
        req = SynthesisBatchStartRequest(
            upload_ids=[ok_upload_id, bad_upload_id],
            mapping_id=None,
        )

        mapping_record = types.SimpleNamespace(
            id=uuid.uuid4(),
            sheet_name=None,
            mapping={
                "column_mappings": [],
                "relation_mappings": [],
                "extended_properties": [],
            },
        )
        ok_upload = types.SimpleNamespace(
            id=ok_upload_id,
            owner_type="project",
            owner_id=project_id,
            status="UPLOADED",
            file_key="k1",
            original_name="a.xlsx",
        )
        batch = types.SimpleNamespace(
            id=uuid.uuid4(),
            requested_count=2,
            accepted_count=1,
        )
        job = types.SimpleNamespace(
            id=uuid.uuid4(),
            mapping_id=mapping_record.id,
            upload_id=ok_upload_id,
            status="PENDING",
            total_rows=0,
            processed_rows=0,
            nodes_created=0,
            relationships_created=0,
            errors=[],
            started_at=None,
            completed_at=None,
            created_at=datetime.now(timezone.utc),
        )

        def _get_upload(_db, upload_id):
            if upload_id == ok_upload_id:
                return ok_upload
            return None

        with (
            patch(
                "app.modules.synthesis.service.repo.get_project_by_id",
                return_value=types.SimpleNamespace(id=project_id),
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_mapping_by_project",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_mapping",
                return_value=None,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_upload_by_id",
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
                side_effect=[uuid.uuid4(), batch.id],
            ),
            patch(
                "app.modules.synthesis.service.org_id_to_schema",
                return_value="tenant_org",
            ),
        ):
            res = service.start_synthesis_batch(
                db,
                self.auth,
                project_id,
                req,
                Mock(),
            )

        self.assertEqual(res.accepted_count, 1)
        self.assertEqual(len(res.failed), 1)
        self.assertEqual(res.failed[0].upload_id, bad_upload_id)

    def test_start_synthesis_batch_falls_back_to_org_mapping(self) -> None:
        db = _FakeSession()
        project_id = uuid.uuid4()
        upload_id = uuid.uuid4()
        req = SynthesisBatchStartRequest(upload_ids=[upload_id], mapping_id=None)

        org_mapping = types.SimpleNamespace(
            id=uuid.uuid4(),
            sheet_name=None,
            mapping={
                "column_mappings": [],
                "relation_mappings": [],
                "extended_properties": [],
            },
        )
        upload = types.SimpleNamespace(
            id=upload_id,
            owner_type="project",
            owner_id=project_id,
            status="UPLOADED",
            file_key="k1",
            original_name="a.xlsx",
        )
        batch = types.SimpleNamespace(
            id=uuid.uuid4(),
            requested_count=1,
            accepted_count=1,
        )
        job = types.SimpleNamespace(
            id=uuid.uuid4(),
            mapping_id=org_mapping.id,
            upload_id=upload_id,
            status="PENDING",
            total_rows=0,
            processed_rows=0,
            nodes_created=0,
            relationships_created=0,
            errors=[],
            started_at=None,
            completed_at=None,
            created_at=datetime.now(timezone.utc),
        )

        with (
            patch(
                "app.modules.synthesis.service.repo.get_project_by_id",
                return_value=types.SimpleNamespace(id=project_id),
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_mapping_by_project",
                return_value=None,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_mapping",
                return_value=org_mapping,
            ) as get_org_mapping,
            patch(
                "app.modules.synthesis.service.repo.get_upload_by_id",
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
                side_effect=[uuid.uuid4(), batch.id],
            ),
            patch(
                "app.modules.synthesis.service.org_id_to_schema",
                return_value="tenant_org",
            ),
        ):
            res = service.start_synthesis_batch(db, self.auth, project_id, req, Mock())

        self.assertEqual(res.accepted_count, 1)
        get_org_mapping.assert_called_once_with(db)

    def test_start_synthesis_batch_rejects_upload_from_other_project(self) -> None:
        db = _FakeSession()
        project_id = uuid.uuid4()
        other_project_id = uuid.uuid4()
        upload_id = uuid.uuid4()
        req = SynthesisBatchStartRequest(upload_ids=[upload_id], mapping_id=None)

        mapping_record = types.SimpleNamespace(
            id=uuid.uuid4(),
            sheet_name=None,
            mapping={
                "column_mappings": [],
                "relation_mappings": [],
                "extended_properties": [],
            },
        )
        upload = types.SimpleNamespace(
            id=upload_id,
            owner_type="project",
            owner_id=other_project_id,
            status="UPLOADED",
            file_key="k1",
            original_name="a.xlsx",
        )
        batch = types.SimpleNamespace(
            id=uuid.uuid4(),
            requested_count=1,
            accepted_count=0,
        )

        with (
            patch(
                "app.modules.synthesis.service.repo.get_project_by_id",
                return_value=types.SimpleNamespace(id=project_id),
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_mapping_by_project",
                return_value=mapping_record,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_latest_mapping",
                return_value=None,
            ),
            patch(
                "app.modules.synthesis.service.repo.get_upload_by_id",
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
            res = service.start_synthesis_batch(db, self.auth, project_id, req, Mock())

        self.assertEqual(res.accepted_count, 0)
        self.assertEqual(len(res.failed), 1)
        self.assertEqual(
            res.failed[0].reason, "해당 프로젝트에 속하지 않은 업로드입니다"
        )

    def test_get_synthesis_batch_returns_aggregated_progress(self) -> None:
        db = _FakeSession()
        batch_id = uuid.uuid4()
        batch = types.SimpleNamespace(
            id=batch_id,
            requested_count=3,
            accepted_count=2,
            failed_uploads=[
                {"upload_id": str(uuid.uuid4()), "reason": "업로드를 찾을 수 없습니다"}
            ],
            created_at=datetime.now(timezone.utc),
        )
        jobs = [
            types.SimpleNamespace(
                id=uuid.uuid4(),
                upload_id=uuid.uuid4(),
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
                upload_id=uuid.uuid4(),
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
                "app.modules.synthesis.service.repo.get_synthesis_batch_by_id",
                return_value=batch,
            ),
            patch(
                "app.modules.synthesis.service.repo.list_synthesis_jobs_by_batch_id",
                return_value=jobs,
            ),
        ):
            res = service.get_synthesis_batch(db, batch_id)

        self.assertEqual(res.accepted_count, 2)
        self.assertEqual(res.completed_count, 1)
        self.assertEqual(res.failed_job_count, 1)
        self.assertEqual(res.failed_count, 1)
        self.assertEqual(res.status, "COMPLETED_WITH_ERRORS")

    def test_run_synthesis_skips_missing_bom_part_in_service(self) -> None:
        db = _FakeSession()
        job_id = uuid.uuid4()
        job = types.SimpleNamespace(
            id=job_id,
            status="PENDING",
            total_rows=0,
            processed_rows=0,
            nodes_created=0,
            relationships_created=0,
            errors=[],
            started_at=None,
            completed_at=None,
            created_at=datetime.now(timezone.utc),
        )

        bom_entries = [
            {"parent_pn": "ASM-001", "child_pn": "COMP-001", "quantity": 1},
            {"parent_pn": "ASM-001", "child_pn": "COMP-002", "quantity": 2},
        ]

        with (
            patch(
                "app.modules.synthesis.service.create_tenant_session", return_value=db
            ),
            patch(
                "app.modules.synthesis.service.repo.get_synthesis_job_required",
                return_value=job,
            ),
            patch("app.modules.synthesis.service._s3.get_object", return_value=b"x"),
            patch("app.modules.synthesis.service.get_sheet_names", return_value=[]),
            patch(
                "app.modules.synthesis.service.read_to_dataframe",
                return_value=pd.DataFrame([{"col": "v"}]),
            ),
            patch(
                "app.modules.synthesis.service._extract_row_part",
                return_value=("COMP-001", {"part_number": "COMP-001"}),
            ),
            patch(
                "app.modules.synthesis.service._extract_related_parts",
                return_value={},
            ),
            patch(
                "app.modules.synthesis.service._extract_bom_data",
                return_value=bom_entries,
            ),
            patch("app.modules.synthesis.service._process_row_nodes", return_value=[]),
            patch(
                "app.modules.synthesis.service._process_row_relationships",
                return_value=[],
            ),
            patch("app.modules.synthesis.service.part_repo.upsert_part"),
            patch("app.modules.synthesis.service.repo.execute_graph_cyphers"),
            patch(
                "app.modules.synthesis.service.part_repo.upsert_bom_link",
                side_effect=[
                    None,
                    part_repo.MissingPartForBomError("ASM-001", "COMP-002"),
                ],
            ) as upsert_bom_link,
            patch("app.modules.synthesis.service.logger.warning") as warning,
        ):
            service._run_synthesis(
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
