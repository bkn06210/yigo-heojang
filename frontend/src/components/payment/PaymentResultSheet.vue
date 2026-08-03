<script setup>

defineProps({

  cards: {
    type: Array,
    default: () => []
  },

  selectedCard: {
    type: Object,
    default: null
  }

});


const emit = defineEmits([
  'select',
  'detail'
]);


const selectCard = (card)=>{

  emit(
    'select',
    card
  );

};

const openDetail = (card) => {

  emit(
    'detail',
    card
  );

};


</script>


<template>

<div class="result-container">


<h2>
추천 결과
</h2>


<div
  v-for="(card,index) in cards"
  :key="card.id"

  class="result-card"

  :class="{
    selected:selectedCard?.id === card.id
  }"

  @click="selectCard(card)"
>


<div class="rank">

추천 {{index + 1}}

</div>


<div class="info">

<h3>
{{card.name}}
</h3>


<p>
{{card.benefit}}
</p>

<button
  class="detail-button"
  @click.stop="openDetail(card)"
>
자세히 보기
</button>

</div>


</div>



</div>

</template>



<style scoped>


.result-container{

padding:20px;

}


.result-card{

display:flex;
gap:12px;

padding:16px;

border:1px solid #ddd;
border-radius:16px;

margin-bottom:12px;

}


.result-card.selected{

border:2px solid #333;

}


.rank{

font-size:14px;

}


</style>