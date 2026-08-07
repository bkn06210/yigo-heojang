package com.wallet.engine.seed;

import java.util.ArrayList;
import java.util.List;

/**
 * 92_seed_user_data.sql 을 만들 입력 — 보유 카드와 거래 목록.
 *
 * <b>여기 있는 것이 "사실"이고, 계산 결과는 여기 없다.</b> 적용된 혜택·할인액·월 실적·한도 소진은
 * 엔진이 거래를 처리하며 채운다({@link UserSeedReplayTest}). 사람이 계산 결과까지 손으로 적으면
 * 묶음 한도·구간별 한도 상속·일 소진 리셋이 얽혀 규칙과 어긋난 값이 남는다.
 *
 * 카드·가맹점·카테고리는 id가 아니라 <b>이름과 코드로</b> 지정한다. 91 시드를 다시 만들면 id가
 * 달라질 수 있어 숫자로 적어두면 조용히 다른 카드를 가리키게 된다.
 *
 * 기간은 2026-04 ~ 2026-08이다. 분기 실적을 쓰는 카드가 있어 직전 분기(4·5·6월)가 필요하고,
 * 전월실적은 7월, 기준월은 8월이다. 두 달치만 있으면 분기 축이 통째로 검증되지 않는다.
 */
final class UserSeedScenario {

    /** 거래가 있는 달. 8월이 기준월, 4~6월이 직전 분기다 */
    static final int[] MONTHS = {4, 5, 6, 7, 8};

    /**
     * 달마다 소비 규모를 다르게 둔다(%). 실적 구간이 달마다 갈려야
     * "지난달은 충족, 이번 달은 미달" 같은 상태가 시드에 실제로 나타난다.
     * 8월(기준월)을 낮게 둬 당월 진행률이 100%가 아닌 상태로 남긴다.
     */
    private static final int[] SCALE = {100, 120, 65, 130, 60};

    static final int YEAR = 2026;

    private UserSeedScenario() {
    }

    /** 보유 카드 한 장 */
    record Holding(long memberId, String cardName, boolean representative) {
    }

    /**
     * 거래 한 건.
     *
     * @param merchantCode 가맹점 코드. 혜택이 걸린 브랜드가 아니면 null
     * @param categoryCode 카테고리 코드. 가맹점이 있어도 소비내역에는 항상 카테고리가 들어간다
     * @param day          결제일(1~28). 말일을 피해 모든 달에 같은 날짜가 존재하게 한다
     */
    record Txn(long memberId, String cardName, String merchantCode, String categoryCode,
               long amount, int month, int day, int hour, String paymentType, boolean interestFree) {
    }

    /**
     * 보유 카드 배분 — 15장을 세 회원에게 나눈다.
     *
     * 한 회원에게 전부 주지 않는 이유: 카드마다 실적 조건이 있어 15장을 모두 충족시키려면
     * 월 수백만원짜리 거래를 만들어야 한다. 나눠 주면 충족·미충족이 섞여 구간 판정이 더 잘 드러난다.
     * 회원 1이 신한카드 핏을 갖는다 — 스탬프(COUNT_STEP)와 분기 실적이 이 카드에만 있다.
     */
    static List<Holding> holdings() {
        return List.of(
                new Holding(1, "신한카드 핏(Fit)", true),
                new Holding(1, "ALL point 카드", false),
                new Holding(1, "YOU Wish 카드", false),
                new Holding(1, "마이핏카드(적립형)", false),
                new Holding(1, "삼성 iD ON 카드", false),
                new Holding(1, "신한카드 Deep Once", false),

                new Holding(2, "ALL 카드", true),
                new Holding(2, "마이핏카드(할인형)", false),
                new Holding(2, "삼성 iD SELECT UP 카드", false),
                new Holding(2, "신한카드 Mr.Life", false),
                new Holding(2, "신한카드 Simple", false),

                new Holding(3, "삼성 iD SIMPLE 카드", true),
                new Holding(3, "삼성카드 & POINT", false),
                new Holding(3, "삼성카드 taptap O", false),
                new Holding(3, "신한카드 The BEST-XO", false));
    }

    /** 다섯 달치 거래를 시간순으로 만든다 */
    static List<Txn> transactions() {
        List<Txn> all = new ArrayList<>();
        for (int i = 0; i < MONTHS.length; i++) {
            all.addAll(monthlyPattern(MONTHS[i], SCALE[i]));
        }
        all.sort((a, b) -> {
            int byDate = Integer.compare(a.month() * 10000 + a.day() * 100 + a.hour(),
                    b.month() * 10000 + b.day() * 100 + b.hour());
            return byDate != 0 ? byDate : Long.compare(a.memberId(), b.memberId());
        });
        return all;
    }

    /**
     * 한 달치 생활 패턴. 달이 바뀌어도 같은 곳에서 쓰되 금액만 달라진다.
     *
     * 회원 1의 편의점이 한 달에 여섯 번인 것은 의도한 것이다 — 신한카드 핏의 스탬프가
     * 5회마다 지급이라 그보다 적으면 스탬프 경로가 한 번도 돌지 않는다.
     */
    private static List<Txn> monthlyPattern(int month, int pct) {
        Builder b = new Builder(month, pct);

        // ── 회원 1 ── 신한카드 핏이 주력 카드다.
        // 이 카드는 전월 40만원을 넘겨야 스탬프·특정영역 혜택이 켜지므로, 생활비의 큰 몫을 여기 몰아준다.
        // 규모 배수와 맞물려 4·5·7월은 충족, 6·8월은 미달이 되어 시드에 두 상태가 함께 남는다.
        String fit = "신한카드 핏(Fit)";
        b.add(1, fit, "GS25", "CONVENIENCE_STORE", 7_500, 3, 8);
        b.add(1, fit, "STARBUCKS", "CAFE", 5_600, 5, 9);
        b.add(1, fit, null, "RESTAURANT", 38_000, 6, 19);
        b.add(1, fit, "CU", "CONVENIENCE_STORE", 6_200, 7, 21);
        b.add(1, fit, null, "TAXI", 12_000, 9, 23);
        b.add(1, fit, "GS25", "CONVENIENCE_STORE", 8_900, 11, 12);
        b.add(1, fit, "COUPANG", "ONLINE_SHOPPING", 46_000, 13, 15);
        // 전분기 실적으로 판정하는 혜택(Flex)의 대상이다. 건당 5만원 조건이 있어
        // 규모가 가장 낮은 달에도 넘도록 금액을 잡았다 — 안 그러면 분기 축이 한 번도 안 돈다.
        b.add(1, fit, null, "SPORTS_LEISURE", 88_000, 15, 16);
        b.add(1, fit, "SEVEN_ELEVEN", "CONVENIENCE_STORE", 5_400, 16, 19);
        b.add(1, fit, null, "SUPERMARKET", 95_000, 17, 11);
        b.add(1, fit, "STARBUCKS", "CAFE", 6_100, 19, 14);
        b.add(1, fit, null, "RESTAURANT", 24_500, 20, 20);
        b.add(1, fit, "GS25", "CONVENIENCE_STORE", 9_100, 22, 8);
        b.add(1, fit, null, "SUPERMARKET", 90_000, 23, 19);
        b.add(1, fit, null, "TAXI", 9_800, 24, 22);
        b.addAuto(1, fit, "SKT", "TELECOM", 55_000, 25);
        b.add(1, fit, "CU", "CONVENIENCE_STORE", 6_800, 26, 20);

        String allPoint = "ALL point 카드";
        b.add(1, allPoint, null, "RESTAURANT", 28_000, 14, 19);
        b.add(1, allPoint, "GMARKET", "ONLINE_SHOPPING", 33_000, 18, 21);

        b.addAuto(1, "삼성 iD ON 카드", "NETFLIX", "SUBSCRIPTION_STREAMING", 17_000, 8);
        b.add(1, "삼성 iD ON 카드", "CGV", "MOVIE", 15_000, 14, 18);
        b.add(1, "신한카드 Deep Once", null, "PUBLIC_TRANSPORT", 58_000, 2, 8);
        b.add(1, "마이핏카드(적립형)", null, "FUEL", 65_000, 12, 17);
        b.add(1, "YOU Wish 카드", null, "HOSPITAL", 42_000, 21, 10);

        // ── 회원 2 ── 생활·자동납부 중심
        String mrLife = "신한카드 Mr.Life";
        b.add(2, mrLife, null, "SUPERMARKET", 85_000, 10, 18);
        b.add(2, mrLife, null, "HOSPITAL", 31_000, 18, 10);
        b.addAuto(2, mrLife, "KT", "TELECOM", 48_000, 25);
        b.addAuto(2, mrLife, null, "UTILITY", 96_000, 25);

        b.add(2, "ALL 카드", null, "RESTAURANT", 52_000, 4, 19);
        b.add(2, "ALL 카드", "GMARKET", "ONLINE_SHOPPING", 63_000, 15, 21);
        b.add(2, "ALL 카드", "OLIVE_YOUNG", "BEAUTY", 29_000, 23, 16);
        b.add(2, "마이핏카드(할인형)", "STARBUCKS", "CAFE", 5_900, 9, 9);
        b.add(2, "마이핏카드(할인형)", "COFFEE_BEAN", "CAFE", 6_400, 27, 15);
        b.add(2, "삼성 iD SELECT UP 카드", null, "TAXI", 11_500, 12, 23);
        b.add(2, "신한카드 Simple", "CU", "CONVENIENCE_STORE", 4_800, 6, 7);
        b.add(2, "신한카드 Simple", null, "FAST_FOOD", 13_200, 21, 13);

        // ── 회원 3 ── 소비 규모가 작아 실적 미달이 섞인다
        b.add(3, "삼성 iD SIMPLE 카드", null, "PUBLIC_TRANSPORT", 41_000, 3, 8);
        b.addAuto(3, "삼성 iD SIMPLE 카드", "LG_UPLUS", "TELECOM", 39_000, 25);
        b.addAuto(3, "삼성카드 & POINT", "WAVVE", "SUBSCRIPTION_STREAMING", 13_900, 8);
        b.add(3, "삼성카드 & POINT", "COUPANG", "ONLINE_SHOPPING", 27_000, 16, 14);
        b.add(3, "삼성카드 taptap O", "MEGABOX", "MOVIE", 14_000, 20, 18);
        b.add(3, "신한카드 The BEST-XO", null, "RESTAURANT", 88_000, 11, 20);

        return b.result();
    }

    /** 달·규모를 반복해서 적지 않으려는 조립기 */
    private static final class Builder {
        private final int month;
        private final int pct;
        private final List<Txn> txns = new ArrayList<>();

        Builder(int month, int pct) {
            this.month = month;
            this.pct = pct;
        }

        void add(long memberId, String cardName, String merchantCode, String categoryCode,
                 long amount, int day, int hour) {
            txns.add(new Txn(memberId, cardName, merchantCode, categoryCode,
                    scaled(amount), month, day, hour, "CARD", false));
        }

        /** 자동납부는 금액이 고정이라 달마다 규모를 조절하지 않는다 */
        void addAuto(long memberId, String cardName, String merchantCode, String categoryCode,
                     long amount, int day) {
            txns.add(new Txn(memberId, cardName, merchantCode, categoryCode,
                    amount, month, day, 4, "AUTO_TRANSFER", false));
        }

        /** 백원 단위로 끊어 금액이 지저분해지지 않게 한다 */
        private long scaled(long amount) {
            return Math.round(amount * pct / 100.0 / 100) * 100;
        }

        List<Txn> result() {
            return txns;
        }
    }
}
