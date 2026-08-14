package com.wallet.common.exception;

import com.wallet.common.ApiResponse;
import com.wallet.common.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    /**
     * 경로변수·쿼리파라미터 타입 불일치 — /api/cards/undefined/monthly-status 처럼
     * 숫자 자리에 다른 값이 온 경우다. 잘못 보낸 요청이라 400이지 500이 아니다.
     *
     * 이 핸들러가 없으면 아래 catch-all 이 먼저 잡아 500으로 나간다. 스프링이 기본
     * 제공하는 400 매핑(DefaultHandlerExceptionResolver)은 @ExceptionHandler 뒤라 닿지 않는다.
     * 잘못된 요청이 서버 장애로 보이면 원인을 엉뚱한 곳에서 찾게 된다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("요청 값 '%s' 의 형식이 올바르지 않습니다.", e.getName());

        return ResponseEntity.status(ErrorCode.INPUT_INVALID.getStatus())
                .body(ApiResponse.error(ErrorCode.INPUT_INVALID.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        log.error("처리되지 않은 서버 오류", e);
        return ResponseEntity.status(ErrorCode.SERVER_INTERNAL_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.SERVER_INTERNAL_ERROR));
    }
}
