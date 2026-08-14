package com.wallet.auth.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.auth.domain.MemberTermAgreement;
import com.wallet.auth.domain.TermScope;

@Mapper
public interface TermAgreementMapper {

    // 요청으로 들어온 termsVersionId들이 해당 스코프에서 지금 사용할 수 있는 약관 버전인지 확인
    int countActiveTermVersionsByIds(
        @Param("termsVersionIds") List<Long> termsVersionIds,
        @Param("termScope") TermScope termScope
    );

    // 해당 스코프에서 현재 활성화된 필수 약관 버전 ID 목록을 조회
    List<Long> findActiveRequiredTermVersionIds(@Param("termScope") TermScope termScope);

    // 회원가입 시 회원의 약관 동의 이력을 저장
    int insertMemberTermAgreements(
        @Param("agreements") List<MemberTermAgreement> agreements
    );
}