<script setup>

// 금융 포인트 상세 바텀시트
//
// 기존 구조:
// 혜택 페이지
//   ↓
// /points/financial/:id
//   ↓
// FinancialPointDetailView.vue (페이지)
//
// 변경 구조:
// 혜택 페이지 내부에서
// FinancialPointBottomSheet.vue 표시
//
// API 연동 시:
// 기존 금융 포인트 상세 조회 API 응답 데이터를
// props로 전달받아 표시 예정
//
// 페이지인지 바텀시트인지는 프론트 화면 표현 방식 차이
// 백엔드 API 구조는 그대로 재사용 가능


import AIBriefingCard from '@/components/common/AIBriefingCard.vue'



const props = defineProps({

  point: {

    type: Object,

    required: true

  }

})



const emit = defineEmits([

  'close'

])



// 바텀시트 닫기

const closeSheet = () => {

  emit('close')

}



</script>




<template>


<!-- 배경 클릭 시 닫기 -->

<div

  class="overlay"

  @click.self="closeSheet"

>


<div class="bottom-sheet">



<!-- 헤더 -->

<div class="sheet-header">


<h2>

{{ point.name }}

</h2>


<button

  class="close-button"

  @click="closeSheet"

>

×


</button>


</div>





<!-- 총 보유 포인트 -->

<section class="point-summary">


<p class="label">

총 보유 포인트

</p>


<p class="point">


{{ point.point.toLocaleString() }}P


</p>


</section>





<!-- AI 브리핑 -->

<AIBriefingCard

  :isLogin="true"

  :message="point.aiMessage"

/>





<!-- 포인트 활용 방법 -->


<section class="info-section">


<h3>

포인트 활용 방법

</h3>



<ul>


<li

v-for="method in point.usageMethods"

:key="method"

>

{{ method }}

</li>


</ul>



</section>






<section class="info-section">
  <h3>포인트 이용내역</h3>
  <ul v-if="point.histories?.length">
    <li v-for="history in point.histories" :key="history.pointHistoryId">
      {{ history.content || history.providerName }}
      {{ history.pointType === 'USE' ? '-' : '+' }}{{ Number(history.pointAmount || 0).toLocaleString() }}P
    </li>
  </ul>
  <p v-else>포인트 이용내역이 없습니다.</p>
</section>

<!-- 포인트 안내 -->


<section class="info-section">


<h3>

포인트 안내

</h3>


<p>

1P = 1원

</p>


<p>

사용 조건은 카드사 정책에 따라 다를 수 있습니다.

</p>


</section>





</div>


</div>


</template>





<style scoped>


.overlay {

position: fixed;

inset: 0;

background: rgba(0,0,0,0.4);

display: flex;

align-items: flex-end;

z-index: 1000;

}



.bottom-sheet {

width: 100%;

background: white;

border-radius: 24px 24px 0 0;

padding: 24px;

max-height: 80vh;

overflow-y: auto;

}



.sheet-header {

display: flex;

justify-content: space-between;

align-items: center;

margin-bottom: 20px;

}



.sheet-header h2 {

font-size: 20px;

font-weight: 700;

}



.close-button {

border: none;

background: none;

font-size: 24px;

cursor: pointer;

}



.point-summary {

background: #f8f9fa;

border-radius: 16px;

padding: 20px;

margin-bottom: 20px;

}



.label {

font-size: 14px;

color: #666;

}



.point {

margin-top: 8px;

font-size: 30px;

font-weight: 700;

}



.info-section {

background: white;

margin-top: 20px;

}



.info-section h3 {

font-size: 16px;

margin-bottom: 12px;

}



ul {

list-style: none;

padding: 0;

margin: 0;

}



li {

padding: 12px 0;

border-bottom: 1px solid #eee;

}



li:last-child {

border-bottom: none;

}



.info-section p {

font-size: 14px;

color: #666;

margin: 8px 0;

}



</style>
<!-- 07_25 연동 변경: 금융포인트 상세 API 응답을 바텀시트에 표시한다. -->
