from typing import Tuple


class IntentClassifier:
    def classify(self, text: str) -> Tuple[str, str, float]:
        t = text.lower().strip()
        if any(k in t for k in ("ye yaad rakhna", "yaad rakhna", "remember this", "remember that")):
            return "MEMORY_SAVE_REQUEST", "OFFLINE_ENGINE", 0.96
        if any(k in t for k in ("timetable", "daily schedule", "task list", "next reminder")):
            return "DAILY_LIFE_QUERY", "OFFLINE_ENGINE", 0.93
        if any(k in t for k in ("delete ", "erase ", "transfer ", "reset ")):
            return "SENSITIVE_ACTION", "OFFLINE_ENGINE", 0.92
        if any(
            k in t
            for k in (
                "whatsapp",
                "call ",
                "alarm",
                "reminder",
                "kholo",
                "open ",
                "wifi",
                "bluetooth",
                "volume",
            )
        ):
            return "ANDROID_ACTION", "ANDROID_ACTION", 0.90
        if any(k in t for k in ("time", "baje", "date", "tarikh", "battery")):
            return "DEVICE_OR_TIME_QUERY", "OFFLINE_ENGINE", 0.94
        if t in ("hi", "hello", "hey", "hey golu", "namaste") or "kaise ho" in t:
            return "GREETING", "OFFLINE_ENGINE", 0.95
        if any(k in t for k in ("samjhao", "explain", "what is", "how does", "kyu")):
            return "EXPLANATION", "ONLINE_AI_BACKEND", 0.91
        return "GENERAL_CHAT", "ONLINE_AI_BACKEND", 0.85


intent_classifier = IntentClassifier()
