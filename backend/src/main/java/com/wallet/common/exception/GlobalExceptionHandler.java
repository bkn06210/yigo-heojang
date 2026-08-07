package com.wallet.common.exception;

import com.wallet.common.ApiResponse;
import com.wallet.common.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INPUT_INVALID;

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(ApiResponse.error(errorCode.getCode(), errorMessage));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException e) {
        String message = e.getReason() == null ? "요청 처리에 실패했습니다." : e.getReason();
        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.error("REQUEST_FAILED", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(ErrorCode.INPUT_INVALID.getStatus())
                .body(ApiResponse.error(ErrorCode.INPUT_INVALID.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        log.error("처리되지 않은 서버 오류", e);
        return ResponseEntity.status(ErrorCode.SERVER_INTERNAL_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.SERVER_INTERNAL_ERROR));
    }
}
