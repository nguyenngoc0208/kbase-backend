package com.kbase.backend.exception;

/**
 * Ném ra khi cố tạo tài nguyên đã tồn tại (email trùng, user đã là member...).
 * GlobalExceptionHandler sẽ trả về HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
