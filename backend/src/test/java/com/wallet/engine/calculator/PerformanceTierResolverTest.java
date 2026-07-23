package com.wallet.engine.calculator;

import com.wallet.engine.model.PerformanceProgress;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PerformanceTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class PerformanceTierResolverTest {

    private final PerformanceTierResolver resolver = new PerformanceTierResolver();

    @Nested
    @DisplayName("구간 판정")
    class Resolve {

        @Test
        void 실적이_구간_기준과_같으면_그_구간이다() {
            PerformanceTier tier = resolver.resolve(threeTiers(), 300000L);

            assertThat(tier.tierId()).isEqualTo(3L);
        }

        @Test
        void 실적이_기준을_넘으면_더_높은_구간을_고른다() {
            PerformanceTier tier = resolver.resolve(threeTiers(), 250000L);

            assertThat(tier.tierId()).isEqualTo(2L);
        }

        @Test
        void 최고_구간을_넘어도_최고_구간에_머문다() {
            PerformanceTier tier = resolver.resolve(threeTiers(), 9999999L);

            assertThat(tier.tierId()).isEqualTo(3L);
        }

        @Test
        void 실적이_0이면_0원_구간이다() {
            PerformanceTier tier = resolver.resolve(threeTiers(), 0L);

            assertThat(tier.tierId()).isEqualTo(1L);
        }

        @Test
        void 구간이_0원_하나뿐이면_그_구간이다() {
            PerformanceTier tier = resolver.resolve(zeroOnly(), 500000L);

            assertThat(tier.tierId()).isEqualTo(10L);
        }

        @Test
        void 구간_목록의_순서가_뒤섞여도_같은_구간을_고른다() {
            List<PerformanceTier> shuffled = List.of(
                    new PerformanceTier(3L, 300000L, 20000L),
                    new PerformanceTier(1L, 0L, 0L),
                    new PerformanceTier(2L, 200000L, 10000L));

            PerformanceTier tier = resolver.resolve(shuffled, 250000L);

            assertThat(tier.tierId()).isEqualTo(2L);
        }

        @Test
        void 구간_목록이_비어_있으면_예외다() {
            assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolve(List.of(), 100000L));
            assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolve(null, 100000L));
        }

        @Test
        void 실적이_음수면_예외다() {
            assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolve(threeTiers(), -1L));
        }

        @Test
        void 구간에_0원_행이_없으면_예외다() {
            // 스키마 위반 — null로 뭉개지 않고 터뜨려 시드 오류를 드러낸다
            List<PerformanceTier> noZero = List.of(
                    new PerformanceTier(2L, 200000L, 10000L),
                    new PerformanceTier(3L, 300000L, 20000L));

            assertThatIllegalStateException().isThrownBy(() -> resolver.resolve(noZero, 100000L));
        }
    }

    @Nested
    @DisplayName("실적 충족 판정")
    class Judge {

        @Test
        void 실적이_0원_구간에_머물면_미충족이다() {
            PerformanceStatus status = resolver.judge(threeTiers(), 100000L);

            assertThat(status.performanceMet()).isFalse();
            assertThat(status.tierId()).isEqualTo(1L);
        }

        @Test
        void 실적이_0원_초과_구간이면_충족이다() {
            PerformanceStatus status = resolver.judge(threeTiers(), 250000L);

            assertThat(status.performanceMet()).isTrue();
        }

        @Test
        void 판정된_구간의_tierId를_그대로_전달한다() {
            PerformanceStatus status = resolver.judge(threeTiers(), 300000L);

            assertThat(status.tierId()).isEqualTo(3L);
            assertThat(status.minPerformanceAmount()).isEqualTo(300000L);
        }

        @Test
        void 통합한도_null은_통합한도_없음으로_전달된다() {
            List<PerformanceTier> noSharedLimit = List.of(
                    new PerformanceTier(7L, 0L, null),
                    new PerformanceTier(8L, 300000L, null));

            PerformanceStatus status = resolver.judge(noSharedLimit, 400000L);

            assertThat(status.sharedMonthlyLimit()).isNull();
        }

        @Test
        void 통합한도_0은_혜택_없음으로_전달된다() {
            PerformanceStatus status = resolver.judge(threeTiers(), 100000L);

            assertThat(status.sharedMonthlyLimit()).isZero();
        }
    }

    @Nested
    @DisplayName("실적 진행률")
    class Progress {

        @Test
        void 아직_도달하지_못한_가장_낮은_구간이_목표다() {
            PerformanceProgress progress = resolver.progressOf(threeTiers(), 100000L);

            assertThat(progress.targetPerformance()).isEqualTo(200000L);
        }

        @Test
        void 중간_구간을_넘으면_그_다음_구간이_목표가_된다() {
            PerformanceProgress progress = resolver.progressOf(threeTiers(), 250000L);

            assertThat(progress.targetPerformance()).isEqualTo(300000L);
        }

        @Test
        void 전_구간을_달성하면_최고_구간이_목표다() {
            PerformanceProgress progress = resolver.progressOf(threeTiers(), 400000L);

            assertThat(progress.targetPerformance()).isEqualTo(300000L);
        }

        @Test
        void 남은_실적은_목표에서_당월_누적을_뺀_값이다() {
            PerformanceProgress progress = resolver.progressOf(threeTiers(), 120000L);

            assertThat(progress.remainingPerformance()).isEqualTo(80000L);
        }

        @Test
        void 초과_달성하면_남은_실적은_음수가_아니라_0이다() {
            PerformanceProgress progress = resolver.progressOf(threeTiers(), 400000L);

            assertThat(progress.remainingPerformance()).isZero();
        }

        @Test
        void 실적_조건이_없는_카드는_달성률이_0이_아니라_null이다() {
            // 구간이 0원 하나뿐 — "실적 조건 없음"과 "0% 달성"은 화면에서 다르다
            PerformanceProgress progress = resolver.progressOf(zeroOnly(), 500000L);

            assertThat(progress.achievementRate()).isNull();
        }

        @Test
        void 실적_조건이_없는_카드는_목표도_0이고_남은_실적도_0이다() {
            PerformanceProgress progress = resolver.progressOf(zeroOnly(), 500000L);

            assertThat(progress.targetPerformance()).isZero();
            assertThat(progress.remainingPerformance()).isZero();
        }

        @Test
        void 달성률은_소수점_첫째_자리까지_반올림한다() {
            // 목표 30만, 당월 25만 → 83.33...% → 83.3
            PerformanceProgress progress = resolver.progressOf(threeTiers(), 250000L);

            assertThat(progress.achievementRate()).isEqualByComparingTo("83.3");
        }

        @Test
        void 전_구간을_달성하면_달성률은_100_이상이다() {
            PerformanceProgress progress = resolver.progressOf(threeTiers(), 600000L);

            assertThat(progress.achievementRate()).isEqualByComparingTo("200.0");
        }
    }

    @Nested
    @DisplayName("두 축이 섞이지 않는다")
    class TwoAxes {

        @Test
        void 전월실적과_당월누적이_다르면_충족_여부와_목표가_따로_판정된다() {
            // 전월 52만(50만 구간 충족)인데 당월 40만(50만 미달, 달성률 80%)인 상황
            List<PerformanceTier> tiers = List.of(
                    new PerformanceTier(4L, 0L, 0L),
                    new PerformanceTier(5L, 300000L, 30000L),
                    new PerformanceTier(6L, 500000L, 50000L));

            PerformanceStatus status = resolver.judge(tiers, 520000L);
            PerformanceProgress progress = resolver.progressOf(tiers, 400000L);

            assertThat(status.performanceMet()).isTrue();
            assertThat(progress.targetPerformance()).isEqualTo(500000L);
            assertThat(progress.achievementRate()).isEqualByComparingTo("80.0");
        }
    }

    /** 카드 1의 구간 — 0원(0), 20만(1만), 30만(2만) */
    private List<PerformanceTier> threeTiers() {
        return List.of(
                new PerformanceTier(1L, 0L, 0L),
                new PerformanceTier(2L, 200000L, 10000L),
                new PerformanceTier(3L, 300000L, 20000L));
    }

    /** 실적 조건 없는 카드 — 0원 구간 하나 */
    private List<PerformanceTier> zeroOnly() {
        return List.of(new PerformanceTier(10L, 0L, 0L));
    }
}
