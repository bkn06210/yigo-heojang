<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getTransactions } from '@/api/walletApi';

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

// 페이지네이션
const currentPage = ref(1);
const itemsPerPage = 15;
const pagesPerGroup = 5;


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


// 사용내역 API에서 조회
const transactions = ref([]);

// 카드 필터링 (페이지네이션 전)
const allFilteredTransactions = computed(() => {
  let result = transactions.value;

  if (filterCardId.value) {
    result = result.filter(
      (transaction) => transaction.cardId === filterCardId.value,
    );
  }

  return result;
});

// 페이지네이션된 거래
const filteredTransactions = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage;
  return allFilteredTransactions.value.slice(start, start + itemsPerPage);
});

// 총 페이지 수
const totalPages = computed(() => {
  return Math.ceil(allFilteredTransactions.value.length / itemsPerPage);
});

// 현재 페이지 그룹의 시작 페이지
const currentGroupStart = computed(() => {
  return Math.floor((currentPage.value - 1) / pagesPerGroup) * pagesPerGroup + 1;
});

// 현재 페이지 그룹의 끝 페이지
const currentGroupEnd = computed(() => {
  return Math.min(currentGroupStart.value + pagesPerGroup - 1, totalPages.value);
});

// 표시할 페이지 배열
const visiblePages = computed(() => {
  const pages = [];
  for (let i = currentGroupStart.value; i <= currentGroupEnd.value; i++) {
    pages.push(i);
  }
  return pages;
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

// 페이지 이동
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
};

// 맨 앞으로
const firstPage = () => {
  goToPage(1);
};

// 이전 그룹으로
const prevGroup = () => {
  const newStart = Math.max(currentGroupStart.value - pagesPerGroup, 1);
  goToPage(newStart);
};

// 다음 그룹으로
const nextGroup = () => {
  const newStart = currentGroupStart.value + pagesPerGroup;
  if (newStart <= totalPages.value) {
    goToPage(newStart);
  }
};

// 맨 뒤로
const lastPage = () => {
  goToPage(totalPages.value);
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

// 카드타입 변환
const cardTypeMap = {
  CREDIT: '신용',
  DEBIT: '체크',
};

// 거래 구분 변환
const transactionTypeMap = {
  LUMP_SUM: '일시불',
  INSTALLMENT: '할부',
};

// 거래 상태 변환
const paymentStatusMap = {
  APPROVED: '승인',
  PENDING: '대기 중',
  CANCELLED: '취소됨',
  FAILED: '실패',
};

// 날짜 포맷팅 (YYYY.MM.DD HH:MM)
const formatDate = (dateStr) => {
  const d = new Date(dateStr);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  return `${year}.${month}.${day} ${hours}:${minutes}`;
};

// 사용내역 조회
const loadTransactions = async () => {
  try {
    const params = filterCardId.value ? { userCardId: filterCardId.value } : {};
    console.log('거래 로드 - filterCardId:', filterCardId.value, 'params:', params);
    const response = await getTransactions(params);
    const rawTransactions = response?.transactions || [];
    transactions.value = rawTransactions.map((t) => ({
      id: t.expenseId,
      cardId: t.userCardId,
      merchant: t.merchantName || t.categoryName,
      amount: t.paymentAmount,
      date: formatDate(t.paymentDate),
      category: t.categoryName,
      cardLastDigits: t.cardName.slice(-4),
      cardType: cardTypeMap[t.cardType] || t.cardType,
      installment: transactionTypeMap[t.transactionType] || t.transactionType,
      approvalNumber: String(t.expenseId).padStart(8, '0'),
      status: paymentStatusMap[t.paymentStatus] || t.paymentStatus,
    }));
  } catch (error) {
    console.error('사용내역 조회 실패:', error);
  }
};

onMounted(() => {
  loadTransactions();
});

// 쿼리 파라미터(카드 선택) 변경 시 다시 로드
watch(() => route.query.cardId, () => {
  currentPage.value = 1;
  loadTransactions();
});

</script>


<template>

<div class="transaction-page">


  <div class="header">

    <PageHeader
      :title="filterCardName ? `${filterCardName} 이용내역` : '카드이용내역'"
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
        {{ transaction.amount.toLocaleString() }}원
      </span>
    </div>

    <!-- 상세 정보 (작음) -->
    <div class="transaction-info">
      <span class="info-text">{{ transaction.date }}</span>
      <span class="info-dot">|</span>
      <span class="info-text">본인 {{ transaction.cardLastDigits.slice(0, 3) }}*</span>
      <span class="info-dot">|</span>
      <span class="info-text">{{ transaction.category }}</span>
      <span class="info-dot">|</span>
      <span class="info-text">{{ transaction.installment }}</span>
      <span class="info-dot">|</span>
      <span class="info-text">{{ transaction.cardType }}</span>
    </div>


  </div>

  <!-- 페이지네이션 -->
  <div v-if="totalPages > 1" class="pagination">
    <button
      class="nav-button"
      @click="firstPage"
      :disabled="currentPage === 1"
    >
      &lt;&lt;
    </button>
    <button
      class="nav-button"
      @click="prevGroup"
      :disabled="currentGroupStart === 1"
    >
      &lt;
    </button>

    <div class="page-numbers">
      <button
        v-for="page in visiblePages"
        :key="page"
        class="page-button"
        :class="{ active: currentPage === page }"
        @click="goToPage(page)"
      >
        {{ page }}
      </button>
    </div>

    <button
      class="nav-button"
      @click="nextGroup"
      :disabled="currentGroupEnd === totalPages"
    >
      &gt;
    </button>
    <button
      class="nav-button"
      @click="lastPage"
      :disabled="currentPage === totalPages"
    >
      &gt;&gt;
    </button>
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

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-2xl);
  margin-bottom: var(--space-xl);
}

.nav-button {
  background: none;
  border: none;
  color: var(--color-text-primary);
  cursor: pointer;
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  padding: 4px 8px;
  transition: var(--transition-fast);
}

.nav-button:hover:not(:disabled) {
  color: var(--color-primary);
}

.nav-button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-button {
  width: 32px;
  height: 32px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-primary);
  border-radius: var(--radius-md);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: var(--font-sm);
}

.page-button:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.page-button.active {
  background: var(--color-primary);
  color: var(--color-btn-primary-text);
  border-color: var(--color-primary);
  font-weight: var(--font-bold);
}

</style>