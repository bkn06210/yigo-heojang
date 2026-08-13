package com.wallet.card.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MockCardSeedTest {
    private static final Path MOCK_CARD_SEED =
        Path.of("db", "93_seed_mock_card.sql");
    private static final Pattern CARD_NUMBER_VALUE = Pattern.compile(
        "\\(\\d+,\\s*'(\\d{13,19})',\\s*'[YN]'\\)"
    );

    @Test
    @DisplayName("Mock 카드 시드의 모든 카드번호는 중복 없이 룬 검증을 통과한다")
    void mockCardNumbersAreValidAndUnique() throws IOException {
        String seedSql = Files.readString(MOCK_CARD_SEED);

        // 테스트에 번호를 다시 적지 않고 실제 SQL을 읽어야 시드가 변경되었을 때도 검증할 수 있다.
        Matcher matcher = CARD_NUMBER_VALUE.matcher(seedSql);
        List<String> cardNumbers = matcher.results()
            .map(result -> result.group(1))
            .toList();

        assertThat(cardNumbers).hasSize(15).doesNotHaveDuplicates();
        assertThat(cardNumbers).allSatisfy(cardNumber ->
            assertThat(CardNumberSupport.normalizeAndValidate(cardNumber))
                .isEqualTo(cardNumber)
        );
    }
}
