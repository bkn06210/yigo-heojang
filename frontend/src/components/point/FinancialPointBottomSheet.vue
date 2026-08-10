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


{{ point?.totalPoint?.toLocaleString() }}


</p>


</section>






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
width: 100%;
max-width: 480px;
left: 50%;
transform: translateX(-50%);
margin: 0 auto;

background: rgba(0,0,0,0.4);

display: flex;

justify-content: center;

align-items: flex-end;

z-index: 1000;

}



.bottom-sheet {

width: 100%;

max-width: 480px;

border-radius: var(--radius-xl) var(--radius-xl) 0 0;

padding: var(--space-xl);

max-height: 80vh;

overflow-y: auto;

/* 카드사용내역 상세 바텀시트와 동일하게 불투명 배경으로 통일 */
background: var(--color-surface);
box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.12);

box-sizing: border-box;

}

[data-theme="dark"] .bottom-sheet {

box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.35);

}



.sheet-header {

display: flex;

justify-content: space-between;

align-items: center;

margin-bottom: var(--space-xl);

}



.sheet-header h2 {

font-size: var(--font-xl);

font-weight: var(--font-bold);

letter-spacing: -0.2px;

color: var(--color-text-primary);

}



.close-button {

border: none;

background: none;

font-size: 24px;

color: var(--color-text-secondary);

cursor: pointer;

}



.point-summary {

border-radius: var(--radius-lg);

padding: var(--space-lg);

margin-bottom: var(--space-xl);

background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.1) 0%, rgba(var(--color-primary-dark-rgb), 0.03) 100%);
border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);
box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03), inset 0 1px 0 rgba(255, 255, 255, 0.35);
backdrop-filter: blur(8px);
-webkit-backdrop-filter: blur(8px);

}

[data-theme="dark"] .point-summary {

background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.18) 0%, rgba(var(--color-primary-dark-rgb), 0.06) 100%);
border: 1px solid rgba(var(--color-primary-dark-rgb), 0.28);
box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.05);

}



.label {

font-size: var(--font-sm);

color: var(--color-text-secondary);

}



.point {

margin-top: var(--space-xs);

font-size: var(--typo-display-large-size);
font-weight: var(--typo-display-large-weight);
line-height: var(--typo-display-large-line-height);
letter-spacing: var(--typo-display-large-letter-spacing);
color: var(--color-text-primary);

}



.info-section {

margin-top: var(--space-xl);

}



.info-section h3 {

font-size: var(--font-md);

font-weight: var(--font-semibold);

margin-bottom: var(--space-sm);

color: var(--color-text-primary);

}



ul {

list-style: none;

padding: 0;

margin: 0;

}



li {

padding: 12px 0;

border-bottom: 1px solid var(--color-border);

}



li:last-child {

border-bottom: none;

}



.info-section p {

font-size: 14px;

color: var(--color-text-secondary);

margin: 8px 0;

}



</style>