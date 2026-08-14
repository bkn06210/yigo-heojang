<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getTransaction } from '@/api/walletApi';

import PageHeader from '@/components/common/PageHeader.vue';

const route = useRoute();
const router = useRouter();

// /transactions/:id 로 들어온다. 목록에서 넘겨준 expenseId다.
const expenseId = computed(() => route.params.id);

const transaction = ref(null);
const isLoading = ref(true);
const errorMessage = ref('');

// 서버 코드 → 화면 문구
const paymentStatusMap = {
  APPROVED: '승인',
  CANCELED: '취소됨',
  CANCELLED: '취소됨',
  PENDING: '대기 중',
  FAILED: '실패',
};

const inputTypeMap = {
  PAYMENT: '앱 결제',
  MANUAL: '직접 입력',
};

// 날짜 포맷팅 (YYYY.MM.DD HH:MM)
const formatDate = (dateStr) => {
  if (!dateStr) return '-';

  const d = new Date(dateStr);

  if (Number.isNaN(d.getTime())) return dateStr;

  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');

  return `${year}.${month}.${day} ${hours}:${minutes}`;
};

const formatAmount = (amount) =>
  amount == null ? '-' : `${Number(amount).toLocaleString()}원`;

// 화면이 쓰는 형태로 옮긴다. 서버가 값을 안 주는 항목(가맹점명 없는 수기 입력 등)은
// 빈칸 대신 카테고리명으로 대신 채운다 — 목록 화면과 같은 규칙이다.
const detail = computed(() => {
  const t = transaction.value;

  if (!t) return null;

  return {
    merchant: t.merchantName || t.categoryName || '-',
    category: t.categoryName || '-',
    amount: t.paymentAmount,
    discountAmount: t.discountAmount,
    benefitName: t.appliedBenefitName,
    cardName: t.cardName || '-',
    paymentDate: formatDate(t.paymentDate),
    status: paymentStatusMap[t.paymentStatus] || t.paymentStatus || '-',
    // 무이자 여부는 Y/N 한 글자로 온다.
    interestFree: t.interestFreeYn === 'Y',
    inputType: inputTypeMap[t.inputType] || t.inputType || '-',
    // 승인번호는 별도 컬럼이 없어 목록 화면과 같은 규칙으로 만든다.
    approvalNumber: String(t.expenseId ?? '').padStart(8, '0'),
  };
});

// 혜택이 적용된 건일 때만 할인 영역을 띄운다. 0원 할인을 굳이 보여줄 필요는 없다.
const hasDiscount = computed(
  () => Number(detail.value?.discountAmount) > 0,
);

const loadDetail = async () => {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    transaction.value = await getTransaction(expenseId.value);
  } catch (error) {
    console.error('소비내역 상세 조회 실패:', error);
    transaction.value = null;
    errorMessage.value =
      error?.response?.data?.message ||
      error?.message ||
      '이용내역을 불러오지 못했습니다.';
  } finally {
    isLoading.value = false;
  }
};

onMounted(loadDetail);

// 상세에서 상세로 바로 이동하는 경로가 생겨도 화면이 옛 데이터를 들고 있지 않게 한다.
watch(expenseId, loadDetail);
</script>


<template>

<div class="transaction-detail-page">

  <PageHeader title="상세 이용내역" @back="router.back()" />


  <p v-if="isLoading" class="page-state">
    불러오는 중…
  </p>


  <div v-else-if="errorMessage" class="page-state">

    <p>{{ errorMessage }}</p>

    <button class="retry-button" type="button" @click="loadDetail">
      다시 시도
    </button>

  </div>


  <section v-else-if="detail" class="detail">


    <!-- 금액 -->
    <div class="amount-box">

      <span class="amount-label">결제 금액</span>

      <strong class="amount">
        {{ formatAmount(detail.amount) }}
      </strong>

      <span v-if="hasDiscount" class="discount">
        {{ detail.benefitName || '혜택' }}
        {{ formatAmount(detail.discountAmount) }} 할인
      </span>

    </div>


    <div class="row">
      <span>가맹점명</span>
      <strong>{{ detail.merchant }}</strong>
    </div>

    <div class="row">
      <span>거래일</span>
      <strong>{{ detail.paymentDate }}</strong>
    </div>

    <div class="row">
      <span>카테고리</span>
      <strong>{{ detail.category }}</strong>
    </div>

    <div class="row">
      <span>이용카드</span>
      <strong>{{ detail.cardName }}</strong>
    </div>

    <div class="row">
      <span>거래구분</span>
      <strong>{{ detail.interestFree ? '무이자 할부' : '일시불' }}</strong>
    </div>

    <div class="row">
      <span>승인번호</span>
      <strong>{{ detail.approvalNumber }}</strong>
    </div>

    <div class="row">
      <span>거래상태</span>
      <strong>{{ detail.status }}</strong>
    </div>

    <div class="row">
      <span>입력구분</span>
      <strong>{{ detail.inputType }}</strong>
    </div>


  </section>

</div>

</template>



<style scoped>

.transaction-detail-page {
  padding: var(--space-md);
}

.page-state {
  padding: var(--space-2xl) 0;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
}

.retry-button {
  margin-top: var(--space-md);
  padding: var(--space-sm) var(--space-lg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  cursor: pointer;
}

.detail {
  margin-top: var(--space-md);
}

.amount-box {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-lg) 0;
  border-bottom: 1px solid var(--color-border);
}

.amount-label {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

.amount {
  font-size: var(--font-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.discount {
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-coral);
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-border);
  font-size: var(--font-sm);
}

.row span {
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.row strong {
  color: var(--color-text-primary);
  font-weight: var(--font-semibold);
  text-align: right;
}

</style>
