<script setup>
import { ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import TransactionFilterBottomSheet from '@/components/transaction/TransactionFilterBottomSheet.vue';
import TransactionDatePickerBottomSheet from '@/components/transaction/TransactionDatePickerBottomSheet.vue';
import TransactionDetailBottomSheet from '@/components/transaction/TransactionDetailBottomSheet.vue';
import Icon from '@/components/common/Icon.vue';


const route = useRoute();
const router = useRouter();

// 카드 상세에서 들어온 경우에만 존재 — 있으면 해당 카드 소비내역만, 없으면 전체
const filterCardId = computed(() =>
  route.query.cardId ? Number(route.query.cardId) : null,
);

const filterCardName = computed(() => route.query.cardName || '');

// 조회조건 표시
const showFilter = ref(false);


// 날짜 선택 표시
const showDatePicker = ref(false);


// 상세 모달 표시
const showDetail = ref(false);


// 선택한 사용내역
const selectedTransaction = ref(null);


// 선택 날짜
const selectedDate = ref({
  startDate: '',
  endDate: '',
});


// 카드 목록
const cards = ref([
  {
    id: 1,
    name: 'KB My WE:SH 카드',
    type: '신용카드',
    imageUrl: '/images/cards/kb.png',
  },
  {
    id: 2,
    name: '신한 Deep Dream 카드',
    type: '체크카드',
    imageUrl: '/images/cards/shinhan.png',
  },
  {
    id: 3,
    name: 'KB 체크카드',
    type: '체크카드',
    imageUrl: '/images/cards/check.png',
  },
]);


// 사용내역 임시 데이터
const transactions = ref([
  {
    id: 1,
    cardId: 1,
    date: '2026.08.04 09:30',
    merchant: '스타벅스',
    cardName: 'KB My WE:SH 카드',
    amount: 8500,
    category: '음식/카페',
    status: '승인',
  },
  {
    id: 2,
    cardId: 1,
    date: '2026.08.03 18:45',
    merchant: 'GS25',
    cardName: 'KB My WE:SH 카드',
    amount: 12400,
    category: '편의점',
    status: '승인',
  },
  {
    id: 3,
    cardId: 1,
    date: '2026.08.02 14:20',
    merchant: '넥슨 게임샵',
    cardName: 'KB My WE:SH 카드',
    amount: 29000,
    category: '게임/엔터',
    status: '승인',
  },
  {
    id: 4,
    cardId: 2,
    date: '2026.08.01 16:15',
    merchant: '컬리마켓',
    cardName: '신한 Deep Dream 카드',
    amount: 45800,
    category: '쇼핑',
    status: '승인',
  },
  {
    id: 5,
    cardId: 2,
    date: '2026.07.31 10:50',
    merchant: '로또판매점',
    cardName: '신한 Deep Dream 카드',
    amount: 5000,
    category: '기타',
    status: '승인',
  },
]);

// 카드 상세에서 진입한 경우 해당 카드 소비내역만, 홈에서 진입한 경우 전체
const filteredTransactions = computed(() => {
  if (!filterCardId.value) return transactions.value;

  return transactions.value.filter(
    (transaction) => transaction.cardId === filterCardId.value,
  );
});


// 조회조건 열기
const openFilter = () => {
  showFilter.value = true;
};


// 조회조건 닫기
const closeFilter = () => {
  showFilter.value = false;
};


// 날짜 선택 열기
const openDatePicker = () => {
  showDatePicker.value = true;
};


// 날짜 선택 닫기
const closeDatePicker = () => {
  showDatePicker.value = false;
};


// 날짜 적용
const applyDate = (date) => {

  selectedDate.value = {
    startDate: date.startDate,
    endDate: date.endDate,
  };

  showDatePicker.value = false;

};


// 조회조건 적용
const applyFilter = (filter) => {

  console.log(
    '조회조건:',
    filter
  );

  showFilter.value = false;

};


// 사용내역 상세 열기
const openDetail = (transaction) => {

  selectedTransaction.value = transaction;

  showDetail.value = true;

};


// 사용내역 상세 닫기
const closeDetail = () => {

  showDetail.value = false;

  selectedTransaction.value = null;

};

</script>


<template>

<div class="transaction-page">


  <div class="header">

    <PageHeader
      :title="filterCardName ? `${filterCardName} 이용내역` : '최근 이용내역'"
      @back="router.back()"
    />


    <button
      class="filter-icon"
      @click="openFilter"
    >
      <Icon name="filter" size="sm" />
    </button>

  </div>




  <!-- 사용내역 리스트 -->

  <div
  v-for="transaction in filteredTransactions"
  :key="transaction.id"
  class="transaction-item"
  @click="openDetail(transaction)"
>

    <!-- 매장명과 금액 (한 줄) -->
    <div class="transaction-header">
      <div class="merchant-title">
        {{ transaction.merchant }}
      </div>
      <span class="amount">
        -{{ transaction.amount.toLocaleString() }}원
      </span>
    </div>

    <!-- 상세 정보 (작음) -->
    <div class="transaction-info">
      <span class="info-text">{{ transaction.date }}</span>
      <span class="info-dot">·</span>
      <span class="info-text">{{ transaction.category }}</span>
      <span class="info-dot">·</span>
      <span class="info-text">{{ transaction.cardName }}</span>
    </div>


  </div>






  <!-- 조회조건 -->

  <TransactionFilterBottomSheet

    v-if="showFilter"

    :cards="cards"

    :selectedDate="selectedDate"

    @close="closeFilter"

    @apply="applyFilter"

    @open-date-picker="openDatePicker"

  />





  <!-- 날짜 선택 -->

  <TransactionDatePickerBottomSheet

    v-if="showDatePicker"

    @close="closeDatePicker"

    @apply="applyDate"

  />





  <!-- 상세 -->

  <TransactionDetailBottomSheet

  v-if="showDetail && selectedTransaction"

  :transaction="selectedTransaction"

  @close="closeDetail"

/>

</div>

</template>


<style scoped>

.transaction-page {
  padding: var(--space-md);
  margin: 0 auto;
  max-width: 480px;
  box-sizing: border-box;
  overflow: hidden visible;
}


.header {

display:flex;
justify-content:space-between;
align-items:center;

}


.filter-icon {

width:36px;
height:36px;
border:none;
background:none;
font-size: var(--font-lg);
cursor: pointer;
color: var(--color-text-primary);

}



.transaction-item {
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.transaction-item:first-of-type {
  margin-top: var(--space-md);
}

.transaction-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-md);
}

.merchant-title {
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  letter-spacing: -0.3px;
  flex: 1;
}

.amount {
  color: var(--color-coral);
  font-weight: var(--font-bold);
  font-size: var(--font-md);
  flex-shrink: 0;
  white-space: nowrap;
}

.transaction-info {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-sm);
  color: var(--color-text-tertiary);
  flex-wrap: wrap;
}

.info-text {
  color: var(--color-text-secondary);
}

.info-dot {
  color: var(--color-text-tertiary);
}

</style>