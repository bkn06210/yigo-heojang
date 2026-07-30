<script setup>
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

  background:#fff;

  border-radius:24px 24px 0 0;

  padding:24px;

}

.handle{

  width:48px;

  height:5px;

  border-radius:999px;

  background:#ddd;

  margin:0 auto 20px;

}

.card-info{

  display:flex;

  gap:16px;

  margin:24px 0;

}

.card-image{

  width:72px;

}

.section{

  margin-bottom:24px;

}

.section ul{

  margin-top:12px;

  padding-left:20px;

}

.section li{

  margin-bottom:8px;

}

.select-button{

  width:100%;

  height:48px;

  border:none;

  border-radius:12px;

}

</style>