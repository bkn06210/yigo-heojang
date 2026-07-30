<script setup>
import PointItem from '@/components/point/PointItem.vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  points: {
    type: Array,
    default: () => [],
  },
})

const router = useRouter()

const goDetail = (id) => {
  router.push(`/points/financial/${id}`)
}
</script>

<template>
  <div class="financial-point-card">
    <PointItem
        v-for="point in props.points"
        :key="point.id"
        :id="point.id"
        :name="point.name"
        :point="point.point"
        @click="goDetail(point.id)"
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