"""Stale 업로드 정리 job.

PENDING 상태로 일정 기간 경과한 업로드의 S3 파일과 DB 레코드를 물리 삭제합니다.
"""

from loguru import logger
from sqlalchemy import select

from app.core.database import SessionLocal
from app.modules.organization.models import Organization
from app.modules.organization.provisioning import org_id_to_schema
from app.modules.file import service as file_service


def job() -> None:
    """모든 테넌트의 stale 업로드 정리."""
    db = SessionLocal()
    try:
        orgs = db.execute(select(Organization)).scalars().all()
    finally:
        db.close()

    for org in orgs:
        schema = org_id_to_schema(org.id)
        try:
            file_service.cleanup_stale_files(tenant_schema=schema)
        except Exception:
            logger.exception("stale 정리 실패: {schema}", schema=schema)
