from datetime import datetime, timezone
from typing import Optional


class OfflineCommandService:
    def try_handle(self, text: str, intent: str, language: str) -> Optional[str]:
        t = text.lower().strip()
        if intent == "GREETING":
            if language.lower() == "hindi":
                return "नमस्ते बॉस! गोलू बिल्कुल तैयार है, बताइए आज क्या करना है?"
            if language.lower() == "english":
                return "Hello! Boss, GOLU is ready! What is our next mission?"
            return "Hello! Boss, GOLU ready hai! Boliye kya hukum hai?"

        if "time" in t or "baje" in t:
            now_utc = datetime.now(timezone.utc).strftime("%H:%M UTC")
            return f"Boss, server clock abhi {now_utc} dikha raha hai."

        if "date" in t or "tarikh" in t:
            today = datetime.now(timezone.utc).strftime("%a, %d %b %Y")
            return f"Boss, aaj ki date {today} hai."

        return None


offline_command_service = OfflineCommandService()
