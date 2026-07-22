<script setup>
import { ref } from 'vue'

import PointItem from '@/components/point/PointItem.vue'
import { useRouter } from 'vue-router'


// 임시 데이터
// 추후 백엔드 금융 포인트 조회 API 응답으로 교체
const pointList = ref([
  {
    id: 1,
    name: 'KB Pay 포인트',
    point: 12300
  },
  {
    id: 2,
    name: 'Liiv 포인트',
    point: 8500
  },
  {
    id: 3,
    name: '스타포인트',
    point: 1200
  }
])

const router = useRouter()

const goDetail = (id) => {
  router.push(`/points/financial/${id}`)
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
       @click="goDetail"
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