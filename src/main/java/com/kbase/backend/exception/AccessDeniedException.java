package com.kbase.backend.exception;

/**
 * Ném ra khi người dùng không có quyền thực hiện hành động.
 * GlobalExceptionHandler sẽ trả về HTTP 403.
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
