package com.legal.chat.controller;

import com.legal.chat.dto.AskRequest;
import com.legal.chat.dto.AskResponse;
import com.legal.chat.dto.ChatMessageDto;
import com.legal.chat.dto.ChatSessionDto;
import com.legal.chat.dto.CreateSessionRequest;
import com.legal.chat.dto.CreateSessionResponse;
import com.legal.chat.service.ChatService;
import com.legal.common.ApiResponse;
import com.legal.security.AuthContextHolder;
import com.legal.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/session")
    public ApiResponse<CreateSessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(chatService.createSession(principal, request));
    }

    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<List<ChatMessageDto>> listMessages(@PathVariable String sessionId) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(chatService.listMessages(principal, sessionId));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionDto>> listSessions() {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(chatService.listSessions(principal));
    }

    @PostMapping("/ask")
    public ApiResponse<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return ApiResponse.ok(chatService.ask(principal, request, MDC.get("traceId")));
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@Valid @RequestBody AskRequest request) {
        AuthPrincipal principal = AuthContextHolder.getRequired();
        return chatService.askStream(principal, request, MDC.get("traceId"));
    }
}
