<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import BenefitItem from '@/components/card/BenefitItem.vue';

// GET /api/cards/{userCardId}/monthly-status
import { getCardMonthlyStatus } from '@/api/walletApi';
import { useCardStore } from '@/stores/cardStore';

const route = useRoute();
const router = useRouter();
const cardStore = useCardStore();

// 라우트의 :id 는 userCardId 다 (카드 상세에서 /benefits/{card.id} 로 넘어온다).
const userCardId = route.params.id;

const status = ref(null);
const loading = ref(true);
const errorMessage = ref('');

const cardName = computed(() => status.value?.cardName ?? '');

// 카드 도안은 현황 API 가 주지 않는다. 목록에서 이미 받아둔 것을 쓰고, 없으면 그냥 안 그린다.
const cardImage = computed(() => {
  const card = cardStore.cards.find(
    (item) => Number(item.id) === Number(userCardId)
  );
  return card?.image || '';
});

// 통합할인한도 잔여. useSharedLimit 혜택이 개별 잔액과 별개로 한 번 더 막히는 지점이다.
// sharedLimit 이 null 이면 통합한도가 없는 카드다.
const sharedRemaining = computed(() => {
  const data = status.value;
  if (!data || data.sharedLimit == null) return null;
  return Math.max(data.sharedLimit - (data.sharedLimitUsed ?? 0), 0);
});

// limitGroupCode 가 같은 혜택은 한도를 공유한다 —
// monthlyLimit·remainingLimit·usageRate 가 전부 같은 값으로 내려온다.
// 혜택마다 따로 그리면 같은 한도가 여러 번 보여 실제보다 몇 배로 읽힌다.
// 같은 코드끼리 묶어 한도를 한 번만 노출한다. 코드가 null 이면 그 혜택 단독이다.
const benefitGroups = computed(() => {
  const groups = new Map();

  (status.value?.benefits ?? []).forEach((benefit) => {
    const key = benefit.limitGroupCode ?? `single-${benefit.benefitId}`;

    if (!groups.has(key)) {
      groups.set(key, {
        key,
        shared: benefit.limitGroupCode != null,
        monthlyLimit: benefit.monthlyLimit,
        remainingLimit: benefit.remainingLimit,
        usageRate: benefit.usageRate,
        useSharedLimit: false,
        benefits: [],
      });
    }

    const group = groups.get(key);

    group.benefits.push({
      benefitId: benefit.benefitId,
      benefitName: benefit.benefitName,
      requirePerformance: benefit.requirePerformance,
      performanceMet: benefit.performanceMet,
    });

    // 묶음 안에 하나라도 통합한도를 쓰면 그 묶음은 통합 잔여에 걸린다.
    if (benefit.useSharedLimit) {
      group.useSharedLimit = true;
    }
  });

  return [...groups.values()];
});

const load = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    status.value = await getCardMonthlyStatus(userCardId);
  } catch (error) {
    console.error('카드 혜택 현황 조회 실패:', error);
    errorMessage.value =
      error.response?.data?.message || '혜택 정보를 불러오지 못했습니다.';
  } finally {
    loading.value = false;
  }
};

onMounted(load);
</script>

<template>
  <div class="benefit-detail-page">
    <PageHeader title="혜택 상세" @back="router.back()" />

    <!-- 카드 정보 -->
    <section class="card-section">
      <img
        v-if="cardImage"
        :src="cardImage"
        :alt="cardName"
        class="card-image"
      />

      <h2>{{ cardName }}</h2>
    </section>

    <p v-if="loading" class="state">불러오는 중…</p>

    <p v-else-if="errorMessage" class="state">{{ errorMessage }}</p>

    <!--
      계산 대상이 아닌 혜택(증정·무이자할부·사후정산)은 이 목록에 오지 않는다.
      그래서 카드에 혜택이 있어도 목록이 빌 수 있다.
    -->
    <p v-else-if="!benefitGroups.length" class="state">
      이번 달 적용되는 혜택이 없어요.
    </p>

    <!-- 혜택 목록 -->
    <section v-else class="benefit-section">
      <BenefitItem
        v-for="group in benefitGroups"
        :key="group.key"
        :group="group"
        :shared-remaining="sharedRemaining"
      />
    </section>
  </div>
</template>

<style scoped>
.benefit-detail-page {
  padding: var(--space-md);
  box-sizing: border-box;
}

.card-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: var(--space-2xl);
}

.card-image {
  width: 220px;
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-md);
}

.card-section h2 {
  font-size: var(--font-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.state {
  padding: var(--space-2xl) 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
}

.benefit-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
</style>
