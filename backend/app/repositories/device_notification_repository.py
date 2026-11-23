from datetime import datetime, timezone
from typing import List, Optional
from sqlalchemy.orm import Session

from app.models.device_notification import DeviceRegistration, NotificationRecord


class DeviceNotificationRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def list_devices(self, user_id: str) -> List[DeviceRegistration]:
        return (
            self.db.query(DeviceRegistration)
            .filter(DeviceRegistration.user_id == user_id)
            .order_by(DeviceRegistration.last_seen_at.desc())
            .all()
        )

    def upsert_device(
        self,
        user_id: str,
        device_identifier: str,
        device_name: str,
        platform: str,
        os_version: str,
        app_version: str,
        push_token_masked: str,
    ) -> DeviceRegistration:
        now = datetime.now(timezone.utc)
        rec = (
            self.db.query(DeviceRegistration)
            .filter(
                DeviceRegistration.user_id == user_id,
                DeviceRegistration.device_identifier == device_identifier,
            )
            .first()
        )
        if rec is None:
            rec = DeviceRegistration(
                user_id=user_id,
                device_identifier=device_identifier,
                device_name=device_name,
                platform=platform,
                os_version=os_version,
                app_version=app_version,
                push_token_masked=push_token_masked,
                is_active=True,
                last_seen_at=now,
            )
            self.db.add(rec)
        else:
            rec.device_name = device_name
            rec.platform = platform
            rec.os_version = os_version
            rec.app_version = app_version
            if push_token_masked:
                rec.push_token_masked = push_token_masked
            rec.is_active = True
            rec.last_seen_at = now
        self.db.commit()
        self.db.refresh(rec)
        return rec

    def get_device_owned(self, user_id: str, device_id: str) -> Optional[DeviceRegistration]:
        return (
            self.db.query(DeviceRegistration)
            .filter(DeviceRegistration.id == device_id, DeviceRegistration.user_id == user_id)
            .first()
        )

    def delete_device(self, rec: DeviceRegistration) -> None:
        self.db.delete(rec)
        self.db.commit()

    def list_notifications(self, user_id: str) -> List[NotificationRecord]:
        return (
            self.db.query(NotificationRecord)
            .filter(NotificationRecord.user_id == user_id)
            .order_by(NotificationRecord.created_at.desc())
            .limit(100)
            .all()
        )

    def create_notification(
        self,
        user_id: str,
        title: str,
        body: str,
        category: str,
        channel: str,
        scheduled_for: Optional[datetime],
    ) -> NotificationRecord:
        rec = NotificationRecord(
            user_id=user_id,
            title=title,
            body=body,
            category=category,
            channel=channel,
            status="PENDING",
            scheduled_for=scheduled_for,
        )
        self.db.add(rec)
        self.db.commit()
        self.db.refresh(rec)
        return rec

    def get_notification_owned(self, user_id: str, notification_id: str) -> Optional[NotificationRecord]:
        return (
            self.db.query(NotificationRecord)
            .filter(
                NotificationRecord.id == notification_id,
                NotificationRecord.user_id == user_id,
            )
            .first()
        )

    def mark_notification_read(self, rec: NotificationRecord) -> NotificationRecord:
        rec.status = "READ"
        if rec.delivered_at is None:
            rec.delivered_at = datetime.now(timezone.utc)
        self.db.commit()
        self.db.refresh(rec)
        return rec

    def delete_notification(self, rec: NotificationRecord) -> None:
        self.db.delete(rec)
        self.db.commit()
