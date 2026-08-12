package com.wallet.engine.service;

import com.wallet.engine.dto.BriefingType;

import java.util.List;
import java.util.Map;

/**
 * 홈 브리핑 문구 표.
 *
 * <b>문장은 LLM으로 미리 작성해 두고 서비스할 때는 고르기만 한다.</b> 요청마다 LLM을 부르면
 * 홈 진입이 1~3초 늦어지는데, 홈은 앱을 열자마자 보여야 하는 화면이다. 상황이 여섯 가지로
 * 정해져 있어 미리 써둘 수 있다는 점이 챗봇과 다르다 — 그쪽은 질문이 무한해서 미리 못 쓴다.
 *
 * 문구를 여러 벌 두는 것은 매달 같은 말만 나오지 않게 하기 위해서다. 고르는 기준은
 * <b>기준월</b>이라 같은 달 안에서는 새로고침해도 문장이 바뀌지 않는다 — 숫자는 그대로인데
 * 말만 달라지면 매번 지어내는 것처럼 보인다.
 *
 * 자리표시자는 서버가 채운다. 숫자를 문장에 끼우는 일까지 LLM에 맡기지 않는다.
 */
final class BriefingMessages {

    private BriefingMessages() {
    }

    /**
     * 상황별 문구.
     *
     * <b>금액 총계를 말하는 문구는 두지 않는다.</b> "이번 달 2만원 놓쳤어요" 같은 말은
     * 거래별 계산을 더해야 나오는데, 그 합은 월 한도에 막히는 몫을 반영하지 못해 실제보다 크다.
     */
    private static final Map<BriefingType, List<String>> MESSAGES = Map.of(
            BriefingType.NO_CARD, List.of(
                    "등록하신 카드가 없습니다. 카드를 등록하면 실적과 혜택을 계산해 드려요.",
                    "보유 카드가 없습니다. 카드를 추가하면 맞춤 안내가 시작됩니다.",
                    "아직 등록된 카드가 없습니다. 카드를 등록해 혜택을 확인해 보세요."),

            BriefingType.UNUSED_BENEFIT, List.of(
                    "{category}에서 {count}번 결제하셨네요. 다음엔 {card}로 결제하면 {benefit}을 받을 수 있어요.",
                    "{category} 결제를 {count}번 하셨습니다. {card}의 {benefit}을 아직 안 쓰셨어요.",
                    "{category}에서 {count}번 결제하셨습니다. {card}의 {benefit}을 활용해 보세요."),

            BriefingType.PERFORMANCE_NEAR, List.of(
                    "보유하신 카드 {cardCount}장 중 {card} 카드 실적이 {rate}%로 가장 임박했어요."
                            + " 이번 달은 이 카드부터 채우는 걸 추천드려요.",
                    "카드 {cardCount}장 중 {card}의 실적이 {rate}%로 가장 가깝습니다. 이 카드부터 채워 보세요.",
                    "{card}의 실적이 {rate}%입니다. 보유 카드 {cardCount}장 중 가장 임박했어요."),

            BriefingType.ALL_ACHIEVED, List.of(
                    "실적 조건을 모두 채우셨습니다. 이제 한도 안에서 혜택이 적용됩니다.",
                    "카드 실적 조건을 모두 달성했습니다. 남은 혜택 한도를 확인해 보세요.",
                    "모든 실적 조건을 충족했습니다. 혜택을 확인해 보세요."),

            BriefingType.SPENDING_INSIGHT, List.of(
                    "이번 달 {category}에서 {count}번 결제하셨습니다. 혜택이 있는 카드를 살펴보세요.",
                    "{category}에서 {count}번 결제하셨네요. 관련 혜택이 있는 카드를 확인해 보세요.",
                    "이번 달은 {category} 결제가 {count}번으로 가장 많았습니다."),

            BriefingType.GETTING_STARTED, List.of(
                    "이번 달 결제 기록이 없습니다. 결제하시면 혜택 계산이 시작됩니다.",
                    "아직 이번 달 결제가 없습니다. 결제하시면 실적과 혜택이 계산됩니다.",
                    "이번 달 결제 기록이 아직 없습니다. 결제 후 혜택이 자동으로 반영됩니다."));

    /**
     * 문구 하나를 고른다.
     *
     * 기준월로 고르므로 같은 달에는 항상 같은 문장이 나온다. 무작위로 고르면 같은 상태인데
     * 새로고침할 때마다 말이 바뀌어, 숫자까지 매번 지어낸다는 인상을 준다.
     *
     * @param yearMonth 기준 연월 (YYYY-MM). 같은 달 안에서 문장을 고정하는 기준이다
     */
    static String pick(BriefingType type, String yearMonth) {
        List<String> variants = MESSAGES.get(type);
        // hashCode는 음수가 나올 수 있어 floorMod로 감싼다. %만 쓰면 음수 인덱스가 된다.
        int index = Math.floorMod((yearMonth == null ? "" : yearMonth).hashCode(), variants.size());
        return variants.get(index);
    }
}
