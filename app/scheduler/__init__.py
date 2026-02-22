"""APScheduler 기반 인앱 cron 스케줄러."""

from apscheduler.schedulers.background import BackgroundScheduler
from loguru import logger

from app.scheduler.cleanup_orphan_uploads import job as cleanup_orphan_job
from app.scheduler.cleanup_deleted_uploads import job as cleanup_deleted_job
from app.scheduler.cleanup_stale_uploads import job as cleanup_stale_job

scheduler = BackgroundScheduler()

# job 등록
scheduler.add_job(cleanup_stale_job, "cron", hour=3, minute=0, id="cleanup_stale_uploads")
scheduler.add_job(cleanup_deleted_job, "cron", hour=3, minute=15, id="cleanup_deleted_uploads")
scheduler.add_job(cleanup_orphan_job, "cron", hour=3, minute=30, id="cleanup_orphan_uploads")


def start() -> None:
    scheduler.start()
    logger.info("스케줄러 시작")


def shutdown() -> None:
    scheduler.shutdown(wait=False)
    logger.info("스케줄러 종료")
