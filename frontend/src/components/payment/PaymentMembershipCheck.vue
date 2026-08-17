<script setup>
import { computed, watch, ref } from 'vue';
import { useCardStore } from '@/stores/cardStore';
import { getTransactions, getMemberships } from '@/api/walletApi';

const cardStore = useCardStore();

const props = defineProps({
  selectedMerchant: {
    type: String,
    default: null
  }
});

// 데이터 상태
const registeredMemberships = computed(() => cardStore.memberships);
const transactions = ref([]);

// 최근 3개월 거래 중 선택한 가맹점 사용 횟수
const merchantUsageCount = computed(() => {
  if (!props.selectedMerchant || transactions.value.length === 0) {
    return 0;
  }

  const merchantName = props.selectedMerchant.toLowerCase().trim();
  const threeMonthsAgo = new Date();
  threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);

  return transactions.value.filter((tx) => {
    const txDate = new Date(tx.transactionDate || tx.paymentDate);
    const txMerchantName = (tx.merchantName || '').toLowerCase().trim();

    return (
      txDate >= threeMonthsAgo &&
      (txMerchantName === merchantName ||
       txMerchantName.includes(merchantName) ||
       merchantName.includes(txMerchantName))
    );
  }).length;
});

// 선택한 가맹점과 연결된 멤버십 (이미 등록된 것만)
const linkedMemberships = computed(() => {
  if (!props.selectedMerchant || registeredMemberships.value.length === 0) {
    return [];
  }

  const merchantName = props.selectedMerchant.toLowerCase().trim();

  return registeredMemberships.value.filter((membership) => {
    if (!membership.usagePlaces || membership.usagePlaces.length === 0) {
      return false;
    }

    return membership.usagePlaces.some((place) => {
      const placeName = place.placeName?.toLowerCase().trim() || '';
      return (
        placeName === merchantName ||
        placeName.includes(merchantName) ||
        merchantName.includes(placeName)
      );
    });
  });
});

// 자주 이용하지만 미등록 멤버십 추천
const recommendedMemberships = computed(() => {
  if (merchantUsageCount.value < 3 || !props.selectedMerchant) {
    return [];
  }

  const merchantName = props.selectedMerchant.toLowerCase().trim();
  const registeredProviderIds = new Set(
    registeredMemberships.value.map(m => m.providerId)
  );

  // 모든 가능한 멤버십 중 미등록인 것들을 찾기
  // (실제로는 백엔드에서 가져와야 하지만, 일단 로직만)
  // TODO: 백엔드에서 getMembershipProviders()로 가져올 수도 있음
  return [];
});

// 거래 내역 로드
const loadTransactions = async () => {
  try {
    const response = await getTransactions({ limit: 100 });
    transactions.value = response?.transactions || response || [];
  } catch (error) {
    console.error('거래 내역 로드 실패:', error);
  }
};

// 멤버십 로드
const ensureMembershipsLoaded = async () => {
  if (registeredMemberships.value.length === 0) {
    try {
      await cardStore.loadMemberships();
    } catch (error) {
      console.error('멤버십 로드 실패:', error);
    }
  }
};

// 가맹점 선택 시 데이터 로드
watch(
  () => props.selectedMerchant,
  async () => {
    await Promise.all([
      ensureMembershipsLoaded(),
      loadTransactions()
    ]);

    // 디버깅
    console.log('===== 멤버십 매칭 디버깅 =====');
    console.log('selectedMerchant:', props.selectedMerchant);
    console.log('registeredMemberships:', registeredMemberships.value);
    registeredMemberships.value.forEach((m, idx) => {
      console.log(`[멤버십 ${idx}] ${m.name}:`, {
        name: m.name,
        providerId: m.providerId,
        usagePlaces: m.usagePlaces,
      });
    });
    console.log('linkedMemberships:', linkedMemberships.value);
  },
  { immediate: true }
);
</script>

<template>
  <!-- 이미 등록된 멤버십 표시 -->
  <div v-if="selectedMerchant && linkedMemberships.length > 0" class="membership-check-inline">
    <div class="section-header">
      <h3>{{ selectedMerchant }}에서 적립 가능</h3>
      <p class="section-subtitle">등록된 멤버십으로 포인트를 적립할 수 있어요</p>
    </div>

    <div class="membership-list">
      <a
        v-for="membership in linkedMemberships"
        :key="membership.id"
        :href="membership.partnerWebsiteUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="membership-card"
      >
        <div class="membership-header">
          <img
            v-if="membership.logoImageUrl"
            :src="membership.logoImageUrl"
            :alt="membership.name"
            class="membership-logo"
          />
          <div class="membership-info">
            <h4 class="membership-name">{{ membership.name }}</h4>
          </div>
        </div>

        <div class="membership-footer">
          <span class="visit-link">🔗 사이트에서 적립 확인하기</span>
        </div>
      </a>
    </div>

    <div class="info-notice">
      💡 각 멤버십의 실제 포인트 적립 현황은 해당 제공사의 사이트에서 확인해주세요
    </div>
  </div>

  <!-- 자주 이용하는 가맹점 추천 -->
  <div v-if="selectedMerchant && merchantUsageCount >= 3 && linkedMemberships.length === 0" class="recommendation-section">
    <div class="recommendation-header">
      <span class="recommendation-badge">✨ 추천</span>
      <h3>자주 이용하는 가맹점!</h3>
    </div>
    <p class="recommendation-text">
      최근 3개월간 <strong>{{ selectedMerchant }}</strong>을(를) <strong>{{ merchantUsageCount }}번</strong> 이용했어요.
      이 가맹점에서 포인트를 적립할 수 있는 멤버십을 등록해보세요.
    </p>
    <a
      href="/point/memberships"
      class="recommendation-link"
    >
      멤버십 추가하러 가기 →
    </a>
  </div>
</template>

<style scoped>
.membership-check-inline {
  width: 100%;
  padding: var(--space-md) 0;
  border-top: 1px solid var(--color-border);
  margin-top: var(--space-md);
}

.section-header {
  margin-bottom: var(--space-sm);
}

.section-header h3 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.section-subtitle {
  margin: 0;
  font-size: 11px;
  color: var(--color-text-secondary);
}

.membership-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 0;
}

.membership-card {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
  background: none;
  border: none;
  border-radius: 0;
  text-decoration: none;
  color: inherit;
  transition: all 0.2s ease;
  cursor: pointer;
}

.membership-card:hover {
  border-color: transparent;
  box-shadow: none;
  transform: none;
}

.membership-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0;
}

.membership-logo {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  object-fit: contain;
  flex-shrink: 0;
  background: var(--color-bg);
  padding: 2px;
}

.membership-info {
  flex: 1;
  min-width: 0;
}

.membership-name {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.membership-footer {
  padding-top: 0;
  border-top: none;
  text-align: left;
}

.visit-link {
  font-size: 11px;
  color: var(--color-primary-dark);
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.membership-card:hover .visit-link {
  color: var(--color-primary);
  text-decoration: underline;
}

.info-notice {
  padding: 6px 0;
  background: none;
  border-radius: 0;
  font-size: 11px;
  color: var(--color-text-secondary);
  line-height: 1.4;
  margin: 8px 0 0 0;
}

/* 자주 이용 가맹점 추천 */
.recommendation-section {
  width: 100%;
  padding: var(--space-sm) 0;
  border-top: 1px solid var(--color-border);
  border-bottom: none;
  margin: var(--space-sm) 0 0 0;
  background: none;
}

.recommendation-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.recommendation-badge {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-primary-dark);
}

.recommendation-header h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.recommendation-text {
  margin: 0 0 8px 0;
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.recommendation-text strong {
  color: var(--color-text-primary);
  font-weight: 700;
}

.recommendation-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: var(--color-primary);
  color: var(--color-btn-primary-text);
  border: none;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.recommendation-link:hover {
  background: var(--color-primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(var(--color-primary-rgb), 0.2);
}

.recommendation-link:active {
  transform: translateY(0);
}

/* 다크모드 */
[data-theme='dark'] .membership-card {
  background: none;
}

[data-theme='dark'] .membership-card:hover {
  background: none;
  box-shadow: none;
}
</style>
