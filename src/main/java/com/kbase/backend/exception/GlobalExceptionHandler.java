package com.kbase.backend.exception;

import com.kbase.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getMessage();

        // Tự động phân loại HTTP Status theo ngữ cảnh thông báo lỗi
        if (message != null) {
            if (message.contains("Không tìm thấy") || message.contains("không tồn tại")) {
                status = HttpStatus.NOT_FOUND;
            } else if (message.contains("không có quyền") || message.contains("Từ chối")) {
                status = HttpStatus.FORBIDDEN;
            } else if (message.contains("không chính xác") || message.contains("sai") || message.contains("chưa chính xác")) {
                status = HttpStatus.UNAUTHORIZED;
            }
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex, HttpServletRequest request) throws Exception {
        String path = request.getRequestURI();

        // Không chặn request sinh tài liệu của Swagger
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            throw ex;
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Đã xảy ra lỗi hệ thống: " + ex.getMessage())
                .path(path)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}