package com.legal.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException ex) {
        return ResponseEntity.status(401).body(ApiResponse.fail(40102, "未登录或登录已失效"));
    }

    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public ResponseEntity<ApiResponse<Void>> handleNoAuth(SaTokenException ex) {
        return ResponseEntity.status(403).body(ApiResponse.fail(40302, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(40002, msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(40003, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError().body(ApiResponse.fail(50000, "系统异常，请稍后重试"));
    }
}
