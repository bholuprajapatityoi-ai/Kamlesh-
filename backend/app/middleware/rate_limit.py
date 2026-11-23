import time
from collections import defaultdict, deque
from threading import Lock
from fastapi import HTTPException, status

from app.core.redis_client import redis_client


class SlidingWindowRateLimiter:
    def __init__(self) -> None:
        self._hits: dict[str, deque[float]] = defaultdict(deque)
        self._lock = Lock()

    def check(self, key: str, max_requests: int, window_seconds: int = 60) -> None:
        redis_count = redis_client.incr_with_ttl(f"rl:{key}:{int(time.time() // window_seconds)}", window_seconds)
        if redis_count is not None:
            if redis_count > max_requests:
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail="Rate limit exceeded. Please wait a minute and try again.",
                )
            return

        now = time.monotonic()
        cutoff = now - window_seconds
        with self._lock:
            q = self._hits[key]
            while q and q[0] < cutoff:
                q.popleft()
            if len(q) >= max_requests:
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail="Rate limit exceeded. Please wait a minute and try again.",
                )
            q.append(now)


rate_limiter = SlidingWindowRateLimiter()
