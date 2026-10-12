package com.wallet.engine.assembler;

import com.wallet.engine.dao.dto.BenefitExclusionRow;
import com.wallet.engine.dao.dto.BenefitRow;
import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitExclusion;
import com.wallet.engine.model.ExclusionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 카드 전체 예외를 혜택마다 붙이는 규칙의 단위 테스트.
 *
 * 조회는 카드 전체 예외를 한 줄로만 준다. 그 한 줄이 그 카드의 모든 혜택에 붙지 않으면 예외가
 * 조용히 빠져 받을 수 없는 혜택이 계산되고, 다른 카드의 혜택에 붙으면 받을 혜택이 사라진다.
 * 둘 다 에러 없이 금액만 틀리므로 여기서 고정한다.
 */
class BenefitCandidateAssemblerTest {

    private static final long CARD_A = 1L;
    private static final long CARD_B = 2L;

    private final BenefitCandidateAssembler assembler = new BenefitCandidateAssembler();

    @Test
    @DisplayName("카드 전체 예외는 그 카드의 모든 혜택에 붙는다")
    void 카드_전체_예외는_그_카드의_모든_혜택에_붙는다() {
        List<BenefitRow> benefits = List.of(benefit(CARD_A, 11L), benefit(CARD_A, 12L));
        List<BenefitExclusionRow> exclusions = List.of(
                cardWide(CARD_A, "TRANSACTION_ATTR", "INTEREST_FREE"));

        List<BenefitCandidate> candidates = assembler.toCandidates(benefits, exclusions);

        assertThat(candidates).hasSize(2);
        assertThat(candidates).allSatisfy(candidate -> assertThat(candidate.getExclusions())
                .containsExactly(new BenefitExclusion(ExclusionType.TRANSACTION_ATTR, "INTEREST_FREE")));
    }

    @Test
    @DisplayName("혜택별 예외는 그 혜택에만 붙고, 카드 전체 예외와 한 목록으로 합쳐진다")
    void 혜택별_예외와_카드_전체_예외가_한_목록으로_합쳐진다() {
        List<BenefitRow> benefits = List.of(benefit(CARD_A, 11L), benefit(CARD_A, 12L));
        List<BenefitExclusionRow> exclusions = List.of(
                own(CARD_A, 11L, "CATEGORY", "DELIVERY"),
                cardWide(CARD_A, "CATEGORY", "GIFT_CARD"));

        Map<Long, BenefitCandidate> candidatesById = byBenefitId(assembler.toCandidates(benefits, exclusions));

        assertThat(candidatesById.get(11L).getExclusions()).containsExactlyInAnyOrder(
                new BenefitExclusion(ExclusionType.CATEGORY, "DELIVERY"),
                new BenefitExclusion(ExclusionType.CATEGORY, "GIFT_CARD"));
        assertThat(candidatesById.get(12L).getExclusions()).containsExactly(
                new BenefitExclusion(ExclusionType.CATEGORY, "GIFT_CARD"));
    }

    @Test
    @DisplayName("여러 카드의 행을 함께 넘겨도 다른 카드의 카드 전체 예외는 붙지 않는다")
    void 다른_카드의_카드_전체_예외는_붙지_않는다() {
        List<BenefitRow> benefits = List.of(benefit(CARD_A, 11L), benefit(CARD_B, 21L));
        List<BenefitExclusionRow> exclusions = List.of(cardWide(CARD_A, "CATEGORY", "GIFT_CARD"));

        Map<Long, BenefitCandidate> candidatesById = byBenefitId(assembler.toCandidates(benefits, exclusions));

        assertThat(candidatesById.get(11L).getExclusions()).hasSize(1);
        assertThat(candidatesById.get(21L).getExclusions()).isEmpty();
    }

    @Test
    @DisplayName("예외 행으로 받아도 같은 규칙으로 붙고, 예외가 없는 혜택은 빈 목록이다")
    void 예외_행으로_받아도_같은_규칙으로_붙는다() {
        List<BenefitRow> benefits = List.of(benefit(CARD_A, 11L), benefit(CARD_A, 12L), benefit(CARD_B, 21L));
        List<BenefitExclusionRow> exclusions = List.of(
                own(CARD_A, 11L, "CATEGORY", "DELIVERY"),
                cardWide(CARD_A, "CATEGORY", "GIFT_CARD"));

        Map<Long, List<BenefitExclusionRow>> rowsByBenefit = assembler.exclusionRowsByBenefit(benefits, exclusions);

        assertThat(rowsByBenefit.get(11L)).extracting(BenefitExclusionRow::getExclusionValue)
                .containsExactlyInAnyOrder("DELIVERY", "GIFT_CARD");
        assertThat(rowsByBenefit.get(12L)).extracting(BenefitExclusionRow::getExclusionValue)
                .containsExactly("GIFT_CARD");
        assertThat(rowsByBenefit.get(21L)).isEmpty();
    }

    @Test
    @DisplayName("혜택 행에 카드 id가 없으면 조용히 넘어가지 않고 실패한다")
    void 혜택_행에_카드_id가_없으면_실패한다() {
        // 조회가 card_id를 빠뜨리면 0으로 온다. 그대로 두면 카드 전체 예외가 어디에도 붙지 않는다
        List<BenefitRow> benefits = List.of(benefit(0L, 11L));
        List<BenefitExclusionRow> exclusions = List.of(cardWide(CARD_A, "CATEGORY", "GIFT_CARD"));

        assertThatThrownBy(() -> assembler.toCandidates(benefits, exclusions))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Map<Long, BenefitCandidate> byBenefitId(List<BenefitCandidate> candidates) {
        return candidates.stream()
                .collect(java.util.stream.Collectors.toMap(BenefitCandidate::getBenefitId, candidate -> candidate));
    }

    /** 전 가맹점 1% 할인 — 예외를 붙이는 규칙만 보므로 나머지 조건은 비운다 */
    private BenefitRow benefit(long cardId, long benefitId) {
        BenefitRow row = new BenefitRow();
        row.setCardId(cardId);
        row.setBenefitId(benefitId);
        row.setBenefitKind("DISCOUNT");
        row.setCalcMethod("RATE");
        row.setBenefitValue(new BigDecimal("1.00"));
        row.setTargetType("ALL");
        row.setRequirePerformance("N");
        row.setUseSharedLimit("N");
        return row;
    }

    private BenefitExclusionRow own(long cardId, long benefitId, String exclusionType, String exclusionValue) {
        BenefitExclusionRow row = cardWide(cardId, exclusionType, exclusionValue);
        row.setBenefitId(benefitId);
        return row;
    }

    /** 카드 전체 예외 — 조회가 benefit_id를 NULL로 준다 */
    private BenefitExclusionRow cardWide(long cardId, String exclusionType, String exclusionValue) {
        BenefitExclusionRow row = new BenefitExclusionRow();
        row.setCardId(cardId);
        row.setExclusionType(exclusionType);
        row.setExclusionValue(exclusionValue);
        return row;
    }
}
