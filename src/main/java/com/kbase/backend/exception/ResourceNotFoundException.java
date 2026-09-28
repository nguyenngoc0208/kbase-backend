package com.kbase.backend.exception;

/**
 * Ném ra khi tài nguyên (User, Project, Document, ...) không tìm thấy.
 * GlobalExceptionHandler sẽ trả về HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
