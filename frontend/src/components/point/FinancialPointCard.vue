<script setup>
<<<<<<< HEAD
import BaseCard from '@/components/common/BaseCard.vue'
import PointItem from '@/components/point/PointItem.vue'

const props = defineProps({
  points: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['select-point', 'click'])

const selectPoint = (point) => {
  emit('select-point', point)
  emit('click', point)
}
</script>

<template>
  <BaseCard class="financial-point-card">
    <PointItem
      v-for="point in props.points"
      :key="point.id"
      :id="point.id"
      :name="point.name"
      :point="point.balance"
      @select-point="selectPoint"
    />
  </BaseCard>
</template>
=======
import PointItem from '@/components/point/PointItem.vue'



// 임시 데이터
//
// TODO: API 연동 시 변경
// GET /api/financial-points
//
// 기존 상세 페이지 API(/points/financial/:id)에서 사용하던 데이터 구조와 동일하게
// 바텀시트에 전달 예정
//

defineProps({
  pointList: {
    type: Array,
    default: () => [],
  },
})




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
<!-- 07_25 연동 변경: 금융포인트 카드 표시값을 백엔드 응답 구조에 맞췄다. -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
