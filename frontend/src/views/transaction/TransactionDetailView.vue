<script setup>
import { ref } from 'vue';
<<<<<<< HEAD
import { useRouter } from 'vue-router';
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

import PageHeader from '@/components/common/PageHeader.vue';
import TransactionFilterBottomSheet from '@/components/transaction/TransactionFilterBottomSheet.vue';
import TransactionDetailBottomSheet from '@/components/transaction/TransactionDetailBottomSheet.vue';
<<<<<<< HEAD
import Icon from '@/components/common/Icon.vue';

const router = useRouter();
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


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
<<<<<<< HEAD

  <div class="header">

    <PageHeader title="카드 이용내역" @back="router.back()" />
=======
<h1>테스트 화면</h1>

  <div class="header">

    <PageHeader title="카드 사용내역" />
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


    <button
      class="filter-button"
      @click="openFilter"
    >
<<<<<<< HEAD
      <Icon name="filter" size="sm" :color="'var(--color-text-primary)'" />
=======
      ⚙️
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

<<<<<<< HEAD
  padding: var(--space-md);
=======
  padding:20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.header {

  display:flex;

  justify-content:space-between;

  align-items:center;

}



.filter-button {

  border:none;

  background:none;

<<<<<<< HEAD
  font-size: var(--font-lg);

  cursor:pointer;

  color:var(--color-text-primary);

=======
  font-size:20px;

  cursor:pointer;

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}



.transaction-item {

<<<<<<< HEAD
  padding: var(--space-md) 0;

  border-bottom: 1px solid var(--color-border);
=======
  padding:16px 0;

  border-bottom:1px solid #eee;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  cursor:pointer;

}



.top {

  display:flex;

  justify-content:space-between;

<<<<<<< HEAD
  font-size: var(--font-sm);

  color: var(--color-text-secondary);
=======
  font-size:14px;

  color:#777;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.amount {

<<<<<<< HEAD
  font-weight: var(--font-semibold);

  color: var(--color-text-primary);
=======
  font-weight:600;

  color:#222;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.merchant {

<<<<<<< HEAD
  margin-top: var(--space-xs);

  font-size: var(--font-md);

  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
=======
  margin-top:8px;

  font-size:16px;

  font-weight:600;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

} 



.card-name {

<<<<<<< HEAD
  margin-top: var(--space-xxs);

  font-size: var(--font-xs);

  color: var(--color-text-tertiary);
=======
  margin-top:4px;

  font-size:13px;

  color:#888;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

</style>
