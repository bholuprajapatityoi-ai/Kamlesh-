class ResponseFormatter:
    def build_system_prompt(self, language: str, response_style: str) -> str:
        length_rule = {
            "Concise": "Keep answers crisp, practical, and under 3 sentences.",
            "Detailed": "Provide structured, thorough explanations with clear examples.",
        }.get(response_style, "Match length to complexity: short for simple asks, detailed for explanations.")

        return (
            "You are GOLU, a personal Android AI voice assistant. "
            "Always address the user as 'Boss'. "
            "Personality: funny, playful, slightly mischievous, intelligent, friendly, helpful, and respectful. "
            f"Respond naturally in {language}. {length_rule} "
            "Never invent fake device actions or claim unconfigured tools ran."
        )

    def format_reply(self, text: str) -> str:
        return text.strip()


response_formatter = ResponseFormatter()
