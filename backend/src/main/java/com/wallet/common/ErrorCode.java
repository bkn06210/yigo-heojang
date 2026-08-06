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

    /** 이미 취소된 소비내역 — 취소 정산을 다시 요청한 경우 */
    ALREADY_CANCELED(
        HttpStatus.CONFLICT,
        "ALREADY_CANCELED",
        "이미 취소된 소비내역입니다."),


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
        "탈퇴한 회원입니다."),

    /** 회원 정보를 찾을 수 없음 */
    MEMBER_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "MEMBER_NOT_FOUND",
            "회원을 찾을 수 없습니다."
    ),

    /** 이미 가입된 이메일 */
    EMAIL_ALREADY_EXISTS(
        HttpStatus.CONFLICT,
        "EMAIL_ALREADY_EXISTS",
            "이미 가입된 이메일입니다."
    ),

    // =========================================================================
    //  6. 회원가입 이메일 인증 (SignupEmailVerification)
    // =========================================================================

    /** 회원가입 이메일 인증 코드 불일치 */
    SIGNUP_EMAIL_VERIFICATION_CODE_INVALID(
        HttpStatus.BAD_REQUEST,
    "SIGNUP_EMAIL_VERIFICATION_CODE_INVALID",
        "인증 코드가 일치하지 않습니다."
    ),

    /** 회원가입 이메일 인증 코드 만료 */
    SIGNUP_EMAIL_VERIFICATION_CODE_EXPIRED(
        HttpStatus.BAD_REQUEST,
    "SIGNUP_EMAIL_VERIFICATION_CODE_EXPIRED",
        "인증 코드가 만료되었습니다."
    ),

    /** 회원가입 이메일 인증 요청 재시도 제한 */
    SIGNUP_EMAIL_VERIFICATION_REQUEST_TOO_FREQUENT(
        HttpStatus.TOO_MANY_REQUESTS,
    "SIGNUP_EMAIL_VERIFICATION_REQUEST_TOO_FREQUENT",
        "인증 코드는 잠시 후 다시 요청할 수 있습니다."
    ),

    /** 회원가입 이메일 인증 실패 횟수 초과 */
    SIGNUP_EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED(
        HttpStatus.TOO_MANY_REQUESTS,
    "SIGNUP_EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED",
        "인증 코드 입력 가능 횟수를 초과했습니다."
    ),

    /** 회원가입 이메일 인증 정보를 찾을 수 없음 */
    SIGNUP_EMAIL_VERIFICATION_NOT_FOUND(
        HttpStatus.NOT_FOUND,
    "SIGNUP_EMAIL_VERIFICATION_NOT_FOUND",
        "이메일 인증 정보를 찾을 수 없습니다."
    ),

    /** 회원가입 인증 토큰 발급 실패 */
    SIGNUP_VERIFICATION_TOKEN_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
    "SIGNUP_VERIFICATION_TOKEN_FAILED",
        "회원가입 인증 처리에 실패했습니다."
    ),

    /** 이메일 발송 실패 */
    EMAIL_SEND_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
    "EMAIL_SEND_FAILED",
        "인증 메일 발송에 실패했습니다."
    ),

    SIGNUP_VERIFICATION_TOKEN_INVALID(
        HttpStatus.BAD_REQUEST,
    "SIGNUP_VERIFICATION_TOKEN_INVALID",
        "회원가입 인증 토큰이 유효하지 않습니다."
    ),

    SIGNUP_VERIFICATION_TOKEN_EXPIRED(
        HttpStatus.BAD_REQUEST,
    "SIGNUP_VERIFICATION_TOKEN_EXPIRED",
        "회원가입 인증 토큰이 만료되었습니다."
    ),

    SIGNUP_VERIFICATION_EMAIL_MISMATCH(
        HttpStatus.BAD_REQUEST,
    "SIGNUP_VERIFICATION_EMAIL_MISMATCH",
        "인증된 이메일과 회원가입 이메일이 일치하지 않습니다."
    ),

    SIGNUP_EMAIL_VERIFICATION_ALREADY_USED(
        HttpStatus.BAD_REQUEST,
    "SIGNUP_EMAIL_VERIFICATION_ALREADY_USED",
        "이미 사용 완료된 이메일 인증 정보입니다."
    ),

    // =========================================================================
    //  7. 비밀번호 재설정 (PasswordReset)
    // =========================================================================

    /** 비밀번호 재설정 인증 코드 요청 횟수 제한 */
    PASSWORD_RESET_REISSUE_COOLDOWN(
        HttpStatus.TOO_MANY_REQUESTS,
    "PASSWORD_RESET_REISSUE_COOLDOWN",
        "비밀번호 재설정 이메일은 1분에 한 번만 요청할 수 있습니다. 잠시 후 다시 시도해 주세요."
    ),

    PASSWORD_RESET_CODE_INVALID(
        HttpStatus.BAD_REQUEST,
        "PASSWORD_RESET_CODE_INVALID",
            "인증 번호가 일치하지 않습니다."
    ),

    PASSWORD_RESET_CODE_EXPIRED(
        HttpStatus.BAD_REQUEST,
        "PASSWORD_RESET_CODE_EXPIRED",
            "만료된 인증 번호입니다."
    ),

    PASSWORD_RESET_CODE_ALREADY_USED(
        HttpStatus.CONFLICT,
        "PASSWORD_RESET_CODE_ALREADY_USED",
            "이미 사용 완료된 인증 번호입니다."
    ),

    PASSWORD_RESET_ATTEMPT_LIMIT_EXCEEDED(
        HttpStatus.TOO_MANY_REQUESTS,
        "PASSWORD_RESET_ATTEMPT_LIMIT_EXCEEDED",
            "인증 번호 입력 시도 횟수를 초과했습니다."
    ),

    PASSWORD_RESET_TOKEN_INVALID(
        HttpStatus.BAD_REQUEST,
        "PASSWORD_RESET_TOKEN_INVALID",
        "비밀번호 재설정 토큰이 유효하지 않습니다."
    ),

    PASSWORD_RESET_TOKEN_EXPIRED(
        HttpStatus.BAD_REQUEST,
        "PASSWORD_RESET_TOKEN_EXPIRED",
        "만료된 비밀번호 재설정 토큰입니다."
    ),

    PASSWORD_RESET_TOKEN_ALREADY_USED(
        HttpStatus.CONFLICT,
        "PASSWORD_RESET_TOKEN_ALREADY_USED",
        "이미 사용 완료된 비밀번호 재설정 토큰입니다."
    ),

    PASSWORD_SAME_AS_CURRENT(
        HttpStatus.BAD_REQUEST,
        "PASSWORD_SAME_AS_CURRENT",
        "기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다."
    ),

    // =========================================================================
    //  8. 카드 관리 (Card)
    // =========================================================================

    /** 카드번호가 숫자/길이/룬 알고리즘 기준을 만족하지 않는 경우 */
    CARD_NUMBER_INVALID(
        HttpStatus.BAD_REQUEST,
    "CARD_NUMBER_INVALID",
        "유효한 카드번호 형식이 아닙니다."
    ),

    /** 프로젝트가 보유한 BIN 데이터에서 카드사를 찾지 못한 경우 */
    CARD_BIN_NOT_FOUND(
        HttpStatus.BAD_REQUEST,
        "CARD_BIN_NOT_FOUND",
        "카드사를 확인할 수 없는 카드번호입니다."
    ),

    /** BIN으로 카드사는 식별했지만, 해당 카드사의 등록 가능한 활성 카드 상품이 없는 경우 */
    CARD_CANDIDATE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "CARD_CANDIDATE_NOT_FOUND",
        "등록 가능한 카드 상품을 찾을 수 없습니다."
    ),

    /** 사용자가 선택한 cardId가 존재하지 않거나 비활성 카드 상품인 경우 */
    CARD_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "CARD_NOT_FOUND",
        "등록 가능한 카드 상품을 찾을 수 없습니다."
    ),

    /** 카드번호의 BIN으로 식별한 카드사와 사용자가 선택한 카드 상품의 카드사가 다른 경우 */
    CARD_COMPANY_MISMATCH(
        HttpStatus.BAD_REQUEST,
        "CARD_COMPANY_MISMATCH",
        "카드번호와 선택한 카드 상품의 카드사가 일치하지 않습니다."
    ),

    /** 이미 ACTIVE 상태로 등록된 보유 카드를 다시 등록하려는 경우. */
    USER_CARD_ALREADY_EXISTS(
        HttpStatus.CONFLICT,
        "USER_CARD_ALREADY_EXISTS",
        "이미 등록된 카드입니다."
    ),

    /** 삭제 상태 보유 카드 재활성화 또는 신규 등록 후, 결과 조회에 실패한 경우 */
    USER_CARD_REGISTRATION_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "USER_CARD_REGISTRATION_FAILED",
        "보유 카드 등록 처리에 실패했습니다."
    ),

    /** 로그인 회원이 삭제할 수 있는 활성 보유 카드를 찾지 못한 경우 */
    USER_CARD_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "USER_CARD_NOT_FOUND",
        "보유 카드를 찾을 수 없습니다."
    );

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
