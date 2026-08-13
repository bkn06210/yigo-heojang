package com.wallet.card.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MockCardMapperXmlTest {
    private static final String MAPPER_RESOURCE =
        "mappers/card/MockCardMapper.xml";
    private static final String FIND_ACTIVE_STATEMENT =
        "com.wallet.card.mapper.MockCardMapper.findActiveByCardNumber";

    @Test
    @DisplayName("Mock 카드 Mapper XML과 조회 구문이 정상적으로 등록된다")
    void mapperXmlLoads() throws IOException {
        Configuration configuration = new Configuration();

        try (InputStream inputStream =
                 Resources.getResourceAsStream(MAPPER_RESOURCE)) {
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                inputStream,
                configuration,
                MAPPER_RESOURCE,
                configuration.getSqlFragments()
            );
            mapperBuilder.parse();
        }

        assertThat(configuration.hasStatement(FIND_ACTIVE_STATEMENT)).isTrue();
    }
}
