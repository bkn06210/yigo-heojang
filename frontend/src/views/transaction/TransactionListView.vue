<script setup>
import { ref } from 'vue';

import PageHeader from '@/components/common/PageHeader.vue';
import TransactionFilterBottomSheet from '@/components/transaction/TransactionFilterBottomSheet.vue';
import TransactionDatePickerBottomSheet from '@/components/transaction/TransactionDatePickerBottomSheet.vue';
import TransactionDetailBottomSheet from '@/components/transaction/TransactionDetailBottomSheet.vue';


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
    date: '2026.07.28',
    merchant: '스타벅스',
    cardName: 'KB My WE:SH 카드',
    amount: 6200,
  },
  {
    id: 2,
    date: '2026.07.27',
    merchant: 'CU',
    cardName: 'KB My WE:SH 카드',
    amount: 4500,
  },
  {
    id: 3,
    date: '2026.07.26',
    merchant: '올리브영',
    cardName: '신한 Deep Dream 카드',
    amount: 28900,
  },
]);


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

    <PageHeader title="카드 사용내역" />


    <button
      class="filter-icon"
      @click="openFilter"
    >
      ⚙️
    </button>

  </div>




  <!-- 사용내역 리스트 -->

  <div
  v-for="transaction in transactions"
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
