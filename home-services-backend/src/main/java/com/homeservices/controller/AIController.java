package com.homeservices.controller;

import com.homeservices.dto.request.ImageDiagnosisRequest;
import com.homeservices.dto.request.VoiceChatRequest;
import com.homeservices.dto.response.AIChatResponse;
import com.homeservices.dto.response.ApiResponse;
import com.homeservices.dto.response.ImageDiagnosisResponse;
import com.homeservices.service.GrokAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class AIController {

    private final GrokAIService grokAIService;

    /**
     * POST /ai/chat
     * Text chat — also handles voice (frontend transcribes speech → text, sends here).
     */
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<AIChatResponse>> chat(
            @RequestBody Map<String, String> body) {

        String message = body.get("message");
        AIChatResponse response = grokAIService.ask(message);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /ai/voice-chat
     * Dedicated voice endpoint. Frontend sends transcribed text + language code.
     * Same AI logic as /chat — separated for clarity and potential future TTS response.
     */
    @PostMapping("/voice-chat")
    public ResponseEntity<ApiResponse<AIChatResponse>> voiceChat(
            @RequestBody VoiceChatRequest request) {

        AIChatResponse response = grokAIService.ask(request.getTranscribedText());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /ai/diagnose-image
     * User uploads image to Cloudinary via frontend → gets URL → sends URL here.
     * AI returns diagnosis, category, and cost estimate.
     */
    @PostMapping("/diagnose-image")
    public ResponseEntity<ApiResponse<ImageDiagnosisResponse>> diagnoseImage(
            @RequestBody ImageDiagnosisRequest request) {

        ImageDiagnosisResponse response = grokAIService.diagnoseImage(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /ai/estimate-cost?appliance=AC&problem=not cooling&urgent=false
     * Returns cost estimate for a given appliance + problem.
     */
    @PostMapping("/estimate-cost")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estimateCost(
            @RequestParam String appliance,
            @RequestParam String problem,
            @RequestParam(defaultValue = "false") boolean urgent) {

        Map<String, Object> estimate = grokAIService.estimateCost(appliance, problem, urgent);
        return ResponseEntity.ok(ApiResponse.success(estimate));
    }
}
