package com.wallet.engine.service;

import com.wallet.engine.assembler.PerformanceInputAssembler;
import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.dao.ReconciliationMapper;
import com.wallet.engine.dao.dto.CardMonthlyStateRow;
import com.wallet.engine.dao.dto.ReconciliationTransactionRow;
import com.wallet.engine.model.ReconciliationReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 상태 대사의 조회 횟수가 대조하는 카드 수와 무관한지 본다.
 *
 * DB 없이 도는 검증이다. 매퍼를 목으로 두고 호출 횟수를 센다 — 매퍼 메서드 호출 하나가 쿼리 하나다.
 * 전 회원을 한 번에 훑는 작업이라, 카드마다 조회하는 코드가 들어오면 카드 수만큼 쿼리가 나간다.
 */
class StateReconciliationQueryCountTest {

    private static final YearMonth AUGUST = YearMonth.of(2026, 8);

    @Test
    @DisplayName("대사의 조회 횟수는 카드가 1장이든 200장이든 같다")
    void 조회_횟수는_카드_수와_무관하다() {
        assertThat(queryCount(200)).isEqualTo(queryCount(1)).isEqualTo(5);
    }

    @Test
    @DisplayName("거래가 한 건도 없으면 실적 제외 규칙은 조회하지 않는다")
    void 거래가_없으면_제외_규칙을_읽지_않는다() {
        Mappers mappers = Mappers.holding(0);

        ReconciliationReport report = mappers.service().reconcile(AUGUST, AUGUST, null);

        assertThat(report.comparisonCount()).isZero();
        // 빈 목록으로 조회하면 IN ()이 되어 SQL 문법 오류다
        verify(mappers.performance(), never()).findExclusionsByCardIds(anyList());
    }

    private int queryCount(int cardCount) {
        Mappers mappers = Mappers.holding(cardCount);

        ReconciliationReport report = mappers.service().reconcile(AUGUST, AUGUST, null);

        // 조회를 줄이느라 대조 자체를 건너뛴 것이 아닌지 — 카드마다 세 값을 맞춰 봤어야 한다
        assertThat(report.comparisonCount()).isEqualTo(cardCount * 3);
        assertThat(report.discrepancies()).isEmpty();
        // 실적 제외 규칙은 카드 전부를 한 번의 조회에 담는다
        verify(mappers.performance()).findExclusionsByCardIds(argThat(cardIds -> cardIds.size() == cardCount));
        return mappers.calls();
    }

    /** 카드마다 8월 상태 한 행과 1만원 거래 한 건을 돌려주는 목 매퍼 */
    private record Mappers(ReconciliationMapper reconciliation, PerformanceMapper performance) {

        static Mappers holding(int cardCount) {
            Mappers mappers = new Mappers(mock(ReconciliationMapper.class), mock(PerformanceMapper.class));

            List<CardMonthlyStateRow> states = new ArrayList<>();
            List<ReconciliationTransactionRow> transactions = new ArrayList<>();
            for (long cardId = 1; cardId <= cardCount; cardId++) {
                CardMonthlyStateRow state = new CardMonthlyStateRow();
                state.setUserCardId(100 + cardId);
                state.setBaseYearMonth(AUGUST.toString());
                state.setCurrentPerformanceAmount(10_000L);
                states.add(state);

                ReconciliationTransactionRow transaction = new ReconciliationTransactionRow();
                transaction.setUserCardId(100 + cardId);
                transaction.setCardId(cardId);
                transaction.setBaseYearMonth(AUGUST.toString());
                transaction.setExpenseId(1000 + cardId);
                transaction.setAmount(10_000L);
                transaction.setPaymentStatus("APPROVED");
                transaction.setCategoryCode("CAFE");
                transaction.setPaymentType("CARD");
                transaction.setIsInterestFree("N");
                transactions.add(transaction);
            }
            when(mappers.reconciliation().findStoredStates(anyString(), anyString(), any())).thenReturn(states);
            when(mappers.reconciliation().findTransactions(any(), any(), any())).thenReturn(transactions);
            return mappers;
        }

        StateReconciliationService service() {
            return new StateReconciliationService(reconciliation, performance, new PerformanceInputAssembler());
        }

        int calls() {
            return Stream.of(reconciliation, performance)
                    .mapToInt(mapper -> mockingDetails(mapper).getInvocations().size())
                    .sum();
        }
    }
}
