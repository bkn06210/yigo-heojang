<script setup>

defineProps({

  cards: {
    type: Array,
    default: () => []
  },

  selectedCard: {
    type: Object,
    default: null
  },

  selectedCategory: {
    type: String,
    default: null
  },

  selectedMerchant: {
    type: String,
    default: null
  },

  paymentAmount: {
    type: Number,
    default: 0
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

<!-- 결제 정보 헤더 -->
<div v-if="selectedCategory && selectedMerchant" class="result-header">
  <p class="result-context">
    <strong>{{ selectedCategory }}</strong>의 <strong>{{ selectedMerchant }}</strong>에서<br/>
    <strong>{{ paymentAmount.toLocaleString() }}원</strong>을 결제하실 때
  </p>
</div>

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

<p class="benefit-amount">
{{card.benefit.toLocaleString()}}원
</p>

<p v-if="card.reasons?.length" class="benefit-reason">
{{card.reasons[0]}}
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

.result-container h2{

margin: 0 0 var(--space-md);
font-size: var(--font-xl);
font-weight: var(--font-bold);
letter-spacing: -0.2px;
color: var(--color-text-primary);

}

.result-header {
  background: linear-gradient(135deg, rgba(200, 220, 240, 0.4) 0%, rgba(220, 240, 255, 0.2) 100%);
  border: 1px solid rgba(100, 150, 200, 0.15);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.result-context {
  margin: 0;
  font-size: var(--font-sm);
  color: var(--color-text-primary);
  line-height: 1.8;
}

.result-context strong {
  color: var(--color-text-primary);
  font-weight: var(--font-bold);
}

[data-theme="dark"] .result-header {
  background: linear-gradient(135deg, rgba(100, 120, 150, 0.2) 0%, rgba(80, 100, 140, 0.15) 100%);
  border: 1px solid rgba(150, 170, 200, 0.15);
}


.result-card{

display:flex;
gap:var(--space-md);

padding:var(--space-lg);

border:1px solid var(--color-border);
border-radius:var(--radius-lg);

margin-bottom:var(--space-md);

background:var(--color-surface);
box-shadow:var(--shadow-card);
cursor:pointer;
transition:var(--transition-fast);

align-items:center;

overflow:hidden;
word-break:break-word;

box-sizing:border-box;

}

.result-card:hover{

transform:translateY(-2px);

}


.result-card.selected{

border:2px solid var(--color-primary);

}


.rank{

font-size: var(--font-xs);
font-weight: var(--font-bold);
color: var(--color-primary-dark);

}

.info{

flex: 1;
min-width: 0;

}

.info h3{

margin: 0 0 var(--space-xxs);
font-size: var(--font-md);
font-weight: var(--font-bold);
color: var(--color-text-primary);

}

.info p{

margin: 0 0 var(--space-sm);
font-size: var(--font-sm);
color: var(--color-text-secondary);

}

.benefit-amount{

font-size: var(--font-md);
font-weight: var(--font-bold);
color: var(--color-primary-dark);

}

.benefit-reason{

font-size: var(--font-xs);
color: var(--color-text-tertiary);

}

.detail-button{

width:auto;

height:32px;

padding:0 12px;

border:1px solid var(--color-border);

border-radius:8px;

background:var(--color-surface);

color:var(--color-text-primary);

font-size:13px;

font-weight:var(--font-semibold);

cursor:pointer;

transition:var(--transition-fast);

}

.detail-button:hover{

background:var(--color-border);

}

</style>