from datetime import datetime, timezone
from typing import Optional
from sqlalchemy.orm import Session

from app.models.assistant_settings import AssistantSettings
from app.models.user import EmailVerificationToken, PasswordResetToken, User
from app.models.user_profile import UserProfile


class UserRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_by_email(self, email: str) -> Optional[User]:
        return self.db.query(User).filter(User.email == email.lower().strip()).first()

    def get_by_id(self, user_id: str) -> Optional[User]:
        return self.db.query(User).filter(User.id == user_id).first()

    def create_user_with_defaults(
        self, email: str, password_hash: str, full_name: str
    ) -> User:
        user = User(email=email.lower().strip(), password_hash=password_hash)
        self.db.add(user)
        self.db.flush()

        profile = UserProfile(
            user_id=user.id,
            full_name=full_name.strip(),
            preferred_title="Boss",
            preferred_language="Hinglish",
        )
        assistant_settings = AssistantSettings(user_id=user.id)
        self.db.add(profile)
        self.db.add(assistant_settings)
        self.db.commit()
        self.db.refresh(user)
        return user

    def update_profile(
        self,
        user: User,
        full_name: Optional[str] = None,
        preferred_title: Optional[str] = None,
        preferred_language: Optional[str] = None,
    ) -> User:
        if user.profile is None:
            user.profile = UserProfile(user_id=user.id, full_name="Boss")
        if full_name is not None:
            user.profile.full_name = full_name.strip()
        if preferred_title is not None:
            user.profile.preferred_title = preferred_title.strip()
        if preferred_language is not None:
            user.profile.preferred_language = preferred_language
        self.db.commit()
        self.db.refresh(user)
        return user

    def get_or_create_assistant_settings(self, user_id: str) -> AssistantSettings:
        rec = (
            self.db.query(AssistantSettings)
            .filter(AssistantSettings.user_id == user_id)
            .first()
        )
        if rec is None:
            rec = AssistantSettings(user_id=user_id)
            self.db.add(rec)
            self.db.commit()
            self.db.refresh(rec)
        return rec

    def save_assistant_settings(self, rec: AssistantSettings) -> AssistantSettings:
        self.db.commit()
        self.db.refresh(rec)
        return rec

    def create_reset_token(
        self, user_id: str, token_hash: str, expires_at
    ) -> PasswordResetToken:
        rec = PasswordResetToken(
            user_id=user_id,
            token_hash=token_hash,
            expires_at=expires_at,
        )
        self.db.add(rec)
        self.db.commit()
        self.db.refresh(rec)
        return rec

    def get_reset_token(self, token_hash: str) -> Optional[PasswordResetToken]:
        return (
            self.db.query(PasswordResetToken)
            .filter(
                PasswordResetToken.token_hash == token_hash,
                PasswordResetToken.used.is_(False),
            )
            .first()
        )

    def complete_password_reset(self, reset_rec: PasswordResetToken, new_password_hash: str) -> None:
        user = self.get_by_id(reset_rec.user_id)
        if user:
            user.password_hash = new_password_hash
            for s in user.sessions:
                s.revoked = True
        reset_rec.used = True
        self.db.commit()

    def create_email_verification_token(
        self, user_id: str, token_hash: str, expires_at
    ) -> EmailVerificationToken:
        rec = EmailVerificationToken(
            user_id=user_id,
            token_hash=token_hash,
            expires_at=expires_at,
        )
        self.db.add(rec)
        self.db.commit()
        self.db.refresh(rec)
        return rec

    def get_email_verification_token(self, token_hash: str) -> Optional[EmailVerificationToken]:
        return (
            self.db.query(EmailVerificationToken)
            .filter(
                EmailVerificationToken.token_hash == token_hash,
                EmailVerificationToken.used.is_(False),
            )
            .first()
        )

    def mark_email_verified(self, token_rec: EmailVerificationToken) -> None:
        user = self.get_by_id(token_rec.user_id)
        if user:
            user.is_verified = True
            user.email_verified_at = datetime.now(timezone.utc)
        token_rec.used = True
        self.db.commit()
