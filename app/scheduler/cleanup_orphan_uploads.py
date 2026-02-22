"""S3 고아 파일 정리 job.

S3에 존재하지만 DB에 레코드가 없는 파일을 삭제합니다.
"""

from loguru import logger
from sqlalchemy import select

from app.core.database import SessionLocal
from app.modules.auth.models import Organization
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.file import service as file_service


def job() -> None:
    """모든 테넌트의 S3 고아 파일 정리."""
    db = SessionLocal()
    try:
        orgs = db.execute(select(Organization)).scalars().all()
    finally:
        db.close()

    for org in orgs:
        schema = org_id_to_schema(org.id)
        s3_prefix = f"tenants/{org.id}/"
        try:
            file_service.cleanup_orphan_files(
                s3_prefix=s3_prefix,
                tenant_schema=schema,
            )
        except Exception:
            logger.exception("orphan 파일 정리 실패: {schema}", schema=schema)
