from typing import List
from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.core.redis_client import redis_client
from app.core.security import redact_sensitive
from app.models.device_notification import DeviceRegistration, NotificationRecord
from app.repositories.device_notification_repository import DeviceNotificationRepository
from app.schemas.device_notification import (
    DeviceRegisterRequest,
    DeviceResponse,
    NotificationCreateRequest,
    NotificationResponse,
)
from app.security.sanitizer import sanitize_text


class DeviceNotificationService:
    def __init__(self, db: Session) -> None:
        self.repo = DeviceNotificationRepository(db)

    def _to_device_dto(self, rec: DeviceRegistration) -> DeviceResponse:
        return DeviceResponse(
            id=rec.id,
            device_identifier=rec.device_identifier,
            device_name=rec.device_name,
            platform=rec.platform,
            os_version=rec.os_version,
            app_version=rec.app_version,
            push_token_masked=rec.push_token_masked,
            is_active=rec.is_active,
            last_seen_at=rec.last_seen_at.isoformat(),
            created_at=rec.created_at.isoformat(),
        )

    def _to_notification_dto(self, rec: NotificationRecord) -> NotificationResponse:
        return NotificationResponse(
            id=rec.id,
            title=rec.title,
            body=rec.body,
            category=rec.category,
            channel=rec.channel,
            status=rec.status,
            retry_count=rec.retry_count,
            scheduled_for=rec.scheduled_for.isoformat() if rec.scheduled_for else None,
            delivered_at=rec.delivered_at.isoformat() if rec.delivered_at else None,
            created_at=rec.created_at.isoformat(),
        )

    def list_devices(self, user_id: str) -> List[DeviceResponse]:
        return [self._to_device_dto(d) for d in self.repo.list_devices(user_id)]

    def register_device(self, user_id: str, payload: DeviceRegisterRequest) -> DeviceResponse:
        masked_token = redact_sensitive(payload.push_token.strip()) if payload.push_token.strip() else ""
        rec = self.repo.upsert_device(
            user_id=user_id,
            device_identifier=sanitize_text(payload.device_identifier, 120),
            device_name=sanitize_text(payload.device_name, 120) or "Android Device",
            platform=sanitize_text(payload.platform, 30).lower() or "android",
            os_version=sanitize_text(payload.os_version, 40),
            app_version=sanitize_text(payload.app_version, 40) or "1.1.0",
            push_token_masked=masked_token,
        )
        return self._to_device_dto(rec)

    def remove_device(self, user_id: str, device_id: str) -> None:
        rec = self.repo.get_device_owned(user_id, device_id)
        if rec is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Device not found.")
        self.repo.delete_device(rec)

    def list_notifications(self, user_id: str) -> List[NotificationResponse]:
        return [self._to_notification_dto(n) for n in self.repo.list_notifications(user_id)]

    def create_notification(
        self, user_id: str, payload: NotificationCreateRequest
    ) -> NotificationResponse:
        rec = self.repo.create_notification(
            user_id=user_id,
            title=sanitize_text(payload.title, 200),
            body=sanitize_text(payload.body, 2000),
            category=sanitize_text(payload.category, 40).upper() or "REMINDER",
            channel=sanitize_text(payload.channel, 40).upper() or "IN_APP",
            scheduled_for=payload.scheduled_for,
        )
        redis_client.enqueue_job(
            "golu:notifications",
            {"notification_id": rec.id, "user_id": user_id},
        )
        return self._to_notification_dto(rec)

    def mark_read(self, user_id: str, notification_id: str) -> NotificationResponse:
        rec = self.repo.get_notification_owned(user_id, notification_id)
        if rec is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Notification not found."
            )
        return self._to_notification_dto(self.repo.mark_notification_read(rec))

    def delete_notification(self, user_id: str, notification_id: str) -> None:
        rec = self.repo.get_notification_owned(user_id, notification_id)
        if rec is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Notification not found."
            )
        self.repo.delete_notification(rec)
