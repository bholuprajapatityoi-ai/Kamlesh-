from typing import List
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.common import MessageResponse
from app.schemas.device_notification import DeviceRegisterRequest, DeviceResponse
from app.services.device_notification_service import DeviceNotificationService

router = APIRouter(prefix="/devices", tags=["Devices"])


@router.get("", response_model=List[DeviceResponse])
def list_devices(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DeviceNotificationService(db).list_devices(user.id)


@router.post("", response_model=DeviceResponse, status_code=status.HTTP_201_CREATED)
def register_device(
    payload: DeviceRegisterRequest,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return DeviceNotificationService(db).register_device(user.id, payload)


@router.delete("/{device_id}", response_model=MessageResponse)
def remove_device(
    device_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    DeviceNotificationService(db).remove_device(user.id, device_id)
    return MessageResponse(message="Device registration removed.")
