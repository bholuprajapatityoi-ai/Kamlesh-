from app.security.sanitizer import (
    contains_secret_pattern,
    mask_email,
    sanitize_text,
)


def test_sanitize_text_strips_control_characters_and_trims():
    raw = "  Hello\x00 Boss\x07!  "
    assert sanitize_text(raw) == "Hello Boss!"


def test_contains_secret_pattern_detects_keys_and_otps():
    assert contains_secret_pattern("sk-1234567890abcdefghij")
    assert contains_secret_pattern("482910 is your otp")
    assert not contains_secret_pattern("Remind me to call Papa at 7pm")


def test_mask_email_hides_middle_characters():
    assert mask_email("bossuser@example.com") == "b***r@example.com"
    assert mask_email("ab@example.com") == "**@example.com"
