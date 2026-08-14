package com.wallet.card.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 시드에 들어 있는 Mock 카드번호가 실제로 등록 가능한 값인지 확인한다.
 * <p>
 * 서비스가 등록 전에 룬(Luhn) 검증을 하므로, 시드 번호가 이를 통과하지 못하면
 * 시연 중에 "등록을 지원하지 않는 카드번호"가 아니라 "유효하지 않은 형식"으로
 * 엉뚱하게 막힌다. 번호를 손으로 추가하다 실수하기 쉬워 테스트로 고정한다.
 */
class MockCardSeedTest {
    private static final Path SEED_FILE = Path.of("db", "93_seed_mock_card.sql");

    // INSERT VALUES 행에서 카드번호만 뽑는다. 예: (1,  '2228790000000016', 'Y'),
    private static final Pattern CARD_NUMBER_PATTERN =
        Pattern.compile("\\(\\s*\\d+\\s*,\\s*'(\\d+)'\\s*,\\s*'[YN]'\\s*\\)");

    @Test
    @DisplayName("시드의 Mock 카드번호는 모두 룬 검증을 통과하고 중복되지 않는다")
    void seedCardNumbersAreValid() throws IOException {
        List<String> cardNumbers = readSeedCardNumbers();

        assertThat(cardNumbers)
            .as("시드 파일에서 카드번호를 하나도 읽지 못했다면 파싱이 깨진 것이다")
            .isNotEmpty();

        assertThat(cardNumbers)
            .as("카드번호가 중복되면 uk_mock_card_number 제약에 걸린다")
            .doesNotHaveDuplicates();

        for (String cardNumber : cardNumbers) {
            assertThat(cardNumber.length())
                .as("카드번호 길이: %s", cardNumber)
                .isBetween(13, 19);

            assertThat(CardNumberSupport.normalizeAndValidate(cardNumber))
                .as("룬 검증을 통과해야 한다: %s", cardNumber)
                .isEqualTo(cardNumber);
        }
    }

    private List<String> readSeedCardNumbers() throws IOException {
        String seed = Files.readString(SEED_FILE, StandardCharsets.UTF_8);

        Matcher matcher = CARD_NUMBER_PATTERN.matcher(seed);

        return matcher.results()
            .map(result -> result.group(1))
            .toList();
    }
}
