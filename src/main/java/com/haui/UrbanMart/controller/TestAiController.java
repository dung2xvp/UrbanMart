package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.service.GeminiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-ai")
@RequiredArgsConstructor
public class TestAiController {
    private final GeminiChatService geminiChatService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public String test(@RequestParam String q) {
        return geminiChatService.ask(q);
    }
}