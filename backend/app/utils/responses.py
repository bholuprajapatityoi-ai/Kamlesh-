from typing import Any, Dict


def ok_payload(message: str, **extra: Any) -> Dict[str, Any]:
    return {"success": True, "message": message, **extra}
