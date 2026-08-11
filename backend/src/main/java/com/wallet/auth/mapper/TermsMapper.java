package com.wallet.auth.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.wallet.auth.dto.TermResponse;

@Mapper
public interface TermsMapper {

    /*
     * 회원가입 화면에서 보여줄 현재 유효한 약관 목록을 조회
     *
     * 여기서 "현재 유효한 약관"은
     * 1. term_status가 ACTIVE이고
     * 2. 시행 시작일이 현재 시각보다 이전이며
     * 3. 시행 종료일이 없거나 아직 지나지 않은 약관 버전을 의미한다.
     */
    List<TermResponse> findActiveTerms();
}