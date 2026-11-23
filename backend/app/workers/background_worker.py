import logging
import time
from datetime import datetime, timezone
from sqlalchemy.orm import Session

from app.core.logging import configure_logging
from app.core.redis_client import redis_client
from app.database.session import SessionLocal
from app.models.device_notification import NotificationRecord
from app.models.user_session import UserSession

logger = logging.getLogger("golu.worker")


def process_pending_notifications(db: Session, batch_size: int = 50) -> int:
    now = datetime.now(timezone.utc)
    pending = (
        db.query(NotificationRecord)
        .filter(NotificationRecord.status == "PENDING")
        .order_by(NotificationRecord.created_at.asc())
        .limit(batch_size)
        .all()
    )
    processed = 0
    for rec in pending:
        sched = rec.scheduled_for
        if sched is not None:
            if sched.tzinfo is None:
                sched = sched.replace(tzinfo=timezone.utc)
            if sched > now:
                continue
        rec.status = "SENT"
        rec.delivered_at = now
        processed += 1
    if processed:
        db.commit()
    return processed


def cleanup_expired_sessions(db: Session) -> int:
    now = datetime.now(timezone.utc)
    expired = (
        db.query(UserSession)
        .filter(UserSession.revoked.is_(False), UserSession.expires_at < now)
        .all()
    )
    for session in expired:
        session.revoked = True
    if expired:
        db.commit()
    return len(expired)


def run_once(db: Session) -> dict[str, int]:
    # Drain optional Redis notification signal without failing if Redis is absent
    redis_client.dequeue_job("golu:notifications", timeout_seconds=1)
    sent_count = process_pending_notifications(db)
    cleaned_count = cleanup_expired_sessions(db)
    return {"notifications_processed": sent_count, "sessions_cleaned": cleaned_count}


def main() -> None:
    configure_logging()
    logger.info("GOLU background worker started.")
    while True:
        db = SessionLocal()
        try:
            summary = run_once(db)
            if summary["notifications_processed"] or summary["sessions_cleaned"]:
                logger.info("Worker cycle summary: %s", summary)
        except Exception as exc:
            logger.warning("Worker cycle warning: %s", type(exc).__name__)
        finally:
            db.close()
        time.sleep(10)


if __name__ == "__main__":
    main()
