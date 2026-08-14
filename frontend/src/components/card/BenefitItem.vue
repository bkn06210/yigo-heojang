<script setup>
import { computed } from 'vue';

const props = defineProps({
  // 한도를 공유하는 혜택 묶음. limitGroupCode 가 같은 혜택은 한 덩어리로 들어온다.
  group: {
    type: Object,
    required: true,
  },

  // 카드의 통합할인한도 잔여액. 통합한도가 없는 카드면 null.
  sharedRemaining: {
    type: Number,
    default: null,
  },
});

const won = (value) => `${Number(value).toLocaleString()}원`;

// monthlyLimit 이 null 이면 '한도 제약 없음'이다. '다 썼다'가 아니다.
const hasLimit = computed(() => props.group.monthlyLimit != null);

// usageRate 는 서버가 0~100 으로 잘라서 준다. 막대가 칸을 넘지 않도록 그대로 쓴다.
const barWidth = computed(() => `${props.group.usageRate ?? 0}%`);
</script>

<template>
  <div class="benefit-item">
    <!-- 묶음이면 혜택이 여러 줄이다. 한도는 아래에서 한 번만 보여준다. -->
    <div
      v-for="benefit in group.benefits"
      :key="benefit.benefitId"
      class="benefit-line"
    >
      <h3>{{ benefit.benefitName }}</h3>

      <!--
        '지금 받을 수 있나'는 카드가 아니라 이 혜택의 performanceMet 으로 판단한다.
        혜택마다 실적 축이 달라(전월/전분기) 카드 값과 갈릴 수 있다.
      -->
      <span
        v-if="benefit.requirePerformance && !benefit.performanceMet"
        class="badge"
      >
        실적 채우면 받을 수 있어요
      </span>
    </div>

    <!-- 한도 -->
    <p v-if="!hasLimit" class="no-limit">한도 제약 없음</p>

    <template v-else>
      <div class="limit-row">
        <span>{{ group.shared ? '묶음 한도' : '월 한도' }}</span>

        <strong>
          남은 {{ won(group.remainingLimit) }} / {{ won(group.monthlyLimit) }}
        </strong>
      </div>

      <div class="bar">
        <div class="fill" :style="{ width: barWidth }"></div>
      </div>
    </template>

    <!--
      개별 잔액이 남아 있어도 통합 지갑이 비면 못 받는다.
      두 잔액은 성격이 달라 서버가 합쳐서 내리지 않으므로 화면에서 함께 알린다.
    -->
    <p v-if="group.useSharedLimit" class="shared-note">
      통합할인한도를 함께 사용<span v-if="sharedRemaining !== null"> · 카드 통합 잔여 {{ won(sharedRemaining) }}</span>
    </p>
  </div>
</template>

<style scoped>
.benefit-item {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-surface);
}

.benefit-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.badge {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--color-point-bg);
  color: var(--color-point-icon);
  font-size: 12px;
  font-weight: 600;
}

.no-limit,
.shared-note {
  margin-top: 4px;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.limit-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 4px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.limit-row strong {
  color: var(--color-text-primary);
  font-weight: 600;
}

.bar {
  height: 6px;
  margin-top: 8px;
  border-radius: var(--radius-full);
  background: var(--color-border);
  overflow: hidden;
}

.fill {
  height: 100%;
  border-radius: var(--radius-full);
  background: var(--color-primary);
}
</style>
