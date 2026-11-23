import smtplib
from email.message import EmailMessage
from typing import Tuple

from app.core.config import get_settings


class EmailService:
    def _send_smtp(self, recipient: str, subject: str, body: str) -> Tuple[bool, bool]:
        settings = get_settings()
        if not settings.email_delivery_configured:
            return False, False

        msg = EmailMessage()
        msg["Subject"] = subject
        msg["From"] = settings.SMTP_FROM_EMAIL
        msg["To"] = recipient
        msg.set_content(body)

        try:
            with smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT, timeout=10) as server:
                server.starttls()
                if settings.SMTP_USER and settings.SMTP_PASSWORD:
                    server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
                server.send_message(msg)
            return True, True
        except Exception:
            return False, True

    def send_password_reset_email(self, recipient: str, reset_token: str) -> Tuple[bool, bool]:
        """
        Returns (email_sent, delivery_configured).
        Never claims an email was sent if SMTP is unconfigured.
        """
        return self._send_smtp(
            recipient=recipient,
            subject="GOLU Password Reset Request",
            body=(
                f"Hello Boss,\n\nUse the following token to reset your GOLU password:\n\n"
                f"{reset_token}\n\nThis token expires in 30 minutes."
            ),
        )

    def send_verification_email(self, recipient: str, verification_token: str) -> Tuple[bool, bool]:
        """
        Returns (email_sent, delivery_configured).
        Never claims an email was sent if SMTP is unconfigured.
        """
        return self._send_smtp(
            recipient=recipient,
            subject="Verify your GOLU Boss Account Email",
            body=(
                f"Hello Boss,\n\nUse the following token to verify your GOLU email address:\n\n"
                f"{verification_token}\n\nThis token expires in 60 minutes."
            ),
        )
