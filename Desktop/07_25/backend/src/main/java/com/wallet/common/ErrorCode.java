package com.wallet.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // =========================================================================
    //  1. 요청 검증 (Validation)
    // =========================================================================

    /** 유효하지 않은 입력값 */
    INPUT_INVALID(
        HttpStatus.BAD_REQUEST,
        "INPUT_INVALID",
        "입력값이 올바르지 않습니다."),

    /** 잘못된 쿼리 파라미터 */
    QUERY_PARAMETER_INVALID(
        HttpStatus.BAD_REQUEST,
        "QUERY_PARAMETER_INVALID",
        "잘못된 쿼리 파라미터입니다."),

    /** 잘못된 경로 파라미터 */
    PATH_PARAMETER_INVALID(
        HttpStatus.BAD_REQUEST,
        "PATH_PARAMETER_INVALID",
        "잘못된 경로 파라미터입니다."),

    /** 지원하지 않는 Enum 값 */
    ENUM_VALUE_INVALID(
        HttpStatus.BAD_REQUEST,
        "ENUM_VALUE_INVALID",
        "지원하지 않는 입력값입니다."),

    /** 리소스 상태 에러 (요청을 처리할 수 없는 상태) */
    RESOURCE_STATE_INVALID(
        HttpStatus.BAD_REQUEST,
        "RESOURCE_STATE_INVALID",
        "요청을 처리할 수 없는 리소스 상태입니다."),


    // =========================================================================
    //  2. 인증 및 권한 (Authorization)
    // =========================================================================

    /** 유효하지 않은 엑세스 토큰 */
    ACCESS_TOKEN_INVALID(
        HttpStatus.UNAUTHORIZED,
        "ACCESS_TOKEN_INVALID",
        "유효하지 않은 엑세스 토큰입니다."),

    /** 만료된 엑세스 토큰 */
    ACCESS_TOKEN_EXPIRED(
        HttpStatus.UNAUTHORIZED,
        "ACCESS_TOKEN_EXPIRED",
        "만료된 엑세스 토큰입니다."),

    /** 리프레시 토큰 갱신 실패 */
    REFRESH_TOKEN_FAILED(
        HttpStatus.UNAUTHORIZED,
        "REFRESH_TOKEN_FAILED",
        "토큰 갱신에 실패했습니다."),

    /** 접근 권한 없음 */
    ACCESS_DENIED(
        HttpStatus.FORBIDDEN,
        "ACCESS_DENIED",
        "해당 기능에 대한 접근 권한이 없습니다."),


    // =========================================================================
    //  3. 리소스 예외 (Resource)
    // =========================================================================

    /** 리소스를 찾을 수 없음 */
    NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "NOT_FOUND",
        "요청한 리소스를 찾을 수 없습니다."),


    // =========================================================================
    //  4. 서버 오류 (Server)
    // =========================================================================

    /** 서버 내부 에러 */
    SERVER_INTERNAL_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "SERVER_INTERNAL_ERROR",
        "서버 내부 오류가 발생했습니다."),

    // =========================================================================
    //  5. 회원 (Member)
    // =========================================================================

    /** 이메일 또는 비밀번호 불일치 */
    LOGIN_CREDENTIAL_MISMATCH(
        HttpStatus.UNAUTHORIZED,
        "LOGIN_CREDENTIAL_MISMATCH",
            "아이디 또는 비밀번호가 일치하지 않습니다."),

    /** 정지된 회원 */
    MEMBER_SUSPENDED(
        HttpStatus.FORBIDDEN,
        "MEMBER_SUSPENDED",
        "정지된 회원입니다."),

    /** 탈퇴한 회원 */
    MEMBER_WITHDRAWN(
        HttpStatus.FORBIDDEN,
        "MEMBER_WITHDRAWN",
        "탈퇴한 회원입니다.");
    

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}