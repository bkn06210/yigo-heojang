package com.wallet.common;

/**
 * 공통 API 응답 포맷 { success, data, message }.
 * 팀 통일 규약: 프론트 분기 편의를 위해 상태코드 외에 body에도 성공 여부를 명시한다.
 *
 * @param <T> data의 타입
 */
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;

    public ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     *  성공 응답. (사용자 정의 메시지 포함)
     *  
     * @param message  성공 메시지
     * @param data     응답 데이터
     * @param <T>      데이터 타입
     * @return 성공 상태, "SUCCESS" 코드, 커스텀 메시지, 데이터를 포함한 ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", message, data);
    }

    /**
     *  성공 응답. (메시지 없음)
     *
     * @param data     응답 데이터
     * @param <T>      데이터 타입
     * @return 성공 상태, "SUCCESS" 코드, 데이터를 포함한 ApiResponse 객체 (message는 null)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", null, data);
    }

    /**
     * 실패 응답.
     *
     * @param errorCode    에러 식별 코드 (미리 정의된 ErrorCode Enum의 code값)
     * @param message      에러 상세 사유 및 메시지
     * @param <T>          데이터의 타입
     * @return 실패 상태, 에러 코드, 에러 메시지를 포함한 ApiResponse 객체 (data는 null)
     */
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, errorCode, message, null);
    }

    /**
     * 실패 응답.
     *
     * @param errorCode    에러 식별 코드 (미리 정의된 ErrorCode Enum)
     * @param <T>          데이터의 타입
     * @return 실패 상태, 에러 코드, 에러 메시지를 포함한 ApiResponse 객체 (data는 null)
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(
            false,
            errorCode.getCode(),
            errorCode.getMessage(),
            null
        );
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

    public String getCode() {
        return code;
    }
}
