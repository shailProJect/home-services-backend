package com.homeservices.dto.request;

import lombok.Data;

@Data
public class VoiceChatRequest {
    /**
     * The transcribed text from the user's voice input.
     * The frontend uses the Web Speech API (SpeechRecognition) or
     * Whisper-compatible API to transcribe audio, then sends the text here.
     */
    private String transcribedText;

    /** Optional: the user's detected language (e.g. "hi-IN", "en-IN") */
    private String language;
}
