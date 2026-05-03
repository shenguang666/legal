package com.legal.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handleAppException(AppException ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        if (isSseRequest(request)) {
            return sseError(ex.getHttpStatus(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<?> handleNotLogin(NotLoginException ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        if (isSseRequest(request)) {
            return sseError(401, "未登录或登录已失效");
        }
        return ResponseEntity.status(401).body(ApiResponse.fail(40102, "未登录或登录已失效"));
    }

    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public ResponseEntity<?> handleNoAuth(SaTokenException ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        if (isSseRequest(request)) {
            return sseError(403, ex.getMessage());
        }
        return ResponseEntity.status(403).body(ApiResponse.fail(40302, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        String msg = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining("; "));
        if (isSseRequest(request)) {
            return sseError(400, msg);
        }
        return ResponseEntity.badRequest().body(ApiResponse.fail(40002, msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        if (isSseRequest(request)) {
            return sseError(400, ex.getMessage());
        }
        return ResponseEntity.badRequest().body(ApiResponse.fail(40003, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        if (isSseRequest(request)) {
            return sseError(500, "系统异常，请稍后重试");
        }
        return ResponseEntity.internalServerError().body(ApiResponse.fail(50000, "系统异常，请稍后重试"));
    }

    private boolean isSseRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return request.getRequestURI().contains("/ask/stream")
                || (accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE));
    }

    private ResponseEntity<String> sseError(int status, String message) {
        String safeMessage = message == null ? "系统异常，请稍后重试" : message.replace("\r", " ").replace("\n", " ");
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body("event: error\ndata: " + safeMessage + "\n\n");
    }
}
