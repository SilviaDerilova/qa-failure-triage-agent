package com.silvia.qa.triage.llm;

public record OllamaRequest(String model, String prompt, boolean stream, String format) {
}
