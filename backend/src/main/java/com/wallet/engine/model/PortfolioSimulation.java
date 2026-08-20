package com.wallet.engine.model;

import java.util.Collections;
import java.util.List;

/**
 * 카드 여러 장을 들고 결제마다 최적 카드를 골랐을 때의 결과.
 *
 * <b>왜 여러 장을 한 번에 도는가</b> — 카드 한 장으로 전부 결제한다고 보면 "카페는 늘지만 마트는
 * 주는" 상황이 총액에 섞여 들어간다. 실제로는 마트에서 원래 카드를 계속 쓰면 되므로 그런 손해가
 * 나지 않는다. 그리고 이 앱은 결제할 때마다 최적 카드를 추천하므로, 그 전제로 계산해야
 * 화면이 실제로 안내할 결과와 같아진다.
 *
 * 카드를 한 장 추가했을 때의 순증은 두 번 돌려 빼면 나온다.
 * <pre>
 * 보유 카드만            → X
 * 보유 카드 + 후보 한 장 → Y
 * 순증 = Y − X          (후보를 안 써도 되니 Y는 X보다 작아질 수 없다)
 * </pre>
 *
 * @param totalBenefitAmount 총 혜택액(원)
 * @param shares             카드별 몫. 혜택액이 큰 순서, 동점이면 cardId 오름차순
 */
public record PortfolioSimulation(long totalBenefitAmount, List<CardShare> shares) {

    public PortfolioSimulation {
        shares = shares == null ? List.of() : Collections.unmodifiableList(List.copyOf(shares));
    }

    public static PortfolioSimulation empty() {
        return new PortfolioSimulation(0L, List.of());
    }

    /** 카드 하나가 가져간 몫. appliedPaymentCount는 그 카드로 혜택을 받은 결제 건수다 */
    public record CardShare(
            long cardId,
            long benefitAmount,
            int appliedPaymentCount,
            List<SpendingSimulation.BenefitBreakdown> breakdowns
    ) {
        public CardShare {
            breakdowns = breakdowns == null ? List.of() : Collections.unmodifiableList(List.copyOf(breakdowns));
        }
    }
}
