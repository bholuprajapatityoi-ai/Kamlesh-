import logging
import re

SENSITIVE_PATTERNS = [
    (re.compile(r"(Bearer\s+)[A-Za-z0-9\-_\.]+", re.IGNORECASE), r"\1[REDACTED]"),
    (re.compile(r"(sk-[A-Za-z0-9\-_]{10,})"), "[REDACTED_API_KEY]"),
    (re.compile(r"(password[\"':=\s]+)[^\s,\"'}]+", re.IGNORECASE), r"\1[REDACTED]"),
]


class RedactingFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        if isinstance(record.msg, str):
            msg = record.msg
            for pattern, repl in SENSITIVE_PATTERNS:
                msg = pattern.sub(repl, msg)
            record.msg = msg
        return True


def configure_logging() -> None:
    root_logger = logging.getLogger()
    if not root_logger.handlers:
        logging.basicConfig(
            level=logging.INFO,
            format="%(asctime)s %(levelname)s %(name)s %(message)s",
        )
    redactor = RedactingFilter()
    for handler in root_logger.handlers:
        handler.addFilter(redactor)
