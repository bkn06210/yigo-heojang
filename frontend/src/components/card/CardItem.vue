<script setup>
import { useRouter } from 'vue-router';

import { maskCardNumber } from '@/utils/card';

const props = defineProps({
  card: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits([
  'toggle-pin',
]);

const router = useRouter();


// 카드 상세 이동
const goDetail = () => {
  router.push(`/cards/${props.card.id}`);
};


// 고정 버튼 클릭
const handlePinClick = () => {
  emit('toggle-pin', props.card.id);
};
</script>


<template>
  <div
    class="card-item"
    @click="goDetail"
  >

    <!-- 카드 정보 -->
    <div class="card-left">

      <img
        :src="card.image"
        :alt="card.name"
        class="card-image"
      />


      <div class="card-info">

        <!-- 카드명 -->
        <div class="card-name">
          {{ card.name }}
        </div>


        <!-- 카드사 -->
        <div class="company">
          {{ card.company }}
        </div>


        <!-- 카드번호 -->
        <div class="number">

          {{ card.owner }}

          {{ maskCardNumber(card.cardNumber) }}

        </div>

      </div>

    </div>



    <!-- 고정 버튼 -->
    <button
      class="pin-button"
      @click.stop="handlePinClick"
    >
      {{ card.pinned ? '📌' : '📍' }}
    </button>


  </div>
</template>



<style scoped>

.card-item {

  display:flex;

  justify-content:space-between;

  align-items:center;

  padding:16px 0;

  cursor:pointer;

}


.card-left {

  display:flex;

  align-items:center;

  gap:16px;

}



.card-image {

  width:72px;

  height:auto;

  border-radius:10px;

}



.card-info {

  display:flex;

  flex-direction:column;

  gap:4px;

}



.card-name {

  font-size:15px;

  font-weight:700;

}



.company {

  font-size:13px;

  color:#666;

}



.number {

  font-size:13px;

  color:#888;

}



.pin-button {

  background:none;

  border:none;

  font-size:20px;

  cursor:pointer;

}


</style>