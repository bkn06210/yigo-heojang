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


      <span class="card-count">
        {{ company.cards.length }}장
      </span>

    </div>



    <!-- 카드 목록 (2열 그리드) -->
    <div class="cards-grid">

      <CardItem
        v-for="card in visibleCards"
        :key="card.id"
        :card="card"
        @toggle-pin="togglePin"
      />

    </div>



    <!-- 더보기 -->
    <button
      v-if="company.cards.length > limit"
      class="more-button"
      @click="isExpanded = !isExpanded"
    >

      {{
        isExpanded
          ? '접기 ∧'
          : `+ ${company.cards.length - limit}개 더보기`
      }}

    </button>


  </section>

</template>



<style scoped>

.company-group {

  margin-bottom: var(--space-2xl);

}



.company-header {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: var(--space-md);

  padding: 0 var(--space-xs);

}



.company-header h3 {

  margin: 0;

  font-size: var(--font-md);

  font-weight: var(--font-semibold);

  color: var(--color-text-primary);

  letter-spacing: -0.2px;

}



.card-count {

  color: var(--color-text-tertiary);

  font-size: var(--font-xs);

  font-weight: var(--font-medium);

}


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
