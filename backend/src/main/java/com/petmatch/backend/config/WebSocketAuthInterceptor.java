package com.petmatch.backend.config;

import com.petmatch.backend.security.JwtUtil;
import com.petmatch.backend.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Intercepts WebSocket CONNECT frames and validates the JWT token
 * passed in the Authorization STOMP header.
 * Client must send: STOMP header "Authorization: Bearer <token>"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.isValid(token)) {
                    String email = jwtUtil.extractEmail(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    accessor.setUser(auth);
                    log.debug("WebSocket authenticated: {}", email);
                } else {
                    log.warn("WebSocket CONNECT rejected: invalid JWT");
                    throw new IllegalArgumentException("Invalid JWT token in WebSocket CONNECT");
                }
            } else {
                log.warn("WebSocket CONNECT rejected: missing Authorization header");
                throw new IllegalArgumentException("Missing Authorization header in WebSocket CONNECT");
            }
        }
        return message;
    }
}
