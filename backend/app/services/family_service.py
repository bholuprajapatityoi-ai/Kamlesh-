from datetime import datetime, timedelta, timezone
from typing import List
from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.core.security import generate_opaque_token, hash_token
from app.models.family import FamilyInvitation, SharedReminder
from app.repositories.family_repository import FamilyRepository
from app.schemas.family import (
    FamilyDashboardResponse,
    FamilyGroupCreate,
    FamilyGroupResponse,
    FamilyInvitationCreate,
    FamilyInvitationResponse,
    FamilyMemberResponse,
    FamilyMemberUpdate,
    SharedReminderCreate,
    SharedReminderResponse,
    SharedReminderUpdate,
)


class FamilyService:
    def __init__(self, db: Session) -> None:
        self.repo = FamilyRepository(db)

    def _require_membership(self, group_id: str, user_id: str):
        member = self.repo.get_membership(group_id, user_id)
        if not member:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="You are not a member of this family group.",
            )
        return member

    def _require_admin(self, group_id: str, user_id: str):
        member = self._require_membership(group_id, user_id)
        if member.role.upper() != "ADMIN":
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Family administrator permission required.",
            )
        return member

    def create_group(self, user_id: str, req: FamilyGroupCreate) -> FamilyGroupResponse:
        group = self.repo.create_group_with_owner(req.name, user_id)
        return FamilyGroupResponse(
            id=group.id,
            name=group.name,
            owner_user_id=group.owner_user_id,
            member_count=1,
            created_at=group.created_at.isoformat(),
        )

    def list_groups(self, user_id: str) -> List[FamilyGroupResponse]:
        groups = self.repo.list_user_groups(user_id)
        return [
            FamilyGroupResponse(
                id=g.id,
                name=g.name,
                owner_user_id=g.owner_user_id,
                member_count=self.repo.count_members(g.id),
                created_at=g.created_at.isoformat(),
            )
            for g in groups
        ]

    def create_invitation(
        self, user_id: str, req: FamilyInvitationCreate
    ) -> FamilyInvitationResponse:
        self._require_admin(req.group_id, user_id)
        raw_code = f"GOLU-{generate_opaque_token()[:12].upper()}"
        inv = FamilyInvitation(
            group_id=req.group_id,
            invited_by_user_id=user_id,
            invitee_email=str(req.invitee_email).lower(),
            invite_code_hash=hash_token(raw_code),
            role=req.role.upper().strip(),
            display_label=req.display_label.strip(),
            father_friendly_mode=req.father_friendly_mode,
            status="PENDING",
            expires_at=datetime.now(timezone.utc) + timedelta(days=7),
        )
        inv = self.repo.create_invitation(inv)
        return FamilyInvitationResponse(
            id=inv.id,
            group_id=inv.group_id,
            invitee_email=inv.invitee_email,
            role=inv.role,
            display_label=inv.display_label,
            father_friendly_mode=inv.father_friendly_mode,
            status=inv.status,
            invite_code=raw_code,
            expires_at=inv.expires_at.isoformat(),
            created_at=inv.created_at.isoformat(),
        )

    def accept_invitation(self, user_id: str, user_email: str, invite_code: str) -> FamilyMemberResponse:
        inv = self.repo.get_invitation_by_code_hash(hash_token(invite_code.strip()))
        if not inv or inv.status != "PENDING":
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or already used family invitation code.",
            )
        exp = inv.expires_at
        if exp.tzinfo is None:
            exp = exp.replace(tzinfo=timezone.utc)
        if exp < datetime.now(timezone.utc):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Family invitation code has expired.",
            )
        if inv.invitee_email != user_email.lower().strip():
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="This family invitation belongs to a different email account.",
            )
        member = self.repo.add_member_from_invite(inv, user_id)
        return self._member_dto(member)

    def _member_dto(self, m) -> FamilyMemberResponse:
        u = self.repo.get_user(m.user_id)
        perm = self.repo.get_permission(m.group_id, m.user_id)
        full_name = u.profile.full_name if u and u.profile else m.display_label
        email = u.email if u else ""
        return FamilyMemberResponse(
            id=m.id,
            group_id=m.group_id,
            user_id=m.user_id,
            full_name=full_name,
            email=email,
            role=m.role,
            display_label=m.display_label,
            father_friendly_mode=m.father_friendly_mode,
            share_shared_reminders=perm.share_shared_reminders if perm else True,
            share_family_routines=perm.share_family_routines if perm else False,
            joined_at=m.joined_at.isoformat(),
        )

    def list_members(self, user_id: str) -> List[FamilyMemberResponse]:
        groups = self.repo.list_user_groups(user_id)
        members = self.repo.list_members_for_groups([g.id for g in groups])
        return [self._member_dto(m) for m in members]

    def update_member(
        self, actor_user_id: str, member_id: str, req: FamilyMemberUpdate
    ) -> FamilyMemberResponse:
        target = self.repo.get_member_by_id(member_id)
        if not target:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Family member not found.")
        actor = self._require_membership(target.group_id, actor_user_id)
        is_self = target.user_id == actor_user_id
        is_admin = actor.role.upper() == "ADMIN"
        if not is_self and not is_admin:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized.")

        # Admin can update role/label; privacy permission toggles belong to the member themselves (or initial setup)
        if req.role is not None and is_admin:
            target.role = req.role.upper().strip()
        if req.display_label is not None:
            target.display_label = req.display_label.strip()
        if req.father_friendly_mode is not None:
            target.father_friendly_mode = req.father_friendly_mode
        self.repo.save(target)

        perm = self.repo.get_permission(target.group_id, target.user_id)
        if perm and is_self:
            if req.share_shared_reminders is not None:
                perm.share_shared_reminders = req.share_shared_reminders
            if req.share_family_routines is not None:
                perm.share_family_routines = req.share_family_routines
            self.repo.save(perm)
        return self._member_dto(target)

    def remove_member(self, actor_user_id: str, member_id: str) -> None:
        target = self.repo.get_member_by_id(member_id)
        if not target:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Family member not found.")
        actor = self._require_membership(target.group_id, actor_user_id)
        if target.user_id != actor_user_id and actor.role.upper() != "ADMIN":
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized.")
        self.repo.delete(target)

    # Shared Reminders
    def _shared_dto(self, r: SharedReminder) -> SharedReminderResponse:
        return SharedReminderResponse(
            id=r.id,
            group_id=r.group_id,
            created_by_user_id=r.created_by_user_id,
            title=r.title,
            notes=r.notes,
            remind_at=r.remind_at.isoformat(),
            is_completed=r.is_completed,
            created_at=r.created_at.isoformat(),
            updated_at=r.updated_at.isoformat(),
        )

    def list_shared_reminders(self, user_id: str) -> List[SharedReminderResponse]:
        group_ids = [g.id for g in self.repo.list_user_groups(user_id)]
        return [self._shared_dto(r) for r in self.repo.list_shared_reminders(group_ids)]

    def create_shared_reminder(
        self, user_id: str, req: SharedReminderCreate
    ) -> SharedReminderResponse:
        self._require_membership(req.group_id, user_id)
        r = SharedReminder(
            group_id=req.group_id,
            created_by_user_id=user_id,
            title=req.title.strip(),
            notes=req.notes.strip(),
            remind_at=req.remind_at,
        )
        return self._shared_dto(self.repo.create_shared_reminder(r))

    def update_shared_reminder(
        self, user_id: str, reminder_id: str, req: SharedReminderUpdate
    ) -> SharedReminderResponse:
        r = self.repo.get_shared_reminder(reminder_id)
        if not r:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Shared reminder not found.")
        self._require_membership(r.group_id, user_id)
        if req.title is not None:
            r.title = req.title.strip()
        if req.notes is not None:
            r.notes = req.notes.strip()
        if req.remind_at is not None:
            r.remind_at = req.remind_at
        if req.is_completed is not None:
            r.is_completed = req.is_completed
        return self._shared_dto(self.repo.save(r))

    def delete_shared_reminder(self, user_id: str, reminder_id: str) -> None:
        r = self.repo.get_shared_reminder(reminder_id)
        if not r:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Shared reminder not found.")
        member = self._require_membership(r.group_id, user_id)
        if r.created_by_user_id != user_id and member.role.upper() != "ADMIN":
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not authorized.")
        self.repo.delete(r)

    def get_dashboard(self, user_id: str, user_email: str) -> FamilyDashboardResponse:
        groups = self.list_groups(user_id)
        gids = [g.id for g in groups]
        members = self.list_members(user_id)
        invites = [
            FamilyInvitationResponse(
                id=i.id,
                group_id=i.group_id,
                invitee_email=i.invitee_email,
                role=i.role,
                display_label=i.display_label,
                father_friendly_mode=i.father_friendly_mode,
                status=i.status,
                invite_code=None,
                expires_at=i.expires_at.isoformat(),
                created_at=i.created_at.isoformat(),
            )
            for i in self.repo.list_pending_invitations_for_groups_or_email(gids, user_email)
        ]
        shared = self.list_shared_reminders(user_id)
        return FamilyDashboardResponse(
            groups=groups,
            members=members,
            pending_invitations=invites,
            shared_reminders=shared,
        )
