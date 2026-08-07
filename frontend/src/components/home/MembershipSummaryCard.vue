<script setup>
import BaseCard from '@/components/common/BaseCard.vue'

const props = defineProps({
  memberships: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['click-more', 'click-item'])
</script>

<template>
  <BaseCard class="membership-summary">
    <div class="membership-list" :class="{ multiple: props.memberships.length > 1 }">
      <article
        v-for="membership in props.memberships.slice(0, 3)"
        :key="membership.id"
        class="membership-item"
        @click="emit('click-item', membership)"
      >
        <div class="membership-icon">M</div>
        <div class="membership-info">
          <p class="membership-name">{{ membership.name }}</p>
        </div>
      </article>
    </div>
  </BaseCard>
</template>

<style scoped>
.membership-summary {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.membership-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  width: 100%;
  justify-content: flex-start;
  flex: 1;
}

.membership-list.multiple {
  justify-content: flex-start;
}

.membership-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm);
  border-radius: var(--radius-md);
  background: var(--color-membership-bg);
  cursor: pointer;
  min-width: 0;
  transition: var(--transition-fast);
}

.membership-item:hover {
  opacity: 0.8;
  transform: translateY(-1px);
}

.membership-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-membership-icon);
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: var(--font-bold);
  font-size: var(--font-sm);
}

.membership-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xxs);
  min-width: 0;
}

.membership-name {
  margin: 0;
  font-size: var(--font-xs);
  color: var(--color-text-primary);
  font-weight: var(--font-medium);
  word-break: break-word;
}
</style>
