<script setup>
<<<<<<< HEAD
import { ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
=======
import { computed, onMounted, ref } from 'vue';
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

import PageHeader from '@/components/common/PageHeader.vue';
import TransactionFilterBottomSheet from '@/components/transaction/TransactionFilterBottomSheet.vue';
import TransactionDatePickerBottomSheet from '@/components/transaction/TransactionDatePickerBottomSheet.vue';
import TransactionDetailBottomSheet from '@/components/transaction/TransactionDetailBottomSheet.vue';
<<<<<<< HEAD
import Icon from '@/components/common/Icon.vue';


const route = useRoute();
const router = useRouter();

// 카드 상세에서 들어온 경우에만 존재 — 있으면 해당 카드 소비내역만, 없으면 전체
const filterCardId = computed(() =>
  route.query.cardId ? Number(route.query.cardId) : null,
);

const filterCardName = computed(() => route.query.cardName || '');

=======
import {
  getExpenseCategories,
  getTransaction,
  getTransactions,
  syncTransactions,
} from '@/api/walletApi';


>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

<<<<<<< HEAD

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
    amount: 8500,
    category: '음식/카페',
    status: '승인',
    cardName: 'KB My WE:SH 카드',
    cardLastDigits: '1234',
    cardType: '신용카드',
    installment: '일시불',
    approvalNumber: '12345678',
  },
  {
    id: 2,
    cardId: 1,
    date: '2026.08.03 18:45',
    merchant: 'GS25',
    amount: 12400,
    category: '편의점',
    status: '승인',
    cardName: 'KB My WE:SH 카드',
    cardLastDigits: '1234',
    cardType: '신용카드',
    installment: '일시불',
    approvalNumber: '87654321',
  },
  {
    id: 3,
    cardId: 1,
    date: '2026.08.02 14:20',
    merchant: '넥슨 게임샵',
    amount: 29000,
    category: '게임/엔터',
    status: '승인',
    cardName: 'KB My WE:SH 카드',
    cardLastDigits: '1234',
    cardType: '신용카드',
    installment: '일시불',
    approvalNumber: '11223344',
  },
  {
    id: 4,
    cardId: 2,
    date: '2026.08.01 16:15',
    merchant: '컬리마켓',
    amount: 45800,
    category: '쇼핑',
    status: '승인',
    cardName: '신한 Deep Dream 카드',
    cardLastDigits: '5678',
    cardType: '체크카드',
    installment: '일시불',
    approvalNumber: '55667788',
  },
  {
    id: 5,
    cardId: 2,
    date: '2026.07.31 10:50',
    merchant: '로또판매점',
    amount: 5000,
    category: '기타',
    status: '승인',
    cardName: '신한 Deep Dream 카드',
    cardLastDigits: '5678',
    cardType: '체크카드',
    installment: '일시불',
    approvalNumber: '99887766',
  },
]);

// 카드 상세에서 진입한 경우 해당 카드 소비내역만, 홈에서 진입한 경우 전체
const filteredTransactions = computed(() => {
  if (!filterCardId.value) return transactions.value;

  return transactions.value.filter(
    (transaction) => transaction.cardId === filterCardId.value,
  );
});

=======
const loading = ref(false);
const detailLoading = ref(false);
const errorMessage = ref('');
const activeFilter = ref(null);
const expenseCategories = ref([]);

const formatDate = (value) => value ? value.slice(0, 10).replaceAll('-', '.') : '-';
const toDateKey = (value) => (value || '').slice(0, 10).replaceAll('.', '-');
const mapTransaction = (item) => ({
  ...item,
  id: item.expenseId,
  date: formatDate(item.paymentDate),
  merchant: item.merchantName || '가맹점 정보 없음',
  cardName: item.cardName || '카드 정보 없음',
  amount: Number(item.paymentAmount || 0),
});

const cardTypeLabels = {
  CREDIT: '신용카드',
  CHECK: '체크카드',
  PREPAID: '선불카드',
  GIFT: '기프트카드',
};

const cardTypeCodes = {
  신용카드: 'CREDIT',
  체크카드: 'CHECK',
  선불카드: 'PREPAID',
  기프트카드: 'GIFT',
};

const transactionTypeCodes = {
  일시불: 'LUMP_SUM',
  할부: 'INSTALLMENT',
  단기카드대출: 'CASH_ADVANCE',
};


// 카드 목록
const cards = computed(() => {
  const unique = new Map();
  transactions.value.forEach((item) => {
    if (!unique.has(item.userCardId)) {
      unique.set(item.userCardId, {
        id: item.userCardId,
        name: item.cardName,
        type: cardTypeLabels[item.cardType] || '카드',
        imageUrl: '',
      });
    }
  });
  return [...unique.values()];
});


// 사용내역 임시 데이터
const transactions = ref([]);

const visibleTransactions = computed(() => {
  const filter = activeFilter.value;
  if (!filter) return transactions.value;
  return transactions.value.filter((item) => {
    if (filter.card !== '전체' && item.cardName !== filter.card) return false;
    if (filter.transactionType === '취소' && item.paymentStatus !== 'CANCELED') return false;
    const paymentDate = toDateKey(item.paymentDate);
    const startDate = toDateKey(filter.startDate);
    const endDate = toDateKey(filter.endDate);
    if (startDate && paymentDate < startDate) return false;
    if (endDate && paymentDate > endDate) return false;
    return true;
  });
});

const loadTransactions = async (params = {}) => {
  loading.value = true;
  errorMessage.value = '';
  try {
    const response = await getTransactions(params);
    transactions.value = (response?.transactions || response || []).map(mapTransaction);
  } catch (error) {
    errorMessage.value = error?.message || '카드 사용내역을 불러오지 못했습니다.';
  } finally {
    loading.value = false;
  }
};

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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
<<<<<<< HEAD
=======
  showFilter.value = true;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

};


// 조회조건 적용
<<<<<<< HEAD
const applyFilter = (filter) => {

  console.log(
    '조회조건:',
    filter
  );

  showFilter.value = false;
=======
const applyFilter = async (filter) => {
  activeFilter.value = { ...filter };
  showFilter.value = false;
  const params = {
    categoryId: filter.categoryId || undefined,
    userCardId: filter.userCardId || undefined,
    paymentStatus: filter.transactionType === '취소' ? 'CANCELED' : undefined,
    approvalStatus: filter.approval === '결제확정'
      ? 'CONFIRMED'
      : filter.approval === '승인'
        ? 'APPROVED'
        : undefined,
    cardType: cardTypeCodes[filter.cardType] || undefined,
    region: filter.region === '국내' ? 'DOMESTIC' : filter.region === '해외' ? 'OVERSEAS' : undefined,
    transactionType: transactionTypeCodes[filter.transactionType] || undefined,
    startDate: toDateKey(filter.startDate) || undefined,
    endDate: toDateKey(filter.endDate) || undefined,
  };
  await loadTransactions(params);
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

};


// 사용내역 상세 열기
<<<<<<< HEAD
const openDetail = (transaction) => {

  selectedTransaction.value = transaction;

  showDetail.value = true;

=======
const openDetail = async (transaction) => {
  selectedTransaction.value = transaction;
  showDetail.value = true;
  detailLoading.value = true;
  try {
    selectedTransaction.value = mapTransaction(await getTransaction(transaction.id));
  } catch (error) {
    errorMessage.value = error?.message || '사용내역 상세 정보를 불러오지 못했습니다.';
  } finally {
    detailLoading.value = false;
  }
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
};


// 사용내역 상세 닫기
const closeDetail = () => {

  showDetail.value = false;

  selectedTransaction.value = null;

};
<<<<<<< HEAD
=======

const initializeTransactions = async () => {
  try {
    const categoryData = await getExpenseCategories();
    expenseCategories.value = categoryData?.categories || [];
  } catch (error) {
    console.error('소비 카테고리 조회 실패:', error);
  }

  try {
    await syncTransactions();
  } catch (error) {
    console.error('소비내역 동기화 실패:', error);
  }

  await loadTransactions();
};

onMounted(initializeTransactions);
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

</script>


<template>

<div class="transaction-page">


  <div class="header">

<<<<<<< HEAD
    <PageHeader
      :title="filterCardName ? `${filterCardName} 이용내역` : '카드이용내역'"
      @back="router.back()"
    />
=======
    <PageHeader title="카드 사용내역" />
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


    <button
      class="filter-icon"
      @click="openFilter"
    >
<<<<<<< HEAD
      <Icon name="filter" size="sm" />
=======
      ⚙️
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
    </button>

  </div>




  <!-- 사용내역 리스트 -->

<<<<<<< HEAD
  <div
  v-for="transaction in filteredTransactions"
=======
  <p v-if="errorMessage" class="transaction-message">
    {{ errorMessage }}
  </p>

  <p
    v-else-if="!loading && visibleTransactions.length === 0"
    class="transaction-message"
  >
    선택한 조건에 해당하는 카드 사용내역이 없습니다.
  </p>

  <div
  v-for="transaction in visibleTransactions"
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  :key="transaction.id"
  class="transaction-item"
  @click="openDetail(transaction)"
>

<<<<<<< HEAD
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
=======

    <div class="top">

      <span>
        {{ transaction.date }}
      </span>


      <span class="amount">
        -{{ transaction.amount.toLocaleString() }}원
      </span>

    </div>



    <div class="merchant">

      {{ transaction.merchant }}

    </div>



    <div class="card-name">

      {{ transaction.cardName }}

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
    </div>


  </div>






  <!-- 조회조건 -->

  <TransactionFilterBottomSheet

    v-if="showFilter"

    :cards="cards"

<<<<<<< HEAD
=======
    :categories="expenseCategories"

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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
<<<<<<< HEAD
  padding: var(--space-md);
  margin: 0 auto;
  max-width: 480px;
  box-sizing: border-box;
  overflow: hidden visible;
=======
  padding:20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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
<<<<<<< HEAD
font-size: var(--font-lg);
cursor: pointer;
color: var(--color-text-primary);
=======
font-size:20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.transaction-item {
<<<<<<< HEAD
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
=======

padding:16px 0;

border-bottom:1px solid #eee;

cursor:pointer;

}

.transaction-message {
  padding: 40px 0;
  text-align: center;
  color: #777;
}



.top {

display:flex;

justify-content:space-between;

}



.amount {

color:#e53935;

font-weight:600; 

}



.merchant {

font-size:16px;

font-weight:600;

}



.card-name {

margin-top:4px;

color:#888;

font-size:14px;

}

</style>
<!-- 07_25 연동 변경: 소비내역 목록과 조회조건을 실제 백엔드 API에 연결한다. -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
