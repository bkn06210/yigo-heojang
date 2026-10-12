package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitExclusion;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PaymentTarget;

import java.util.Objects;

/**
 * 혜택 매칭기 — "이 혜택이 이번 결제에 해당하는가"만 판정한다. 금액은 계산하지 않는다.
 *
 * 두 판정이 순서대로 걸린다:
 * 1) 대상 매칭 — 혜택이 겨냥한 가맹점·카테고리가 이번 결제와 맞는가
 * 2) 제외 판정 — 맞더라도 예외 규칙에 걸리지 않는가 ("외식 5%, 단 배달앱 제외")
 *
 * 카테고리 계층은 양쪽에 대칭으로 적용된다. 대분류를 겨냥한 혜택이 하위 중분류 결제에
 * 매칭되듯, 대분류를 제외한 규칙도 하위 중분류 결제를 제외한다.
 *
 * 대상 매칭은 id로, 제외 판정은 코드 문자열로 한다 — 스키마가 그렇게 되어 있다
 * (benefit.target_category_id는 id, benefit_exclusion.exclusion_value는 'CAFE' 같은 코드).
 */
public final class BenefitMatcher {

    private static final String INTEREST_FREE = "INTEREST_FREE";

    public boolean matches(BenefitCandidate candidate, PaymentRequest request) {
        if (candidate == null || request == null) {
            throw new IllegalArgumentException("candidate와 request는 필수다");
        }
        return matchesTarget(candidate, request.target()) && !isExcluded(candidate, request);
    }

    private boolean matchesTarget(BenefitCandidate candidate, PaymentTarget target) {
        return switch (candidate.getTargetType()) {
            case ALL -> true;
            case MERCHANT -> Objects.equals(candidate.getTargetMerchantId(), target.getMerchantId());
            // 혜택이 대분류를 겨냥하면 하위 중분류 결제도 매칭된다
            case CATEGORY -> Objects.equals(candidate.getTargetCategoryId(), target.getCategoryId())
                    || Objects.equals(candidate.getTargetCategoryId(), target.getParentCategoryId());
        };
    }

    private boolean isExcluded(BenefitCandidate candidate, PaymentRequest request) {
        return candidate.getExclusions().stream()
                .anyMatch(exclusion -> hits(exclusion, request));
    }

    private boolean hits(BenefitExclusion exclusion, PaymentRequest request) {
        PaymentTarget target = request.target();
        return switch (exclusion.type()) {
            // 대상 매칭과 대칭 — 대분류 제외는 하위 중분류 결제까지 제외한다
            case CATEGORY -> exclusion.value().equals(target.getCategoryCode())
                    || exclusion.value().equals(target.getParentCategoryCode());
            case MERCHANT -> exclusion.value().equals(target.getMerchantCode());
            case PAYMENT_TYPE -> exclusion.value().equals(request.paymentType());
            case TRANSACTION_ATTR -> hitsTransactionAttribute(exclusion.value(), request);
        };
    }

    /**
     * 거래 속성 제외. expense가 구분해 주는 속성은 무이자할부뿐이라 그것만 판정한다.
     * 해외이용·상품권 같은 나머지는 판정할 데이터가 없어 제외하지 않는다.
     * 추천 시점엔 할부 방식이 정해지지 않아 일시불로 보고, 정산 시점엔 실제 값으로 판정한다.
     */
    private boolean hitsTransactionAttribute(String attribute, PaymentRequest request) {
        return INTEREST_FREE.equals(attribute) && request.interestFree();
    }
}
