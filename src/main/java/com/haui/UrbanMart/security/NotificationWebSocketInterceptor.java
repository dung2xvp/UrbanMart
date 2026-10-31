package com.haui.UrbanMart.security;

import com.haui.UrbanMart.entity.User;
import com.haui.UrbanMart.entity.UserRole;
import com.haui.UrbanMart.entity.UserStatus;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationWebSocketInterceptor implements ChannelInterceptor {

    private static final String NOTIFICATIONS_DESTINATION = "/user/queue/notifications";
    private static final String BRANCH_ORDERS_DESTINATION = "/user/queue/branch-orders";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticate(accessor);
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor);
        } else if (accessor.getCommand() == StompCommand.SEND) {
            throw new AccessDeniedException("Client không được gửi message qua WebSocket");
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Thiếu access token cho WebSocket");
        }

        try {
            UUID userId = jwtUtil.getUserId(authorization.substring(7));
            String tokenRole = jwtUtil.getRole(authorization.substring(7));
            User user = userRepository.findById(userId)
                    .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                    .filter(candidate -> candidate.getLockedUntil() == null
                            || !candidate.getLockedUntil().isAfter(OffsetDateTime.now()))
                    .filter(candidate -> candidate.getRole().name().equals(tokenRole))
                    .orElseThrow(() -> new AccessDeniedException(
                            "Tài khoản không được phép kết nối WebSocket"
                    ));

            if (user.getRole() == UserRole.BRANCH
                    && branchRepository.findByAccount_Id(userId).isEmpty()) {
                throw new AccessDeniedException("Tài khoản chưa được gắn với chi nhánh");
            }

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            ));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AccessDeniedException("Access token WebSocket không hợp lệ");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null
                || !(destination.equals(NOTIFICATIONS_DESTINATION)
                || destination.equals(BRANCH_ORDERS_DESTINATION))) {
            throw new AccessDeniedException("Không được phép đăng ký kênh WebSocket này");
        }

        if (destination.equals(BRANCH_ORDERS_DESTINATION)
                && (!(accessor.getUser() instanceof Authentication authentication)
                || authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_BRANCH")))) {
            throw new AccessDeniedException("Chỉ nhân viên chi nhánh được nhận đơn mới");
        }
    }
}
