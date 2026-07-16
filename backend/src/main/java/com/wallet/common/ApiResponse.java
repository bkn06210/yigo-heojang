package com.wallet.common;

/**
 * 공통 API 응답 포맷 { success, data, message }.
 * 팀 통일 규약(CLAUDE.md): 프론트 분기 편의를 위해 상태코드 외에 body에도 성공 여부를 명시한다.
 *
 * @param <T> data의 타입
 */
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;

    public ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    /** 성공 응답. message는 null. */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 실패 응답. data는 null, 사유를 message에 담는다. */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    // Jackson 직렬화용 getter (isSuccess → "success")
    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
