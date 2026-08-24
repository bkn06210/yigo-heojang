package com.wallet.engine.seed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * 선택형 혜택 묶음의 그달 선택 한 건.
     *
     * 선택 기록이 없는 묶음은 그달에 고르지 않은 것이라 혜택이 하나도 적용되지 않는다.
     * 그래서 선택형 카드를 보유만 시키고 이 행을 빼면 그 카드의 묶음 혜택이 영영 계산되지 않는다.
     *
     * @param optionGroupCode benefit.option_group_code 와 같은 값
     * @param optionKey       그달에 고른 선택지. benefit.option_key 와 같은 값
     */
    record Selection(long memberId, String cardName, String optionGroupCode, String optionKey) {
    }

    /**
     * 거래 한 건.
     *
     * @param merchantCode 가맹점 코드. 혜택이 걸린 브랜드가 아니면 null
     * @param placeName    소비내역에 표시할 상호. merchantCode가 없는 거래에만 쓴다.
     *                     이 값이 없으면 화면이 카테고리명("병원")을 상호 자리에 그대로 노출한다
     * @param categoryCode 카테고리 코드. 가맹점이 있어도 소비내역에는 항상 카테고리가 들어간다
     * @param day          결제일(1~28). 말일을 피해 모든 달에 같은 날짜가 존재하게 한다
     */
    record Txn(long memberId, String cardName, String merchantCode, String placeName, String categoryCode,
               long amount, int month, int day, int hour, String paymentType, boolean interestFree) {
    }

    /**
     * 혜택이 걸리지 않은 카테고리에 쓸 상호 목록.
     *
     * merchant 마스터에는 혜택 대상 브랜드만 있다. 그 밖의 소비는 merchant_id가 없어
     * 상호도 비어 있었고, 화면이 카테고리명을 대신 보여줘 "병원"만 열 건 늘어서는 모양이 됐다.
     * 실재하는 상호를 달마다 돌려 쓴다 — 상호는 표시용이라 엔진 계산에 관여하지 않는다.
     */
    private static final Map<String, String[]> PLACE_NAMES = Map.ofEntries(
            Map.entry("RESTAURANT", new String[]{"김밥천국 역삼점", "백채김치찌개", "홍콩반점0410", "명륜진사갈비", "청담물갈비"}),
            Map.entry("TAXI", new String[]{"카카오 T 택시", "서울개인택시", "타다 대리", "우티 택시", "국민택시"}),
            Map.entry("SUPERMARKET", new String[]{"이마트 에브리데이", "홈플러스 익스프레스", "롯데슈퍼", "농협하나로마트", "킴스클럽"}),
            Map.entry("PUBLIC_TRANSPORT", new String[]{"서울교통공사", "티머니 교통카드", "경기버스", "코레일", "서울시버스"}),
            Map.entry("HOSPITAL", new String[]{"서울내과의원", "연세이비인후과", "강남정형외과", "미소드림치과", "한빛안과의원"}),
            Map.entry("FUEL", new String[]{"SK주유소 역삼점", "GS칼텍스 논현점", "현대오일뱅크 삼성점", "S-OIL 개포점", "알뜰주유소"}),
            Map.entry("SPORTS_LEISURE", new String[]{"더클라임 클라이밍", "스포애니 헬스", "필라테스온", "골프존파크", "요가스튜디오숨"}),
            Map.entry("FAST_FOOD", new String[]{"맘스터치", "버거킹", "롯데리아", "노브랜드버거", "KFC"}),
            Map.entry("UTILITY", new String[]{"한국전력공사"}),
            Map.entry("BEAUTY", new String[]{"준오헤어", "이철헤어커커", "박승철헤어"}),
            Map.entry("CAFE", new String[]{"카페 델마노", "블루보틀", "폴바셋"}),
            Map.entry("ONLINE_SHOPPING", new String[]{"11번가", "위메프", "티몬"}),
            Map.entry("MOVIE", new String[]{"씨네Q", "인디스페이스"}),
            Map.entry("CONVENIENCE_STORE", new String[]{"이마트24", "미니스톱"}));

    /**
     * 보유 카드 배분 — 15장을 세 회원에게 나눈다.
     *
     * 한 회원에게 전부 주지 않는 이유: 카드마다 실적 조건이 있어 15장을 모두 충족시키려면
     * 월 수백만원짜리 거래를 만들어야 한다. 나눠 주면 충족·미충족이 섞여 구간 판정이 더 잘 드러난다.
     * 회원 1이 신한카드 핏을 갖는다 — 스탬프(COUNT_STEP)와 분기 실적이 이 카드에만 있다.
     *
     * <b>실적을 채운 카드를 넷 둔다</b> — 신한카드 핏·삼성 iD ON(회원 1), ALL 카드·Mr.Life(회원 2).
     * 한 장만 충족시키면 실적 조건부 혜택이 그 카드에서만 켜져, 구간별 한도·통합한도·묶음 한도가
     * 대부분 0인 채로 남는다. 화면도 "실적을 채우면 무엇이 달라지는가"를 한 장으로만 보여주게 된다.
     *
     * <b>회원 3은 넷 다 미달로 둔다.</b> 충족과 미달이 한 회원 안에 섞이기만 하면 "이 회원은 왜
     * 혜택이 적은가"를 보여줄 수 없다. 소비 규모가 작아 아무 카드도 못 채운 회원이 따로 있어야
     * 실적 조건이 결과를 가른다는 것이 드러난다.
     */
    static List<Holding> holdings() {
        return List.of(
                new Holding(1, "신한카드 핏(Fit)", true),
                new Holding(1, "ALL point 카드", false),
                new Holding(1, "YOU Wish 카드", false),
                new Holding(1, "마이핏카드(적립형)", false),
                new Holding(1, "삼성 iD ON 카드", false),
                new Holding(1, "신한카드 Deep Once", false),
                new Holding(1, "삼성카드 taptap O", false),

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

    /**
     * 선택형 혜택의 선택 — 보유카드마다 하나씩, 다섯 달 모두 같은 선택을 유지한다.
     *
     * 회원마다 다른 선택지를 고르게 둔다. 같은 카드를 가진 두 회원의 화면에 서로 다른 혜택이
     * 떠야 "매월 택1"이 실제로 회원 단위로 동작한다는 것이 시드에서 드러난다.
     *
     * 신한카드 The BEST-XO의 GIFT_OPTION은 넣지 않는다 — 증정(GIFT) 혜택이라 결제 트랜잭션이
     * 없고, 엔진이 계산·현황 조회에서 애초에 제외하므로 선택 상태가 아무 데도 쓰이지 않는다.
     */
    static List<Selection> selections() {
        return List.of(
                new Selection(1, "YOU Wish 카드", "WISH_PICK", "DAILY"),
                new Selection(1, "삼성카드 taptap O", "LIFESTYLE", "PACKAGE_2"),
                new Selection(2, "삼성 iD SELECT UP 카드", "SELECT_SERVICE", "LIVING"),
                new Selection(3, "삼성카드 taptap O", "LIFESTYLE", "PACKAGE_5"));
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

        // ALL point — 회원 1의 세 번째 충족 카드(전월 30만원).
        // 결제 화면이 추천을 3순위까지 보여주므로 한 회원이 실적을 채운 카드를 셋은 갖고 있어야
        // 순위가 끝까지 채워진다. 둘뿐이면 3순위 자리가 실적 미달 카드로 내려가, 화면이
        // 보여주려는 "혜택이 켜진 카드끼리의 비교"가 되지 않는다.
        String allPoint = "ALL point 카드";
        b.add(1, allPoint, null, "RESTAURANT", 28_000, 14, 19);
        b.add(1, allPoint, "GMARKET", "ONLINE_SHOPPING", 33_000, 18, 21);
        b.add(1, allPoint, "EMART", "LARGE_MART", 108_000, 7, 16);
        b.add(1, allPoint, "COUPANG", "ONLINE_SHOPPING", 55_000, 20, 21);
        b.add(1, allPoint, null, "RESTAURANT", 46_000, 24, 19);

        // 삼성 iD ON — 전월 30만원을 넘겨 실적 조건부 혜택이 켜지는 두 번째 카드다.
        // 커피·배달앱·델리 세 영역에 모두 결제를 넣는다. 이 카드의 주력 혜택이
        // "세 영역 중 이용금액이 가장 큰 하나에 30%"라, 한 영역만 쓰면 영역을 고르는 경로가 돌지 않는다.
        String idOn = "삼성 iD ON 카드";
        b.addAuto(1, idOn, "NETFLIX", "SUBSCRIPTION_STREAMING", 17_000, 8);
        b.add(1, idOn, "CGV", "MOVIE", 15_000, 14, 18);
        b.add(1, idOn, "BAEMIN", "DELIVERY", 38_000, 5, 19);
        b.add(1, idOn, "STARBUCKS", "CAFE", 7_000, 10, 9);
        b.add(1, idOn, "EMART", "LARGE_MART", 124_000, 17, 15);
        b.add(1, idOn, null, "RESTAURANT", 62_000, 22, 20);
        b.add(1, "신한카드 Deep Once", null, "PUBLIC_TRANSPORT", 58_000, 2, 8);
        b.add(1, "마이핏카드(적립형)", null, "FUEL", 65_000, 12, 17);
        b.add(1, "YOU Wish 카드", null, "HOSPITAL", 42_000, 21, 10);

        // ── 회원 2 ── 생활·자동납부 중심
        // Mr.Life — 전월 30만원 구간을 넘긴다. 자동납부 두 건이 규모 배수를 타지 않아
        // 달이 바뀌어도 14만원이 고정으로 깔리고, 나머지 결제가 구간을 가른다.
        String mrLife = "신한카드 Mr.Life";
        b.add(2, mrLife, null, "SUPERMARKET", 85_000, 10, 18);
        b.add(2, mrLife, null, "HOSPITAL", 31_000, 18, 10);
        b.add(2, mrLife, "LOTTE_MART", "LARGE_MART", 64_000, 14, 16);
        b.addAuto(2, mrLife, "KT", "TELECOM", 48_000, 25);
        b.addAuto(2, mrLife, null, "UTILITY", 96_000, 25);

        // ALL 카드 — 전월 40만원을 넘긴다. 회원 2의 대표 카드라 충족 상태로 두어야
        // 홈 화면이 보여주는 카드가 혜택이 켜진 상태로 나온다.
        b.add(2, "ALL 카드", null, "RESTAURANT", 52_000, 4, 19);
        b.add(2, "ALL 카드", "GMARKET", "ONLINE_SHOPPING", 63_000, 15, 21);
        b.add(2, "ALL 카드", "OLIVE_YOUNG", "BEAUTY", 29_000, 23, 16);
        b.add(2, "ALL 카드", "EMART", "LARGE_MART", 118_000, 8, 15);
        b.add(2, "ALL 카드", "SK_ENERGY", "FUEL", 68_000, 19, 11);
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
        /** 같은 달 안에서 몇 번째로 추가된 거래인지. 날짜 흔들기와 상호 고르기의 씨앗이다 */
        private int seq;

        Builder(int month, int pct) {
            this.month = month;
            this.pct = pct;
        }

        void add(long memberId, String cardName, String merchantCode, String categoryCode,
                 long amount, int day, int hour) {
            seq++;
            txns.add(new Txn(memberId, cardName, merchantCode, placeName(merchantCode, categoryCode),
                    categoryCode, scaled(amount), month, jitterDay(day), jitterHour(hour),
                    "CARD", false));
        }

        /**
         * 자동납부는 금액이 고정이라 달마다 규모를 조절하지 않는다.
         * 날짜도 흔들지 않는다 — 통신비·구독료가 매달 같은 날 같은 금액인 것은 실제 그렇다.
         */
        void addAuto(long memberId, String cardName, String merchantCode, String categoryCode,
                     long amount, int day) {
            seq++;
            // 자동납부 상호는 고정이다. 매달 다른 전력회사에 내지 않는다
            String place = merchantCode == null ? firstPlaceName(categoryCode) : null;
            txns.add(new Txn(memberId, cardName, merchantCode, place, categoryCode,
                    amount, month, day, 4, "AUTO_TRANSFER", false));
        }

        /**
         * 결제일을 달마다 ±4일 흔든다.
         *
         * 흔들지 않으면 다섯 달이 글자 하나 안 틀리고 같아져 "CGV를 매달 14일에 간다"가 된다.
         * 1~28을 벗어나면 반대쪽으로 8일 밀어 되돌린다 — 2월에도 존재하는 날짜여야 한다.
         */
        private int jitterDay(int day) {
            int shifted = day + ((month * 7 + seq * 13) % 9) - 4;
            if (shifted < 1) {
                return shifted + 8;
            }
            return shifted > 28 ? shifted - 8 : shifted;
        }

        /** 시각도 ±2시간 흔든다. 매달 같은 시각에 같은 곳을 가지는 않는다 */
        private int jitterHour(int hour) {
            int shifted = hour + ((month * 5 + seq * 3) % 5) - 2;
            return Math.max(0, Math.min(23, shifted));
        }

        /**
         * 표시용 상호를 고른다. 혜택이 걸린 브랜드(merchantCode)가 있으면 그 이름을 쓰므로 null.
         * 그 밖에는 카테고리 목록에서 달·순번으로 골라 달마다 다른 곳을 가게 한다.
         */
        private String placeName(String merchantCode, String categoryCode) {
            if (merchantCode != null) {
                return null;
            }
            String[] pool = PLACE_NAMES.get(categoryCode);
            if (pool == null) {
                return null;
            }
            return pool[Math.floorMod(month + seq, pool.length)];
        }

        private String firstPlaceName(String categoryCode) {
            String[] pool = PLACE_NAMES.get(categoryCode);
            return pool == null ? null : pool[0];
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
