#!/usr/bin/env python3
"""Generate cryptographically strong secrets for GOLU backend .env configuration."""
import secrets


def main() -> None:
    print(f"SECRET_KEY={secrets.token_urlsafe(48)}")
    print(f"JWT_SECRET_KEY={secrets.token_urlsafe(48)}")


if __name__ == "__main__":
    main()
