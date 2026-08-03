<script setup>
import { ref } from 'vue'

import PointItem from '@/components/point/PointItem.vue'



// 임시 데이터
//
// TODO: API 연동 시 변경
// GET /api/financial-points
//
// 기존 상세 페이지 API(/points/financial/:id)에서 사용하던 데이터 구조와 동일하게
// 바텀시트에 전달 예정
//

const pointList = ref([

  {
    id: 1,
    name: 'KB Pay 포인트',
    point: 12300,

    aiMessage:
      '현재 보유 포인트는 카드 결제에 활용하는 것을 추천해요.',

    usageMethods: [
      '카드 결제',
      '간편결제',
      '포인트 전환'
    ]

  },


  {
    id: 2,
    name: 'Liiv 포인트',
    point: 8500,

    aiMessage:
      '자주 사용하는 금융 서비스에서 활용할 수 있어요.',

    usageMethods: [
      '포인트 사용',
      '포인트 전환'
    ]

  },


  {
    id: 3,
    name: '스타포인트',
    point: 1200,

    aiMessage:
      '소멸 예정 포인트가 있는지 확인해보세요.',

    usageMethods: [
      '카드 결제',
      '포인트 사용'
    ]

  }

])




// 부모(PointListView)로 선택한 포인트 전달

const emit = defineEmits([

  'select-point'

])



// 포인트 클릭

const selectPoint = (point) => {

  emit('select-point', point)

}


</script>




<template>


<div class="financial-point-card">


<PointItem

  v-for="point in pointList"

  :key="point.id"

  :id="point.id"

  :name="point.name"

  :point="point.point"

  @click="selectPoint(point)"

/>


</div>


</template>




<style scoped>


.financial-point-card {

  background: white;

  border-radius: 16px;

  padding: 20px;


  box-shadow:

    0 2px 8px rgba(0, 0, 0, 0.08);

}


</style>