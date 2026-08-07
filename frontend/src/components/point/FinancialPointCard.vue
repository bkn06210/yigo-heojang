<script setup>
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
      :point="point.point"
      @select-point="selectPoint"
    />
  </BaseCard>
</template>
