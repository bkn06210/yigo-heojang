<script setup>
import { computed } from 'vue'


// 부모 컴포넌트(FinancialPointCard)에서 전달받는 데이터
const props = defineProps({

  id: {
    type: Number,
    required: true
  },

  name: {
    type: String,
    required: true
  },

  point: {
    type: Number,
    required: true
  }

})


// 천 단위 콤마 적용
const formattedPoint = computed(() => {
  return props.point.toLocaleString()
})


// 클릭 이벤트 전달
// 부모에서 상세 페이지 이동 처리
const emit = defineEmits([
  'click'
])


</script>


<template>

  <div
    class="point-item"
    @click="emit('click', id)"
  >

    <span class="point-name">
      {{ name }}
    </span>


    <div class="point-right">

      <span class="point-value">
        {{ formattedPoint }}P
      </span>


      <span class="arrow">
        >
      </span>

    </div>


  </div>

</template>


<style scoped>

.point-item {

  display: flex;

  justify-content: space-between;

  align-items: center;

  padding: 14px 0;

  cursor: pointer;

}


.point-item:not(:last-child) {

  border-bottom: 1px solid #eeeeee;

}


.point-name {

  font-size: 15px;

  color: #333333;

}


.point-right {

  display: flex;

  align-items: center;

  gap: 8px;

}


.point-value {

  font-size: 16px;

  font-weight: 700;

  color: #1d4ed8;

}


.arrow {

  color: #999;

  font-size: 18px;

}


</style>