from typing import List
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.common import MessageResponse
from app.schemas.device_notification import NotificationCreateRequest, NotificationResponse
from app.services.device_notification_service import DeviceNotificationService

router = APIRouter(prefix="/notifications", tags=["Notifications"])


@router.get("", response_model=List[NotificationResponse])
def list_notifications(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DeviceNotificationService(db).list_notifications(user.id)


@router.post("", response_model=NotificationResponse, status_code=status.HTTP_201_CREATED)
def create_notification(
    payload: NotificationCreateRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DeviceNotificationService(db).create_notification(user.id, payload)


@router.post("/{notification_id}/read", response_model=NotificationResponse)
def mark_notification_read(
    notification_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DeviceNotificationService(db).mark_read(user.id, notification_id)


@router.delete("/{notification_id}", response_model=MessageResponse)
def delete_notification(
    notification_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    DeviceNotificationService(db).delete_notification(user.id, notification_id)
    return MessageResponse(message="Notification removed.")
