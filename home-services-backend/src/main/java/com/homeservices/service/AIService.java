//package com.homeservices.service;
//
//import org.springframework.ai.chat.client.ChatClient;  // ✅ REQUIRED IMPORT
//import org.springframework.stereotype.Service;
//
//@Service
//public class AIService {
//
//    private final ChatClient chatClient;
//
//    public AIService(ChatClient.Builder builder) {
//        this.chatClient = builder.build();
//    }
//
//    public String ask(String message) {
//        return chatClient.prompt()
//                .system("You are a helpful Spring Boot tutor.")
//                .user(message)
//                .call()
//                .content();
//    }
//}