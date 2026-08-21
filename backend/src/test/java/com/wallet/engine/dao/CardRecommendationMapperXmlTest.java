package com.wallet.engine.dao;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 카드 추천이 새로 쓰는 조회 구문이 실제로 조립되는지 본다.
 *
 * DB 없이 도는 검증이다. 동적 SQL(foreach·if)은 인자 조합에 따라 문법이 깨질 수 있는데,
 * 그건 통합 테스트를 돌려야 드러난다. 여기서 조합별로 미리 조립해 본다.
 */
class CardRecommendationMapperXmlTest {

    private Configuration parse(String resource) throws IOException {
        Configuration configuration = new Configuration();
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(inputStream, configuration, resource,
                    configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private String sqlOf(Configuration configuration, String statement, Map<String, Object> params) {
        return configuration.getMappedStatement(statement)
                .getBoundSql(params)
                .getSql()
                .replaceAll("\\s+", " ");
    }

    @Test
    @DisplayName("거래 단위 조회는 취소 건을 빼고 날짜순으로 내린다")
    void 거래_조회_구문() throws IOException {
        String resource = "mappers/SpendingMapper.xml";
        String statement = "com.wallet.engine.dao.SpendingMapper.findMonthlyTransactions";
        Configuration configuration = parse(resource);

        assertThat(configuration.hasStatement(statement)).isTrue();
        String sql = sqlOf(configuration, statement, Map.of("memberId", 1L, "yearMonth", "2026-07"));

        assertThat(sql).contains("e.payment_status = 'APPROVED'");
        assertThat(sql).contains("ORDER BY e.payment_date");
        // 가맹점 미등록 결제도 카테고리 혜택을 받으므로 빠지면 안 된다
        assertThat(sql).contains("LEFT JOIN merchant");
    }

    @Test
    @DisplayName("후보 축소는 업종만 있어도, 가맹점만 있어도 조립된다")
    void 후보_축소_구문() throws IOException {
        String resource = "mappers/BenefitMapper.xml";
        String statement = "com.wallet.engine.dao.BenefitMapper.findCandidateCardIds";
        Configuration configuration = parse(resource);

        assertThat(configuration.hasStatement(statement)).isTrue();

        String both = sqlOf(configuration, statement, Map.of(
                "categoryIds", List.of(1L, 2L), "merchantIds", List.of(3L),
                "excludeCardIds", List.of(9L), "limit", 10));
        assertThat(both).contains("b.target_category_id IN")
                .contains("OR b.target_merchant_id IN")
                .contains("b.card_id NOT IN");

        String categoryOnly = sqlOf(configuration, statement, Map.of(
                "categoryIds", List.of(1L), "merchantIds", List.of(),
                "excludeCardIds", List.of(), "limit", 10));
        assertThat(categoryOnly).contains("b.target_category_id IN")
                .doesNotContain("b.target_merchant_id IN")
                .doesNotContain("b.card_id NOT IN");

        String merchantOnly = sqlOf(configuration, statement, Map.of(
                "categoryIds", List.of(), "merchantIds", List.of(3L),
                "excludeCardIds", List.of(), "limit", 10));
        assertThat(merchantOnly).contains("b.target_merchant_id IN")
                .doesNotContain("b.target_category_id IN");

        // 계산 대상이 아닌 혜택만 가진 카드가 후보로 올라오면 결과가 늘 0이다
        assertThat(both).contains("NOT IN ('GIFT', 'INSTALLMENT_FREE', 'RETROACTIVE')");
    }

    @Test
    @DisplayName("카드 마스터 조회는 연회비 행이 없는 카드도 빠뜨리지 않는다")
    void 카드_마스터_구문() throws IOException {
        String resource = "mappers/CardCatalogMapper.xml";
        String statement = "com.wallet.engine.dao.CardCatalogMapper.findByCardIds";
        Configuration configuration = parse(resource);

        assertThat(configuration.hasStatement(statement)).isTrue();
        String sql = sqlOf(configuration, statement, Map.of("cardIds", List.of(1L, 2L)));

        assertThat(sql).contains("LEFT JOIN card_annual_fee");
        assertThat(sql).contains("COALESCE(MIN(f.total_fee), 0)");
    }
}
