from typing import List
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database.session import get_db
from app.middleware.authentication import get_current_user_and_session
from app.schemas.common import MessageResponse
from app.schemas.family import (
    FamilyDashboardResponse,
    FamilyGroupCreate,
    FamilyGroupResponse,
    FamilyInvitationAccept,
    FamilyInvitationCreate,
    FamilyInvitationResponse,
    FamilyMemberResponse,
    FamilyMemberUpdate,
    SharedReminderCreate,
    SharedReminderResponse,
    SharedReminderUpdate,
)
from app.services.family_service import FamilyService

router = APIRouter(prefix="/family", tags=["Family Mode (Phase 11)"])


@router.get("/dashboard", response_model=FamilyDashboardResponse)
def get_family_dashboard(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return FamilyService(db).get_dashboard(user.id, user.email)


@router.post("/groups", response_model=FamilyGroupResponse, status_code=status.HTTP_201_CREATED)
def create_family_group(
    payload: FamilyGroupCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return FamilyService(db).create_group(user.id, payload)


@router.get("/groups", response_model=List[FamilyGroupResponse])
def list_family_groups(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return FamilyService(db).list_groups(user.id)


@router.post(
    "/invitations", response_model=FamilyInvitationResponse, status_code=status.HTTP_201_CREATED
)
def invite_family_member(
    payload: FamilyInvitationCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return FamilyService(db).create_invitation(user.id, payload)


@router.post("/invitations/accept", response_model=FamilyMemberResponse)
def accept_family_invitation(
    payload: FamilyInvitationAccept,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return FamilyService(db).accept_invitation(user.id, user.email, payload.invite_code)


@router.get("/members", response_model=List[FamilyMemberResponse])
def list_family_members(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return FamilyService(db).list_members(user.id)


@router.put("/members/{member_id}", response_model=FamilyMemberResponse)
def update_family_member(
    member_id: str,
    payload: FamilyMemberUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return FamilyService(db).update_member(user.id, member_id, payload)


@router.delete("/members/{member_id}", response_model=MessageResponse)
def delete_family_member(
    member_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    FamilyService(db).remove_member(user.id, member_id)
    return MessageResponse(message="Family member removed.")


@router.post(
    "/shared-reminders", response_model=SharedReminderResponse, status_code=status.HTTP_201_CREATED
)
def create_shared_reminder(
    payload: SharedReminderCreate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return FamilyService(db).create_shared_reminder(user.id, payload)


@router.get("/shared-reminders", response_model=List[SharedReminderResponse])
def list_shared_reminders(
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return FamilyService(db).list_shared_reminders(user.id)


@router.put("/shared-reminders/{reminder_id}", response_model=SharedReminderResponse)
def update_shared_reminder(
    reminder_id: str,
    payload: SharedReminderUpdate,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    return FamilyService(db).update_shared_reminder(user.id, reminder_id, payload)


@router.delete("/shared-reminders/{reminder_id}", response_model=MessageResponse)
def delete_shared_reminder(
    reminder_id: str,
    auth=Depends(get_current_user_and_session),
    db: Session = Depends(get_db),
):
    user, _ = auth
    FamilyService(db).delete_shared_reminder(user.id, reminder_id)
    return MessageResponse(message="Shared family reminder deleted.")
