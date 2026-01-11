package com.typingquiz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * 全局异常处理器
 * 统一处理应用中的异常并返回标准错误响应
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理资源不存在异常(404)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        if (ex.getMessage() != null && ex.getMessage().contains("不存在")) {
            ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        }
        
        // 其他运行时异常返回500
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理验证错误(400)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理所有其他异常(500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "服务器内部错误: " + ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
