package com.wallet.engine.service;

import java.util.Set;

/**
 * 브리핑이 소비 업종과 맞춰볼 때 쓰는, 혜택 하나의 적용 범위.
 *
 * 대상 업종만으로는 부족하다. 혜택이 대분류(교통)를 겨냥하면서 그 아래 중분류(고속시외버스)를
 * 제외하는 경우가 있어, 대상만 보고 권하면 <b>제외된 업종의 소비를 근거로</b> 카드를 권하게 된다.
 *
 * 여기 담기는 제외는 <b>업종 축뿐이다</b>. 소비 집계가 업종 단위라 결제수단·가맹점·거래속성
 * 제외는 맞춰볼 값 자체가 없다. 그 축들은 결제 시점에 BenefitMatcher가 판정한다.
 *
 * @param targetCategoryId       혜택이 겨냥한 업종. 하위 업종 소비도 대상이다
 * @param excludedCategoryCodes  제외 업종 코드. exclusion_value가 id가 아니라 코드라 코드로 담는다
 */
public record BriefingBenefitTarget(long targetCategoryId, Set<String> excludedCategoryCodes) {

    /**
     * 이 업종 소비를 근거로 삼아도 되는가. 제외 업종이면 안 된다(상위가 제외돼도 마찬가지).
     *
     * 대분류 소비는 상위 코드가 null이다. {@code Set.of()}로 만든 집합은 {@code contains(null)}에
     * NPE를 던지므로 먼저 걸러낸다 — 코드가 없는 것은 "제외에 걸리지 않는다"는 뜻이다.
     */
    public boolean covers(String categoryCode, String parentCategoryCode) {
        return !isExcluded(categoryCode) && !isExcluded(parentCategoryCode);
    }

    private boolean isExcluded(String categoryCode) {
        return categoryCode != null && excludedCategoryCodes.contains(categoryCode);
    }
}
