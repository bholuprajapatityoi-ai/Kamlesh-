from datetime import datetime, timedelta, timezone
from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.security import (
    create_access_token,
    generate_opaque_token,
    hash_password,
    hash_token,
    verify_password,
)
from app.models.user import User
from app.repositories.security_repository import SecurityRepository
from app.repositories.session_repository import SessionRepository
from app.repositories.user_repository import UserRepository
from app.schemas.auth import AuthResponse, UserResponse
from app.schemas.common import MessageResponse
from app.services.email_service import EmailService
from app.utils.validators import validate_password_strength


class AuthService:
    def __init__(self, db: Session) -> None:
        self.user_repo = UserRepository(db)
        self.session_repo = SessionRepository(db)
        self.security_repo = SecurityRepository(db)
        self.email_service = EmailService()
        self.settings = get_settings()

    def to_user_response(self, user: User) -> UserResponse:
        profile = user.profile
        return UserResponse(
            id=user.id,
            full_name=profile.full_name if profile else "Boss",
            email=user.email,
            preferred_title=profile.preferred_title if profile else "Boss",
            preferred_language=profile.preferred_language if profile else "Hinglish",
            is_verified=user.is_verified,
            created_at=user.created_at.isoformat(),
        )

    def _issue_session(self, user: User) -> AuthResponse:
        raw_refresh = generate_opaque_token()
        expires_at = datetime.now(timezone.utc) + timedelta(
            days=self.settings.REFRESH_TOKEN_EXPIRE_DAYS
        )
        session = self.session_repo.create(
            user_id=user.id,
            refresh_token_hash=hash_token(raw_refresh),
            expires_at=expires_at,
        )
        access_token = create_access_token(user_id=user.id, session_id=session.id)
        return AuthResponse(
            access_token=access_token,
            refresh_token=raw_refresh,
            expires_in=self.settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
            user=self.to_user_response(user),
        )

    def register(
        self,
        full_name: str,
        email: str,
        password: str,
        confirm_password: str,
        ip_address: str = "",
    ) -> AuthResponse:
        if password != confirm_password:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Password and confirm_password do not match.",
            )
        validate_password_strength(password)
        if self.user_repo.get_by_email(email):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="An account with this email already exists.",
            )
        user = self.user_repo.create_user_with_defaults(
            email=email,
            password_hash=hash_password(password),
            full_name=full_name,
        )
        self.security_repo.log_security_event(
            user_id=user.id,
            event_type="ACCOUNT_REGISTERED",
            severity="INFO",
            detail="New Boss account created",
            ip_address=ip_address,
        )
        return self._issue_session(user)

    def login(self, email: str, password: str, ip_address: str = "") -> AuthResponse:
        user = self.user_repo.get_by_email(email)
        if not user or not verify_password(password, user.password_hash):
            self.security_repo.log_login_attempt(
                email=email,
                user_id=user.id if user else None,
                success=False,
                ip_address=ip_address,
            )
            if user:
                self.security_repo.log_security_event(
                    user_id=user.id,
                    event_type="LOGIN_FAILED",
                    severity="WARN",
                    detail="Invalid login credentials presented",
                    ip_address=ip_address,
                )
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid email or password.",
            )
        if not user.is_active:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="User account is disabled.",
            )
        self.security_repo.log_login_attempt(
            email=email, user_id=user.id, success=True, ip_address=ip_address
        )
        self.security_repo.log_security_event(
            user_id=user.id,
            event_type="LOGIN_SUCCESS",
            severity="INFO",
            detail="Authenticated via password",
            ip_address=ip_address,
        )
        return self._issue_session(user)

    def refresh(self, raw_refresh_token: str) -> AuthResponse:
        session = self.session_repo.get_by_refresh_hash(hash_token(raw_refresh_token))
        now = datetime.now(timezone.utc)
        if not session or session.revoked:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Refresh token is invalid or revoked.",
            )
        exp = session.expires_at
        if exp.tzinfo is None:
            exp = exp.replace(tzinfo=timezone.utc)
        if exp < now:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Refresh token has expired.",
            )

        user = self.user_repo.get_by_id(session.user_id)
        if not user or not user.is_active:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User account not active.",
            )

        new_raw_refresh = generate_opaque_token()
        new_exp = now + timedelta(days=self.settings.REFRESH_TOKEN_EXPIRE_DAYS)
        rotated = self.session_repo.rotate_refresh_token(
            session=session,
            new_refresh_hash=hash_token(new_raw_refresh),
            new_expires_at=new_exp,
        )
        access_token = create_access_token(user_id=user.id, session_id=rotated.id)
        return AuthResponse(
            access_token=access_token,
            refresh_token=new_raw_refresh,
            expires_in=self.settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
            user=self.to_user_response(user),
        )

    def logout(self, session_id: str, user_id: str | None = None) -> MessageResponse:
        session = self.session_repo.get_by_id(session_id)
        if session and not session.revoked:
            self.session_repo.revoke(session)
            self.security_repo.revoke_session_for_user(
                user_id=user_id or session.user_id,
                session_id=session.id,
                reason="LOGOUT",
            )
        return MessageResponse(message="Logged out successfully. Session token revoked.")

    def forgot_password(self, email: str) -> MessageResponse:
        if not self.settings.email_delivery_configured:
            return MessageResponse(
                message="Password reset cannot send emails because SMTP email delivery is not configured on this server.",
                email_sent=False,
                delivery_configured=False,
            )

        user = self.user_repo.get_by_email(email)
        if not user:
            return MessageResponse(
                message="If an account matches this email, password reset instructions have been processed.",
                email_sent=True,
                delivery_configured=True,
            )

        raw_token = generate_opaque_token()
        expires_at = datetime.now(timezone.utc) + timedelta(
            minutes=self.settings.RESET_TOKEN_EXPIRE_MINUTES
        )
        self.user_repo.create_reset_token(
            user_id=user.id,
            token_hash=hash_token(raw_token),
            expires_at=expires_at,
        )
        email_sent, _ = self.email_service.send_password_reset_email(
            recipient=user.email,
            reset_token=raw_token,
        )
        if not email_sent:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Email delivery provider failed to send password reset message.",
            )
        self.security_repo.log_security_event(
            user_id=user.id,
            event_type="PASSWORD_RESET_REQUESTED",
            severity="INFO",
            detail="Reset token issued via SMTP",
        )
        return MessageResponse(
            message="If an account matches this email, password reset instructions have been sent.",
            email_sent=True,
            delivery_configured=True,
        )

    def reset_password(
        self, token: str, new_password: str, confirm_password: str
    ) -> MessageResponse:
        if new_password != confirm_password:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Passwords do not match.",
            )
        validate_password_strength(new_password)
        rec = self.user_repo.get_reset_token(hash_token(token))
        if not rec:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or expired password reset token.",
            )
        exp = rec.expires_at
        if exp.tzinfo is None:
            exp = exp.replace(tzinfo=timezone.utc)
        if exp < datetime.now(timezone.utc):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Password reset token has expired.",
            )
        user_id = rec.user_id
        self.user_repo.complete_password_reset(rec, hash_password(new_password))
        self.security_repo.log_security_event(
            user_id=user_id,
            event_type="PASSWORD_RESET_COMPLETED",
            severity="WARN",
            detail="Password changed and prior sessions revoked",
        )
        return MessageResponse(message="Password has been reset successfully. Please sign in again.")

    def request_email_verification(self, user: User) -> MessageResponse:
        if user.is_verified:
            return MessageResponse(
                message="Email address is already verified.",
                email_sent=False,
                delivery_configured=self.settings.email_delivery_configured,
            )
        if not self.settings.email_delivery_configured:
            return MessageResponse(
                message="Verification email cannot be sent because SMTP email delivery is not configured on this server.",
                email_sent=False,
                delivery_configured=False,
            )

        raw_token = generate_opaque_token()
        expires_at = datetime.now(timezone.utc) + timedelta(
            minutes=self.settings.EMAIL_VERIFICATION_EXPIRE_MINUTES
        )
        self.user_repo.create_email_verification_token(
            user_id=user.id,
            token_hash=hash_token(raw_token),
            expires_at=expires_at,
        )
        email_sent, _ = self.email_service.send_verification_email(
            recipient=user.email,
            verification_token=raw_token,
        )
        if not email_sent:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Email delivery provider failed to send verification email.",
            )
        self.security_repo.log_security_event(
            user_id=user.id,
            event_type="EMAIL_VERIFICATION_SENT",
            severity="INFO",
            detail="Verification token dispatched via SMTP",
        )
        return MessageResponse(
            message="Verification instructions sent to your registered email.",
            email_sent=True,
            delivery_configured=True,
        )

    def confirm_email_verification(self, token: str) -> MessageResponse:
        rec = self.user_repo.get_email_verification_token(hash_token(token))
        if not rec:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or expired email verification token.",
            )
        exp = rec.expires_at
        if exp.tzinfo is None:
            exp = exp.replace(tzinfo=timezone.utc)
        if exp < datetime.now(timezone.utc):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Email verification token has expired.",
            )
        self.user_repo.mark_email_verified(rec)
        self.security_repo.log_security_event(
            user_id=rec.user_id,
            event_type="EMAIL_VERIFIED",
            severity="INFO",
            detail="Account email verified successfully",
        )
        return MessageResponse(message="Email verified successfully.")
