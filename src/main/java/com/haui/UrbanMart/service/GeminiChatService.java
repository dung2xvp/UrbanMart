package com.haui.UrbanMart.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class GeminiChatService {
    private final ChatClient chatClient;

    public GeminiChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String ask(String question) {
        return chatClient.prompt().user(question).call().content();
    }
}