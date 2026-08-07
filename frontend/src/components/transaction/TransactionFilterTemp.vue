<script setup>
import { ref } from 'vue';

import CardTypeBottomSheet from './CardTypeBottomSheet.vue';
import CardSelectBottomSheet from './CardSelectBottomSheet.vue';


const emit = defineEmits([
  'close',
  'apply',
]);


// 조회 조건 데이터
const filter = ref({

  approval: '승인',

  cardType: '전체',

  card: '전체',

  region: '전체',

  transactionType: '전체',

  period: '이번달',

  startDate: '2026.06.29',

  endDate: '2026.07.29',

});


// 하위 바텀시트 상태

const showCardType = ref(false);

const showCardSelect = ref(false);



// 카드 구분 선택창 열기
const openCardType = () => {

  showCardType.value = true;

};



// 카드 선택창 열기
const openCardSelect = () => {

  showCardSelect.value = true;

};



// 카드 구분 선택 완료
const selectCardType = (type) => {

  filter.value.cardType = type;

  showCardType.value = false;

};



// 카드 선택 완료
const selectCard = (card) => {

  filter.value.card = card;

  showCardSelect.value = false;

};



// 적용
const apply = () => {

  emit(
    'apply',
    filter.value,
  );

};



// 닫기
const close = () => {

  emit('close');

};

</script>



<template>

  <div
    class="overlay"
    @click.self="close"
  >

    <section class="filter-sheet">


      <div class="handle"></div>


      <div class="title-area">

        <h2>
          조회조건 선택
        </h2>

        <button
          @click="close"
        >
          ✕
        </button>

      </div>

      <!-- 승인구분 -->

      <div class="filter-item">

        <h3>
          승인구분
        </h3>


        <div class="chips">

          <button
            :class="{
              active: filter.approval === '승인'
            }"

            @click="filter.approval='승인'"
          >
            승인
          </button>


          <button
            :class="{
              active: filter.approval === '결제확정'
            }"

            @click="filter.approval='결제확정'"
          >
            결제확정
          </button>

        </div>


      </div>

      <!-- 카드구분 -->

      <div class="filter-item">

        <h3>
          카드구분
        </h3>


        <button
          class="select-button"
          @click="openCardType"
        >

          <span>
            {{ filter.cardType }}
          </span>


          <span>
            〉
          </span>


        </button>


      </div>

      <!-- 카드 선택 -->

      <div class="filter-item">

        <h3>
          카드 선택
        </h3>


        <button
          class="select-button"
          @click="openCardSelect"
        >

          <span>
            {{ filter.card }}
          </span>


          <span>
            〉
          </span>


        </button>


      </div>

      <!-- 지역 -->

      <div class="filter-item">

        <h3>
          지역
        </h3>


        <div class="chips">

          <button
            v-for="item in [
              '전체',
              '국내',
              '해외'
            ]"

            :key="item"

            :class="{
              active: filter.region === item
            }"

            @click="filter.region=item"
          >

            {{ item }}

          </button>


        </div>


      </div>

      <!-- 거래구분 -->

      <div class="filter-item">

        <h3>
          거래구분
        </h3>


        <div class="chips wrap">

          <button
            v-for="item in [
              '전체',
              '일시불',
              '할부',
              '단기카드대출',
              '취소'
            ]"

            :key="item"

            :class="{
              active: filter.transactionType === item
            }"

            @click="
              filter.transactionType=item
            "
          >

            {{ item }}

          </button>


        </div>


      </div>

      <!-- 조회기간 -->

      <div class="filter-item">


        <h3>
          조회기간 (최대 6개월)
        </h3>


        <div class="chips wrap">


          <button
            v-for="item in [
              '이번달',
              '1개월',
              '3개월',
              '월별 선택',
              '직접 선택'
            ]"

            :key="item"

            :class="{
              active: filter.period === item
            }"

            @click="
              filter.period=item
            "
          >

            {{ item }}

          </button>


        </div>

        <div class="date">

          {{ filter.startDate }}

          ~

          {{ filter.endDate }}


        </div>


      </div>

      <!-- 버튼 -->

      <div class="footer-buttons">


        <button
          class="reset"
        >
          초기화
        </button>


        <button
          class="apply"
          @click="apply"
        >
          적용
        </button>


      </div>

    </section>

    <!-- 카드 구분 선택 -->
<CardTypeBottomSheet
  v-if="showCardType"
  @close="showCardType=false"
  @select="selectCardType"
/>


<!-- 카드 선택 -->
<CardSelectBottomSheet
  v-if="showCardSelect"
  @close="showCardSelect=false"
  @select="selectCard"
/>

  </div>
</template>

<style scoped>

.overlay {

  position: fixed;
  inset: 0;

  background: rgba(0,0,0,.35);

  display:flex;
  align-items:flex-end;

  z-index: 1100;

}


.filter-sheet {

  width:100%;

  max-height:85vh;

  overflow-y:auto;

  background:white;

  border-radius:24px 24px 0 0;

  padding:20px;

}



.handle {

  width:40px;
  height:5px;

  background:#ddd;

  border-radius:10px;

  margin:0 auto 20px;

}



.title-area {

  display:flex;

  justify-content:space-between;

  align-items:center;

}



.title-area button {

  border:none;

  background:none;

  font-size:20px;

}



.filter-item {

  margin-top:24px;

}



.filter-item h3 {

  font-size:15px;

  margin-bottom:12px;

}



.chips {

  display:flex;

  gap:8px;

}



.chips.wrap {

  flex-wrap:wrap;

}



.chips button {

  padding:10px 14px;

  border-radius:12px;

  border:1px solid #ddd;

  background:white;

}



.chips button.active {

  border:2px solid #4F46E5;

  color:#4F46E5;

}



.select-button {

  width:100%;

  height:48px;

  padding:0 16px;

  display:flex;

  justify-content:space-between;

  align-items:center;

  border:1px solid #ddd;

  background:white;

  border-radius:12px;

}



.date {

  margin-top:12px;

  padding:14px;

  background:#f7f7f7;

  border-radius:12px;

  text-align:center;

}



.footer-buttons {

  display:flex;

  gap:12px;

  margin-top:30px;

}



.footer-buttons button {

  height:50px;

  border-radius:12px;

  font-size:15px;

}



.reset {

  flex:1;

  border:1px solid #ddd;

  background:white;

}



.apply {

  flex:2;

  border:none;

  background:#4F46E5;

  color:white;

}


</style>