<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import TransactionFilterBottomSheet from '@/components/transaction/TransactionFilterBottomSheet.vue';
import TransactionDetailBottomSheet from '@/components/transaction/TransactionDetailBottomSheet.vue';
import Icon from '@/components/common/Icon.vue';

const router = useRouter();


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

  <div class="header">

    <PageHeader title="카드 이용내역" @back="router.back()" />


    <button
      class="filter-button"
      @click="openFilter"
    >
      <Icon name="filter" size="sm" :color="'var(--color-text-primary)'" />
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

  padding: var(--space-md);

}



.header {

  display:flex;

  justify-content:space-between;

  align-items:center;

}



.filter-button {

  border:none;

  background:none;

  font-size: var(--font-lg);

  cursor:pointer;

  color:var(--color-text-primary);

}



.transaction-item {

  padding: var(--space-md) 0;

  border-bottom: 1px solid var(--color-border);

  cursor:pointer;

}



.top {

  display:flex;

  justify-content:space-between;

  font-size: var(--font-sm);

  color: var(--color-text-secondary);

}



.amount {

  font-weight: var(--font-semibold);

  color: var(--color-text-primary);

}



.merchant {

  margin-top: var(--space-xs);

  font-size: var(--font-md);

  font-weight: var(--font-semibold);
  color: var(--color-text-primary);

} 



.card-name {

  margin-top: var(--space-xxs);

  font-size: var(--font-xs);

  color: var(--color-text-tertiary);

}

</style>
