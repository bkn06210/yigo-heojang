<<<<<<< HEAD
﻿<script setup>
=======
<script setup>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
const props = defineProps({

  // 바텀시트 표시 여부
  visible: {

    type: Boolean,

    default: false

  },

  // 선택한 카드
  card: {

    type: Object,

    default: null

  }

});


const emit = defineEmits([

  'close',

  'select'

]);


// 닫기
const closeSheet = () => {

  emit('close');

};


// 카드 선택
const selectCard = () => {

  emit('select', props.card);

};
</script>

<template>

  <div
    v-if="visible"
    class="overlay"
    @click.self="closeSheet"
  >

    <div class="sheet">


      <!-- 위쪽 손잡이 -->
      <div class="handle"></div>


      <h2>
        추천 결과
      </h2>



      <!-- 카드 정보 -->
      <section
        v-if="card"
        class="card-info"
      >

        <img
<<<<<<< HEAD
=======
          v-if="card.image"
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
          :src="card.image"
          :alt="card.name"
          class="card-image"
        />


        <div class="card-text">

          <h3>
            {{ card.name }}
          </h3>


          <p>
            예상 혜택

            <strong>
              {{ card.benefit.toLocaleString() }}원
            </strong>
          </p>

        </div>

      </section>



      <!-- 추천 이유 -->
      <section class="section">

        <h4>
          추천 이유
        </h4>


        <ul>

          <li
            v-for="reason in card?.reasons"
            :key="reason"
          >

            {{ reason }}

          </li>

        </ul>

      </section>



      <!-- 멤버십 혜택 -->
      <section
  v-if="card?.membershipBenefit"
  class="section"
>

<h4>
멤버십 혜택
</h4>

<p>
{{ card.membershipBenefit.message }}
</p>

</section>


      <!-- 선택 버튼 -->
      <button
        class="select-button"
        @click="selectCard"
      >

        이 카드 선택하기

      </button>


    </div>

  </div>

</template>



<style scoped>

.overlay{

  position:fixed;

  inset:0;

  background:rgba(0,0,0,.4);

  display:flex;

  justify-content:center;

  align-items:flex-end;

  z-index:9999;

}

.sheet{

  width:100%;

  max-width:430px;

<<<<<<< HEAD
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;

  padding: var(--space-xl);

  background: linear-gradient(180deg, rgba(255, 255, 255, 0.6) 0%, var(--color-surface) 30%);
  border-top: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);

}

[data-theme="dark"] .sheet{

  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06) 0%, var(--color-surface) 30%);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.35);

}

.sheet h2{

  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  letter-spacing: var(--typo-display-medium-letter-spacing);
  color: var(--color-text-primary);
  margin: 0;
=======
  background:#fff;

  border-radius:24px 24px 0 0;

  padding:24px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.handle{

  width:48px;

  height:5px;

  border-radius:999px;

<<<<<<< HEAD
  background:var(--color-border);

  margin:0 auto var(--space-lg);
=======
  background:#ddd;

  margin:0 auto 20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.card-info{

  display:flex;

<<<<<<< HEAD
  gap: var(--space-md);

  align-items: center;

  margin: var(--space-xl) 0;

  padding: var(--space-md);

  border-radius: var(--radius-md);

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.1) 0%, rgba(var(--color-primary-dark-rgb), 0.03) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);

}

[data-theme="dark"] .card-info{

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.18) 0%, rgba(var(--color-primary-dark-rgb), 0.06) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.28);

}

.card-info h3{

  margin: 0 0 var(--space-xs);
  font-size: var(--font-md);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);

}

.card-info p{

  margin: 0;
  font-size: var(--font-sm);
  color: var(--color-text-secondary);

}

.card-info strong{

  color: var(--color-primary-dark);
  font-weight: var(--font-bold);
=======
  gap:16px;

  margin:24px 0;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.card-image{

  width:72px;

<<<<<<< HEAD
  border-radius: var(--radius-sm);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.section{

<<<<<<< HEAD
  margin-bottom: var(--space-xl);

}

.section h4{

  margin: 0;
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
=======
  margin-bottom:24px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.section ul{

<<<<<<< HEAD
  margin-top: var(--space-sm);

  padding-left: var(--space-lg);
=======
  margin-top:12px;

  padding-left:20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.section li{

<<<<<<< HEAD
  margin-bottom: var(--space-xs);

  font-size: var(--font-sm);
  color: var(--color-text-secondary);

}

.section p{

  margin-top: var(--space-sm);
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
=======
  margin-bottom:8px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

.select-button{

  width:100%;

  height:48px;

  border:none;

  border-radius:12px;

<<<<<<< HEAD
  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );

  color:var(--color-btn-primary-text);

  font-weight:var(--font-semibold);

  cursor:pointer;

  transition:var(--transition-fast);

}

.select-button:hover{

  opacity:0.9;

}

</style>
=======
}

</style>
<!-- 07_25 연동 변경: 추천 API의 상세 계산 결과를 바텀시트에 표시한다. -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
