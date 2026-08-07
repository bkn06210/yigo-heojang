<script setup>
import { computed, ref } from 'vue';

import CardItem from '@/components/card/CardItem.vue';


const props = defineProps({
  company: {
    type: Object,
    required: true,
  },
});


const emit = defineEmits([
  'toggle-pin',
]);


// 처음 보여줄 카드 개수
const limit = 2;


// 더보기 상태
const isExpanded = ref(false);


// 표시할 카드
const visibleCards = computed(() => {

  if (isExpanded.value) {
    return props.company.cards;
  }


  return props.company.cards.slice(0, limit);

});


// 핀 이벤트 부모 전달
const togglePin = (id) => {

  emit(
    'toggle-pin',
    id,
  );

};

</script>



<template>

  <section class="company-group">


    <!-- 카드사명 -->
    <div class="company-header">

      <h3>
        {{ company.name }}
      </h3>


<<<<<<< HEAD
      <span class="card-count">
=======
      <span>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
        {{ company.cards.length }}장
      </span>

    </div>



<<<<<<< HEAD
    <!-- 카드 목록 (2열 그리드) -->
    <div class="cards-grid">

      <CardItem
        v-for="card in visibleCards"
        :key="card.id"
        :card="card"
        @toggle-pin="togglePin"
      />

    </div>
=======
    <!-- 카드 목록 -->
    <CardItem
      v-for="card in visibleCards"
      :key="card.id"
      :card="card"
      @toggle-pin="togglePin"
    />
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e



    <!-- 더보기 -->
    <button
      v-if="company.cards.length > limit"
      class="more-button"
      @click="isExpanded = !isExpanded"
    >

      {{
        isExpanded
<<<<<<< HEAD
          ? '접기 ∧'
=======
          ? '접기'
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
          : `+ ${company.cards.length - limit}개 더보기`
      }}

    </button>


  </section>

</template>



<style scoped>

.company-group {

<<<<<<< HEAD
  margin-bottom: var(--space-2xl);
=======
  margin-bottom:32px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.company-header {

<<<<<<< HEAD
  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: var(--space-md);

  padding: 0 var(--space-xs);
=======
  display:flex;

  justify-content:space-between;

  align-items:center;

  margin-bottom:12px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.company-header h3 {

<<<<<<< HEAD
  margin: 0;

  font-size: var(--font-md);

  font-weight: var(--font-semibold);

  color: var(--color-text-primary);

  letter-spacing: -0.2px;
=======
  font-size:18px;

  font-weight:700;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



<<<<<<< HEAD
.card-count {

  color: var(--color-text-tertiary);

  font-size: var(--font-xs);

  font-weight: var(--font-medium);
=======
.company-header span {

  color:#888;

  font-size:14px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


<<<<<<< HEAD
.cards-grid {

  display: flex;

  flex-direction: column;

  gap: var(--space-md);

  margin-bottom: var(--space-md);

}


.more-button {

  width: 100%;

  padding: var(--space-sm) 0;

  background: none;

  border: none;

  color: var(--color-text-secondary);

  cursor: pointer;

  font-size: var(--font-sm);

  font-weight: var(--font-medium);

  transition: var(--transition-fast);

}

.more-button:hover {

  color: var(--color-text-primary);

}

</style>
=======

.more-button {

  width:100%;

  padding:12px;

  border:none;

  background:none;

  color:#666;

  cursor:pointer;

  font-size:14px;

}


</style>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
