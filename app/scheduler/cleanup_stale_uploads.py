"""Stale 업로드 정리 job.

PENDING 상태로 일정 기간 경과한 업로드의 S3 파일을 삭제하고 EXPIRED 처리합니다.
"""

from loguru import logger
from sqlalchemy import select

from app.core.database import SessionLocal
from app.modules.auth.models import Organization
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.upload import service as upload_service


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
            upload_service.cleanup_stale_uploads(tenant_schema=schema)
        except Exception:
            logger.exception("stale 정리 실패: {schema}", schema=schema)
