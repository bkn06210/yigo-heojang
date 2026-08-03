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


      <span>
        {{ company.cards.length }}장
      </span>

    </div>



    <!-- 카드 목록 -->
    <CardItem
      v-for="card in visibleCards"
      :key="card.id"
      :card="card"
      @toggle-pin="togglePin"
    />



    <!-- 더보기 -->
    <button
      v-if="company.cards.length > limit"
      class="more-button"
      @click="isExpanded = !isExpanded"
    >

      {{
        isExpanded
          ? '접기'
          : `+ ${company.cards.length - limit}개 더보기`
      }}

    </button>


  </section>

</template>



<style scoped>

.company-group {

  margin-bottom:32px;

}



.company-header {

  display:flex;

  justify-content:space-between;

  align-items:center;

  margin-bottom:12px;

}



.company-header h3 {

  font-size:18px;

  font-weight:700;

}



.company-header span {

  color:#888;

  font-size:14px;

}



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