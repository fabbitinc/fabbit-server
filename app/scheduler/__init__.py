"""APScheduler 기반 인앱 cron 스케줄러."""

from apscheduler.schedulers.background import BackgroundScheduler
from loguru import logger

from app.scheduler.cleanup_converter_cache import job as cleanup_converter_job
from app.scheduler.cleanup_deleted_uploads import job as cleanup_deleted_job
from app.scheduler.cleanup_orphan_uploads import job as cleanup_orphan_job
from app.scheduler.cleanup_stale_uploads import job as cleanup_stale_job

scheduler = BackgroundScheduler()

# 매일 03:00 — PENDING 상태로 방치된 업로드의 S3 파일 + DB 레코드 물리 삭제
scheduler.add_job(
    cleanup_stale_job, "cron", hour=3, minute=0, id="cleanup_stale_uploads"
)
# 매일 03:15 — soft-delete 후 보존 기간 만료된 파일의 S3 + DB 레코드 물리 삭제 (보존기간 기본 7일)
scheduler.add_job(
    cleanup_deleted_job, "cron", hour=3, minute=15, id="cleanup_deleted_uploads"
)
# 매일 03:30 — S3에 존재하지만 DB 레코드가 없는 고아 파일 삭제
scheduler.add_job(
    cleanup_orphan_job, "cron", hour=3, minute=30, id="cleanup_orphan_uploads"
)
# 10분 간격 — 도면 변환 임시 캐시 파일 정리 (1시간 경과분)
scheduler.add_job(
    cleanup_converter_job, "interval", minutes=10, id="cleanup_converter_cache"
)


def start() -> None:
    scheduler.start()
    logger.info("스케줄러 시작")


def shutdown() -> None:
    scheduler.shutdown(wait=False)
    logger.info("스케줄러 종료")
