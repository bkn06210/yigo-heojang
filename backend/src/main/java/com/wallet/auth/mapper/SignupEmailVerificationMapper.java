package com.wallet.auth.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.auth.domain.SignupEmailVerification;

@Mapper
public interface SignupEmailVerificationMapper {
    // 이메일 기준으로 인증 정보를 조회
    SignupEmailVerification findByEmail(@Param("email") String email);

    /*
     * 이메일 기준으로 인증 정보를 조회하면서 행 잠금을 건다.
     *
     * 같은 이메일로 인증 코드 요청이나 검증 요청이 동시에 들어올 때
     * 같은 row를 동시에 변경하지 못하도록 Service의 @Transactional 안에서 사용한다.
     */
    SignupEmailVerification findByEmailForUpdate(@Param("email") String email);

    /*
     * 회원가입 인증 토큰 해시로 인증 정보를 조회한다.
     *
     * 최종 회원가입 요청에서 signupVerificationToken 원문을 해시한 뒤
     * 이 메서드로 검증 대상 인증 정보를 찾는다.
     */
    SignupEmailVerification findBySignupTokenHash(
        @Param("signupTokenHash") String signupTokenHash
    );

    /*
     * 회원가입 인증 토큰 해시로 인증 정보를 조회하면서 행 잠금을 건다.
     *
     * 최종 회원가입 요청에서 같은 signupVerificationToken이 동시에 사용되는 것을 막기 위해
     * AuthService.signup()의 @Transactional 범위 안에서 사용한다.
     */
    SignupEmailVerification findBySignupTokenHashForUpdate(
        @Param("signupTokenHash") String signupTokenHash
    );

    // 인증 정보 최초 저장
    int insert(SignupEmailVerification verification);

    /*
     * 같은 이메일로 인증 코드를 다시 요청했을 때 기존 행을 재사용한다.
     *
     * 새 인증 코드를 발급할 때는 이전 인증 상태, 실패 횟수,
     * 기존 signup token 정보를 모두 초기화한다.
     */
    int updateForReissue(
        @Param("email") String email,
        @Param("verificationCodeHash") String verificationCodeHash,
        @Param("verificationCodeExpiresAt") LocalDateTime verificationCodeExpiresAt
    );

    // 인증 번호가 틀렸을 때 실패 횟수를 1 증가시킨다.
    int increaseFailedAttemptCount(
        @Param("signupEmailVerificationId") Long signupEmailVerificationId
    );


    // 인증 코드가 만료된 경우 상태를 EXPIRED로 변경한다.
    int expireVerificationCode(
        @Param("signupEmailVerificationId") Long signupEmailVerificationId
    );

    /*
     * 인증 코드 검증 성공 시 상태를 VERIFIED로 변경하고
     * 최종 회원가입에 사용할 signup token 해시를 저장한다.
     */
    int verify(
        @Param("signupEmailVerificationId") Long signupEmailVerificationId,
        @Param("signupTokenHash") String signupTokenHash,
        @Param("signupTokenExpiresAt") LocalDateTime signupTokenExpiresAt
    );

    /*
     * 최종 회원가입 성공 후 인증 정보를 사용 완료 처리한다.
     *
     * WHERE 조건에 verification_status = 'VERIFIED'를 포함해서
     * 같은 signup token으로 동시에 회원가입 요청이 들어와도
     * 한 요청만 USED 처리에 성공하도록 한다.
     */
    int markAsUsed(
        @Param("signupEmailVerificationId") Long signupEmailVerificationId
    );
}