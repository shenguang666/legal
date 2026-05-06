package com.legal.court.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 智能小法庭 SSE 安全发送工具。
 */
@Slf4j
@Component
public class CourtSseEmitterSupport {

    /**
     * 创建庭审流式输出 emitter。
     */
    public SseEmitter createEmitter(String traceId, AtomicBoolean streamClosed) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        emitter.onCompletion(() -> streamClosed.set(true));
        emitter.onTimeout(() -> streamClosed.set(true));
        emitter.onError(error -> {
            streamClosed.set(true);
            if (isClientDisconnect(error)) {
                log.debug("court hearing stream client disconnected traceId={}", traceId);
            } else {
                log.warn("court hearing stream failed traceId={}", traceId, error);
            }
        });
        return emitter;
    }

    /**
     * 安全发送 SSE 事件，客户端断开时只完成 emitter，不触发 Spring MVC error dispatch。
     */
    public void safeSend(SseEmitter emitter, AtomicBoolean streamClosed, String traceId, String eventName, Object data) {
        if (streamClosed.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception ex) {
            if (isClientDisconnect(ex)) {
                log.debug("court hearing stream client disconnected traceId={} event={}", traceId, eventName);
            } else {
                log.warn("court hearing stream send failed traceId={} event={}", traceId, eventName, ex);
            }
            safeComplete(emitter, streamClosed);
        }
    }

    /**
     * 安全完成 SSE 输出。
     */
    public void safeComplete(SseEmitter emitter, AtomicBoolean streamClosed) {
        if (!streamClosed.compareAndSet(false, true)) {
            return;
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

    private boolean isClientDisconnect(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SocketException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("connection reset")
                        || lower.contains("broken pipe")
                        || lower.contains("connection aborted")
                        || lower.contains("远程主机强迫关闭")
                        || lower.contains("你的主机中的软件中止了一个已建立的连接")) {
                    return true;
                }
            }
            String className = current.getClass().getName();
            if (className.contains("ClientAbortException") || className.contains("AsyncRequestNotUsableException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
