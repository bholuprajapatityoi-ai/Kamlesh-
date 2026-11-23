from typing import List, Optional
from sqlalchemy.orm import Session

from app.models.family import (
    FamilyGroup,
    FamilyInvitation,
    FamilyMember,
    FamilyPermission,
    SharedReminder,
)
from app.models.user import User


class FamilyRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def list_user_groups(self, user_id: str) -> List[FamilyGroup]:
        member_group_ids = [
            m.group_id
            for m in self.db.query(FamilyMember).filter(FamilyMember.user_id == user_id).all()
        ]
        if not member_group_ids:
            return []
        return (
            self.db.query(FamilyGroup)
            .filter(FamilyGroup.id.in_(member_group_ids))
            .order_by(FamilyGroup.created_at.asc())
            .all()
        )

    def get_group(self, group_id: str) -> Optional[FamilyGroup]:
        return self.db.query(FamilyGroup).filter(FamilyGroup.id == group_id).first()

    def get_membership(self, group_id: str, user_id: str) -> Optional[FamilyMember]:
        return (
            self.db.query(FamilyMember)
            .filter(FamilyMember.group_id == group_id, FamilyMember.user_id == user_id)
            .first()
        )

    def create_group_with_owner(self, name: str, owner_user_id: str) -> FamilyGroup:
        group = FamilyGroup(name=name.strip(), owner_user_id=owner_user_id)
        self.db.add(group)
        self.db.flush()
        member = FamilyMember(
            group_id=group.id,
            user_id=owner_user_id,
            role="ADMIN",
            display_label="Boss",
            father_friendly_mode=False,
        )
        perm = FamilyPermission(
            group_id=group.id,
            user_id=owner_user_id,
            share_shared_reminders=True,
            share_family_routines=True,
        )
        self.db.add(member)
        self.db.add(perm)
        self.db.commit()
        self.db.refresh(group)
        return group

    def count_members(self, group_id: str) -> int:
        return self.db.query(FamilyMember).filter(FamilyMember.group_id == group_id).count()

    def list_members_for_groups(self, group_ids: List[str]) -> List[FamilyMember]:
        if not group_ids:
            return []
        return (
            self.db.query(FamilyMember)
            .filter(FamilyMember.group_id.in_(group_ids))
            .order_by(FamilyMember.joined_at.asc())
            .all()
        )

    def get_member_by_id(self, member_id: str) -> Optional[FamilyMember]:
        return self.db.query(FamilyMember).filter(FamilyMember.id == member_id).first()

    def get_permission(self, group_id: str, user_id: str) -> Optional[FamilyPermission]:
        return (
            self.db.query(FamilyPermission)
            .filter(FamilyPermission.group_id == group_id, FamilyPermission.user_id == user_id)
            .first()
        )

    def get_user(self, user_id: str) -> Optional[User]:
        return self.db.query(User).filter(User.id == user_id).first()

    def create_invitation(self, invitation: FamilyInvitation) -> FamilyInvitation:
        self.db.add(invitation)
        self.db.commit()
        self.db.refresh(invitation)
        return invitation

    def get_invitation_by_code_hash(self, code_hash: str) -> Optional[FamilyInvitation]:
        return (
            self.db.query(FamilyInvitation)
            .filter(FamilyInvitation.invite_code_hash == code_hash)
            .first()
        )

    def list_pending_invitations_for_groups_or_email(
        self, group_ids: List[str], email: str
    ) -> List[FamilyInvitation]:
        q = self.db.query(FamilyInvitation).filter(FamilyInvitation.status == "PENDING")
        if group_ids:
            return (
                q.filter(
                    (FamilyInvitation.group_id.in_(group_ids))
                    | (FamilyInvitation.invitee_email == email.lower())
                )
                .order_by(FamilyInvitation.created_at.desc())
                .all()
            )
        return (
            q.filter(FamilyInvitation.invitee_email == email.lower())
            .order_by(FamilyInvitation.created_at.desc())
            .all()
        )

    def add_member_from_invite(
        self,
        invitation: FamilyInvitation,
        user_id: str,
    ) -> FamilyMember:
        existing = self.get_membership(invitation.group_id, user_id)
        if existing:
            invitation.status = "ACCEPTED"
            self.db.commit()
            return existing
        member = FamilyMember(
            group_id=invitation.group_id,
            user_id=user_id,
            role=invitation.role,
            display_label=invitation.display_label,
            father_friendly_mode=invitation.father_friendly_mode,
        )
        perm = FamilyPermission(
            group_id=invitation.group_id,
            user_id=user_id,
            share_shared_reminders=True,
            share_family_routines=False,
        )
        invitation.status = "ACCEPTED"
        self.db.add(member)
        self.db.add(perm)
        self.db.commit()
        self.db.refresh(member)
        return member

    def list_shared_reminders(self, group_ids: List[str]) -> List[SharedReminder]:
        if not group_ids:
            return []
        return (
            self.db.query(SharedReminder)
            .filter(SharedReminder.group_id.in_(group_ids))
            .order_by(SharedReminder.remind_at.asc())
            .all()
        )

    def get_shared_reminder(self, reminder_id: str) -> Optional[SharedReminder]:
        return self.db.query(SharedReminder).filter(SharedReminder.id == reminder_id).first()

    def create_shared_reminder(self, reminder: SharedReminder) -> SharedReminder:
        self.db.add(reminder)
        self.db.commit()
        self.db.refresh(reminder)
        return reminder

    def save(self, obj):
        self.db.commit()
        self.db.refresh(obj)
        return obj

    def delete(self, obj) -> None:
        self.db.delete(obj)
        self.db.commit()
