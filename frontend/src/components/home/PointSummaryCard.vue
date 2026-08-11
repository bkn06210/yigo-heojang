<script setup>
import BaseCard from '@/components/common/BaseCard.vue'

const props = defineProps({
  isLogin: {
    type: Boolean,
    default: false,
  },
  points: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['click-item'])
</script>

<template>
  <BaseCard class="point-summary">
    <div v-if="!props.isLogin" class="empty">
      <p>
        로그인하면
        <br />
        등록된 금융 포인트를 확인할 수 있어요.
      </p>
    </div>

    <div v-else-if="!props.points.length" class="empty">
      <p>등록된 금융 포인트가 없습니다.</p>
    </div>

    <div v-else class="point-list" :class="{ multiple: props.points.length > 1 }">
      <article
        v-for="point in props.points.slice(0, 3)"
        :key="point.id"
        class="point-item"
        @click="emit('click-item', point)"
      >
        <div class="point-icon">P</div>
        <div class="point-info">
          <p class="point-name">{{ point.name }}</p>
          <p class="point-balance">
            {{ point.point ? point.point.toLocaleString() : '0' }} P
          </p>
        </div>
      </article>
    </div>
  </BaseCard>
</template>

<style scoped>
.point-summary {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.empty {
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: var(--font-xs);
  flex: 1;
}

.empty p {
  margin: 0;
  line-height: 1.5;
}

.point-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  width: 100%;
  justify-content: flex-start;
  flex: 1;
}

.point-list.multiple {
  justify-content: flex-start;
}

.point-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm);
  border-radius: var(--radius-md);
  background: var(--color-point-bg);
  cursor: pointer;
  min-width: 0;
  transition: var(--transition-fast);
}

.point-item:hover {
  opacity: 0.8;
  transform: translateY(-1px);
}

.point-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--color-surface);
  color: var(--color-point-icon);
  font-weight: var(--font-bold);
  font-size: var(--font-sm);
}

.point-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xxs);
  min-width: 0;
}

.point-name {
  margin: 0;
  font-size: var(--font-xs);
  color: var(--color-text-primary);
  font-weight: var(--font-medium);
  word-break: break-word;
}

.point-balance {
  margin: 0;
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  font-size: var(--font-xs);
}
</style>
