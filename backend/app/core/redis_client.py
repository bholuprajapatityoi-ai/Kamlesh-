import json
import logging
from typing import Any, Optional, Tuple

from app.core.config import get_settings

logger = logging.getLogger("golu.redis")

try:
    import redis  # type: ignore
except ImportError:  # pragma: no cover - optional in lightweight test envs
    redis = None  # type: ignore


class RedisClientWrapper:
    """
    Fail-safe Redis helper for rate limiting, short-lived caching, and background job queuing.
    If REDIS_URL is unset or Redis is unreachable, callers fall back cleanly to PostgreSQL/in-memory.
    """

    def __init__(self) -> None:
        self._client = None
        self._configured_url = ""

    def _get_client(self):
        settings = get_settings()
        url = settings.REDIS_URL.strip()
        if not url or redis is None:
            return None
        if self._client is None or self._configured_url != url:
            self._configured_url = url
            self._client = redis.from_url(
                url,
                decode_responses=True,
                socket_connect_timeout=1.5,
                socket_timeout=1.5,
            )
        return self._client

    def ping(self) -> Tuple[bool, Optional[str]]:
        client = self._get_client()
        if client is None:
            return False, "Redis not configured"
        try:
            ok = bool(client.ping())
            return ok, None if ok else "Ping returned false"
        except Exception as exc:
            return False, type(exc).__name__

    def get(self, key: str) -> Optional[str]:
        client = self._get_client()
        if client is None:
            return None
        try:
            return client.get(key)
        except Exception:
            return None

    def setex(self, key: str, ttl_seconds: int, value: str) -> bool:
        client = self._get_client()
        if client is None:
            return False
        try:
            client.setex(key, ttl_seconds, value)
            return True
        except Exception:
            return False

    def delete(self, key: str) -> None:
        client = self._get_client()
        if client is None:
            return
        try:
            client.delete(key)
        except Exception:
            return

    def incr_with_ttl(self, key: str, ttl_seconds: int) -> Optional[int]:
        client = self._get_client()
        if client is None:
            return None
        try:
            pipe = client.pipeline()
            pipe.incr(key, 1)
            pipe.expire(key, ttl_seconds, nx=True)
            result = pipe.execute()
            return int(result[0])
        except Exception:
            return None

    def enqueue_job(self, queue_name: str, payload: dict[str, Any]) -> bool:
        client = self._get_client()
        if client is None:
            return False
        try:
            client.rpush(queue_name, json.dumps(payload))
            return True
        except Exception as exc:
            logger.debug("Redis enqueue skipped (%s); database queue remains active.", type(exc).__name__)
            return False

    def dequeue_job(self, queue_name: str, timeout_seconds: int = 1) -> Optional[dict[str, Any]]:
        client = self._get_client()
        if client is None:
            return None
        try:
            item = client.blpop(queue_name, timeout=timeout_seconds)
            if not item:
                return None
            _, raw = item
            return json.loads(raw)
        except Exception:
            return None


redis_client = RedisClientWrapper()
