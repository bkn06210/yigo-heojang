package com.wallet.engine.service;

import com.wallet.engine.assembler.BenefitCandidateAssembler;
import com.wallet.engine.assembler.CardStateAssembler;
import com.wallet.engine.assembler.PerformanceInputAssembler;
import com.wallet.engine.dao.BenefitMapper;
import com.wallet.engine.dao.CardStateMapper;
import com.wallet.engine.dao.PaymentTargetMapper;
import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.dao.SpendingMapper;
import com.wallet.engine.dao.UserCardMapper;
import com.wallet.engine.dao.dto.PerformanceTierRow;
import com.wallet.engine.dao.dto.UserCardRow;
import com.wallet.engine.dto.RecommendationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 결제 직전 추천·카드 현황의 조회 횟수가 보유 카드 수와 무관한지 본다.
 *
 * DB 없이 도는 검증이다. 매퍼를 목으로 두고 호출 횟수를 센다 — 매퍼 메서드 호출 하나가 쿼리 하나다.
 * 카드마다 조회하는 코드가 다시 들어오면 카드 1장과 20장의 호출 수가 갈라져 여기서 걸린다.
 * 계산 결과가 맞는지는 실 DB 통합 테스트가 본다.
 */
class HeldCardQueryCountTest {

    private static final long MEMBER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    @Test
    @DisplayName("결제 직전 추천의 조회 횟수는 보유 카드가 1장이든 20장이든 같다")
    void 추천_조회_횟수는_보유카드_수와_무관하다() {
        assertThat(recommendQueryCount(20)).isEqualTo(recommendQueryCount(1));
    }

    @Test
    @DisplayName("카드 현황의 조회 횟수는 보유 카드가 1장이든 20장이든 같다")
    void 현황_조회_횟수는_보유카드_수와_무관하다() {
        assertThat(overviewQueryCount(20)).isEqualTo(overviewQueryCount(1));
    }

    private int recommendQueryCount(int cardCount) {
        Mappers mappers = Mappers.holding(cardCount);
        RecommendationService service = new RecommendationService(
                mappers.userCard(), mappers.cardState(), mappers.benefit(), mappers.performance(),
                mappers.paymentTarget(), new CardStateAssembler(), new BenefitCandidateAssembler(),
                new PerformanceInputAssembler());

        RecommendationRequest request = new RecommendationRequest();
        request.setExpectedAmount(10_000L);
        service.recommend(MEMBER_ID, request, TODAY);

        mappers.verifyBenefitsReadOnceForAll(cardCount);
        return mappers.calls();
    }

    private int overviewQueryCount(int cardCount) {
        Mappers mappers = Mappers.holding(cardCount);
        CardStatusService service = new CardStatusService(
                mappers.userCard(), mappers.cardState(), mappers.performance(), mappers.benefit(),
                new CardStateAssembler(), new PerformanceInputAssembler(),
                new CardMonthlyStatusBuilder(), new CardStatusOverviewBuilder(), mappers.spending());

        service.getOverview(MEMBER_ID, YearMonth.from(TODAY));

        mappers.verifyBenefitsReadOnceForAll(cardCount);
        return mappers.calls();
    }

    /** 엔진이 읽는 매퍼 전부. 보유 카드 목록과 카드마다 0원 구간 하나만 채우고 나머지는 빈 결과다. */
    private record Mappers(UserCardMapper userCard, CardStateMapper cardState, BenefitMapper benefit,
                           PerformanceMapper performance, PaymentTargetMapper paymentTarget,
                           SpendingMapper spending) {

        static Mappers holding(int cardCount) {
            Mappers mappers = new Mappers(mock(UserCardMapper.class), mock(CardStateMapper.class),
                    mock(BenefitMapper.class), mock(PerformanceMapper.class),
                    mock(PaymentTargetMapper.class), mock(SpendingMapper.class));

            List<UserCardRow> cards = new ArrayList<>();
            List<PerformanceTierRow> tiers = new ArrayList<>();
            for (long cardId = 1; cardId <= cardCount; cardId++) {
                UserCardRow card = new UserCardRow();
                card.setUserCardId(100 + cardId);
                card.setCardId(cardId);
                card.setCardName("카드" + cardId);
                cards.add(card);

                // 모든 카드가 0원 구간을 갖는다는 전제 — 없으면 실적 판정이 예외를 던진다
                PerformanceTierRow tier = new PerformanceTierRow();
                tier.setCardId(cardId);
                tier.setTierId(1000 + cardId);
                tier.setPeriodType("MONTH");
                tier.setMinPerformanceAmount(0L);
                tiers.add(tier);
            }
            when(mappers.userCard().findActiveCards(MEMBER_ID)).thenReturn(cards);
            when(mappers.performance().findTiersByCardIds(anyList())).thenReturn(tiers);
            return mappers;
        }

        int calls() {
            return Stream.of(userCard, cardState, benefit, performance, paymentTarget, spending)
                    .mapToInt(mapper -> mockingDetails(mapper).getInvocations().size())
                    .sum();
        }

        /** 혜택·예외를 카드마다 읽지 않고, 한 번의 조회에 모든 카드를 담았는가 */
        void verifyBenefitsReadOnceForAll(int cardCount) {
            verify(benefit, never()).findActiveBenefits(anyLong(), anyLong(), any());
            verify(benefit, never()).findExclusions(anyLong());
            verify(benefit).findActiveBenefitsByCards(argThat(selectors -> selectors.size() == cardCount));
            verify(benefit).findExclusionsByCards(argThat(cardIds -> cardIds.size() == cardCount));
        }
    }
}
