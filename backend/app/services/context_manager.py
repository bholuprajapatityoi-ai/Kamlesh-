from collections import defaultdict, deque
from threading import Lock
from typing import Dict, List


class ContextManager:
    """
    Temporary in-memory conversation context for Phase 6 multi-turn chat.
    Permanent personal memory belongs to Phase 7 and is intentionally excluded here.
    """

    def __init__(self, max_turns: int = 12) -> None:
        self._max_turns = max_turns
        self._store: Dict[str, deque[Dict[str, str]]] = defaultdict(lambda: deque(maxlen=max_turns))
        self._lock = Lock()

    def get_history(self, context_key: str) -> List[Dict[str, str]]:
        with self._lock:
            return list(self._store[context_key])

    def append_turn(self, context_key: str, user_message: str, assistant_reply: str) -> None:
        with self._lock:
            self._store[context_key].append({"role": "user", "content": user_message})
            self._store[context_key].append({"role": "assistant", "content": assistant_reply})

    def clear(self, context_key: str) -> None:
        with self._lock:
            self._store.pop(context_key, None)


context_manager = ContextManager()
