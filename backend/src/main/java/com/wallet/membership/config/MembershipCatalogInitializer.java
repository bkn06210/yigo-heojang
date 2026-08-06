package com.wallet.membership.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 기존 DB에 누락된 제휴 멤버십 제공사를 데이터 손실 없이 보충한다. */
@Component
public class MembershipCatalogInitializer implements InitializingBean {

    private static final String[][] MEMBERSHIP_PROVIDERS = {
        {"CJ ONE", "/images/cj-one.png", "3", "CJ 계열 제휴 멤버십"},
        {"해피포인트", "/images/happypoint.png", "1", "SPC 계열 제휴 멤버십"},
        {"L.POINT", "/images/lpoint.png", "2", "롯데 계열 제휴 멤버십"},
        {"신세계포인트", "/images/ssg-point.png", "4", "신세계 계열 제휴 멤버십"},
        {"H.Point", "/images/h-point.png", "5", "현대백화점 그룹 제휴 멤버십"},
        {"OK캐쉬백", "/images/okcashbag.png", "6", "포인트 적립·사용 멤버십"},
        {"GS ALL 멤버십", "/images/gs-all.png", "7", "GS 계열 통합 멤버십"},
        {"E.POINT", "/images/e-point.png", "8", "이랜드 계열 제휴 멤버십"},
        {"NH멤버스", "/images/nh-members.png", "9", "농협 포인트 멤버십"},
        {"뷰티포인트", "/images/beauty-point.png", "10", "아모레퍼시픽 뷰티 멤버십"},
        {"T 멤버십", "/images/t-membership.png", "11", "SKT 통신사 멤버십"},
        {"KT 멤버십", "/images/kt-membership.png", "12", "KT 통신사 멤버십"},
        {"U+ 멤버십", "/images/uplus-membership.png", "13", "LG U+ 통신사 멤버십"},
        {"네이버플러스 멤버십", "/images/naver-plus.png", "14", "네이버 구독형 멤버십"},
        {"PAYCO 포인트", "/images/payco-point.png", "15", "PAYCO 포인트 멤버십"},
        {"삼성패션 멤버십", null, "16", "삼성물산 패션 멤버십"},
        {"LF Members", "/images/lf-members.png", "17", "LF 패션 멤버십"},
        {"한섬 THE 클럽", "/images/handsome-club.png", "18", "한섬 패션 멤버십"},
        {"블루멤버스", "/images/blue-members.png", "19", "현대자동차 멤버십"},
        {"기아멤버스", "/images/kia-members.png", "20", "기아자동차 멤버십"}
    };

    private final JdbcTemplate jdbcTemplate;

    public MembershipCatalogInitializer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void afterPropertiesSet() {
        Integer tableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name = 'point_provider'",
            Integer.class
        );
        if (tableCount == null || tableCount == 0) {
            return;
        }

        String sql = "INSERT INTO point_provider "
            + "(point_provider_name, point_provider_type, logo_image_url, use_yn, "
            + "default_recommend_yn, recommend_priority, recommend_message) "
            + "VALUES (?, 'MEMBERSHIP', ?, 'Y', 'Y', ?, ?) "
            + "ON DUPLICATE KEY UPDATE point_provider_type = VALUES(point_provider_type), "
            + "logo_image_url = VALUES(logo_image_url), use_yn = 'Y', "
            + "default_recommend_yn = 'Y', recommend_priority = VALUES(recommend_priority), "
            + "recommend_message = VALUES(recommend_message)";

        for (String[] provider : MEMBERSHIP_PROVIDERS) {
            jdbcTemplate.update(
                sql,
                provider[0],
                provider[1],
                Integer.valueOf(provider[2]),
                provider[3]
            );
        }
    }
}
