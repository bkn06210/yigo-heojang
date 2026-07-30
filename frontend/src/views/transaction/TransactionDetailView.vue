<script setup>
import { ref } from 'vue';

import PageHeader from '@/components/common/PageHeader.vue';
import TransactionFilterBottomSheet from '@/components/transaction/TransactionFilterBottomSheet.vue';
import TransactionDetailBottomSheet from '@/components/transaction/TransactionDetailBottomSheet.vue';


// 조회조건 표시 여부
const showFilter = ref(false);


// 임시 소비내역 데이터
// 추후 API 연결
const transactions = ref([
  {
    id: 1,
    date: '07.28',
    merchant: '스타벅스',
    amount: 6200,
    cardName: '신한 Deep Dream 카드',
  },
  {
    id: 2,
    date: '07.27',
    merchant: 'CU',
    amount: 4500,
    cardName: '신한 Deep Dream 카드',
  },
  {
    id: 3,
    date: '07.26',
    merchant: '올리브영',
    amount: 28900,
    cardName: 'KB My WE:SH 카드',
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


// 조회조건 적용
const applyFilter = (filter) => {

  console.log('적용 조건:', filter);

  showFilter.value = false;

};



// 선택한 사용내역
const selectedTransaction = ref(null);


// 상세 열기
const openDetail = (transaction) => {

  console.log('클릭됨:', transaction);

  selectedTransaction.value = transaction;

};


// 상세 닫기
const closeDetail = () => {

  selectedTransaction.value = null;

};

</script>


<template>

<div class="transaction-detail-page">
<h1>테스트 화면</h1>

  <div class="header">

    <PageHeader title="카드 사용내역" />


    <button
      class="filter-button"
      @click="openFilter"
    >
      ⚙️
    </button>


  </div>



  <section class="transaction-list">


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


  </section>



  <TransactionFilterBottomSheet

    v-if="showFilter"

    @close="closeFilter"

    @apply="applyFilter"

  />



  <TransactionDetailBottomSheet

    v-if="selectedTransaction"

    :transaction="selectedTransaction"

    @close="closeDetail"

  />


</div>

</template>



<style scoped>

.transaction-detail-page {

  padding:20px;

}



.header {

  display:flex;

  justify-content:space-between;

  align-items:center;

}



.filter-button {

  border:none;

  background:none;

  font-size:20px;

  cursor:pointer;

}



.transaction-item {

  padding:16px 0;

  border-bottom:1px solid #eee;

  cursor:pointer;

}



.top {

  display:flex;

  justify-content:space-between;

  font-size:14px;

  color:#777;

}



.amount {

  font-weight:600;

  color:#222;

}



.merchant {

  margin-top:8px;

  font-size:16px;

  font-weight:600;

}



.card-name {

  margin-top:4px;

  font-size:13px;

  color:#888;

}

</style>