package com.homeservices.controller;

import org.springframework.web.bind.annotation.*;
import com.homeservices.service.GrokAIService;

@RestController
@RequestMapping("/chat")
public class AIController {

  private final GrokAIService service;

  public AIController(GrokAIService service) {
      this.service = service;
  }

  @GetMapping("/ask")
  public String ask(@RequestParam String q) {
      return service.ask(q);
  }
}