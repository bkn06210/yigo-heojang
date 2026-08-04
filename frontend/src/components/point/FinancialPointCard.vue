<script setup>
import { ref } from 'vue'
import BaseCard from '@/components/common/BaseCard.vue'
import PointItem from '@/components/point/PointItem.vue'

// TODO: API 연동 시 변경 — GET /api/financial-points
const pointList = ref([
  {
    id: 1,
    name: 'KB Pay 포인트',
    point: 12300,
    aiMessage: '현재 보유 포인트는 카드 결제에 활용하는 것을 추천해요.',
    usageMethods: ['카드 결제', '간편결제', '포인트 전환'],
  },
  {
    id: 2,
    name: 'Liiv 포인트',
    point: 8500,
    aiMessage: '자주 사용하는 금융 서비스에서 활용할 수 있어요.',
    usageMethods: ['포인트 사용', '포인트 전환'],
  },
  {
    id: 3,
    name: '스타포인트',
    point: 1200,
    aiMessage: '소멸 예정 포인트가 있는지 확인해보세요.',
    usageMethods: ['카드 결제', '포인트 사용'],
  },
])

const emit = defineEmits(['select-point', 'click'])

const selectPoint = (point) => {
  emit('select-point', point)
  emit('click', point)
}
</script>

<template>
  <BaseCard class="financial-point-card">
    <PointItem
      v-for="point in pointList"
      :key="point.id"
      :id="point.id"
      :name="point.name"
      :point="point.point"
      @select-point="selectPoint"
    />
  </BaseCard>
</template>
