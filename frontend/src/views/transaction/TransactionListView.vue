<script setup>
import { computed, onMounted, ref } from 'vue';

import PageHeader from '@/components/common/PageHeader.vue';
import TransactionFilterBottomSheet from '@/components/transaction/TransactionFilterBottomSheet.vue';
import TransactionDatePickerBottomSheet from '@/components/transaction/TransactionDatePickerBottomSheet.vue';
import TransactionDetailBottomSheet from '@/components/transaction/TransactionDetailBottomSheet.vue';
import {
  getExpenseCategories,
  getTransaction,
  getTransactions,
  syncTransactions,
} from '@/api/walletApi';


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
  showFilter.value = true;

};


// 조회조건 적용
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

};


// 사용내역 상세 열기
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
};


// 사용내역 상세 닫기
const closeDetail = () => {

  showDetail.value = false;

  selectedTransaction.value = null;

};

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

</script>


<template>

<div class="transaction-page">


  <div class="header">

    <PageHeader title="카드 사용내역" />


    <button
      class="filter-icon"
      @click="openFilter"
    >
      ⚙️
    </button>

  </div>




  <!-- 사용내역 리스트 -->

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
  :key="transaction.id"
  class="transaction-item"
  @click="openDetail(transaction)"
>


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

    </div>


  </div>






  <!-- 조회조건 -->

  <TransactionFilterBottomSheet

    v-if="showFilter"

    :cards="cards"

    :categories="expenseCategories"

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
  padding:20px;
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
font-size:20px;

}



.transaction-item {

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
