package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.ChatMode;
import com.haui.UrbanMart.dto.request.ChatRequest;
import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.ChatMessageResponse;
import com.haui.UrbanMart.entity.UserRole;
import com.haui.UrbanMart.exception.FeatureNotImplementedException;
import com.haui.UrbanMart.security.CustomUserDetails;
import com.haui.UrbanMart.service.AiRateLimitService;
import com.haui.UrbanMart.service.ChatAssistantService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatAssistantService chatAssistantService;
    private final AiRateLimitService rateLimitService;

    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/send-message")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        if (request.mode() == ChatMode.STAFF) {
            throw new FeatureNotImplementedException(
                    "Chat với nhân viên chi nhánh chưa được hỗ trợ"
            );
        }
        if (currentUser.getUser().getRole() != UserRole.CUSTOMER) {
            throw new AccessDeniedException("Chỉ khách hàng mới được sử dụng chat AI");
        }
        rateLimitService.checkChatMessageLimit(currentUser.getId());
        return ApiResponse.success(
                "Phản hồi từ AI thành công",
                chatAssistantService.send(currentUser.getId(), request)
        );
    }
}
