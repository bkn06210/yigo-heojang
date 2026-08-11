package com.wallet.engine.dto;

import java.time.LocalDateTime;

/**
 * 결제 가산 정산의 입력 — 방금 승인된 결제 한 건.
 *
 * 소비내역 도메인이 결제 트랜잭션 안에서 이 값을 채워 엔진을 자바 메서드로 직접 호출한다(REST 아님).
 * 엔진은 이 정보로 혜택을 계산해 applied_benefit_id·discount_amount를 돌려주고, 상태를 가산한다.
 *
 * <b>시그니처는 잠정이다</b> — 소비내역 도메인과 연동을 맞추면서 확정한다.
 * merchantId·categoryId는 id로 받고, 매칭에 쓸 코드는 엔진이 조회로 펼친다(추천 흐름과 대칭).
 *
 * @param userCardId   보유카드 ID (상태 테이블 키)
 * @param cardId       카드 마스터 ID (혜택·실적 제외 조회 키)
 * @param categoryId   결제 카테고리 ID (소비내역 필수값)
 * @param merchantId   가맹점 ID. 혜택 걸린 브랜드가 아니면 NULL
 * @param amount       카드 승인액(포인트 차감 후)
 * @param paymentType  결제수단 (CARD, SIMPLE_PAY 등)
 * @param interestFree 무이자할부 여부
 * @param paymentDate  결제일시. 기준월과 일 소진 리셋 판정에 쓴다
 */
public record SettlementCommand(
        long userCardId,
        long cardId,
        long categoryId,
        Long merchantId,
        long amount,
        String paymentType,
        boolean interestFree,
        LocalDateTime paymentDate
) {
}
