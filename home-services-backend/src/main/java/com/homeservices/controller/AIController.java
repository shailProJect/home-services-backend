package com.homeservices.controller;

import com.homeservices.dto.request.ChatRequest;
import com.homeservices.dto.response.AIChatResponse;
import com.homeservices.service.GrokAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/chat")
@RequiredArgsConstructor
public class AIController {

    private final GrokAIService grokAIService;

    @PostMapping("/ask")
    public AIChatResponse ask(@RequestBody ChatRequest request) {

        return grokAIService.ask(request.getMessage());
    }
}