package com.legal.common;

public class AppException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public AppException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static AppException unauthorized(String message) {
        return new AppException(40101, 401, message);
    }

    public static AppException forbidden(String message) {
        return new AppException(40301, 403, message);
    }

    public static AppException notFound(String message) {
        return new AppException(40401, 404, message);
    }

    public static AppException badRequest(String message) {
        return new AppException(40001, 400, message);
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
