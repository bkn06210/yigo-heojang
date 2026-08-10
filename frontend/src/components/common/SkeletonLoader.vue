<script setup>
defineProps({
  type: {
    type: String,
    default: 'card', // card, text-line, avatar, button
  },
  count: {
    type: Number,
    default: 1,
  },
})
</script>

<template>
  <div v-if="type === 'card'" class="skeleton-card">
    <div class="skeleton-header"></div>
    <div class="skeleton-line"></div>
    <div class="skeleton-line short"></div>
  </div>

  <div v-else-if="type === 'text-line'" class="skeleton-container">
    <div v-for="i in count" :key="i" class="skeleton-line"></div>
  </div>

  <div v-else-if="type === 'avatar'" class="skeleton-avatar"></div>

  <div v-else-if="type === 'button'" class="skeleton-button"></div>
</template>

<style scoped>
@keyframes shimmer {
  0% {
    background-position: -1000px 0;
  }
  100% {
    background-position: 1000px 0;
  }
}

.skeleton-card {
  width: 100%;
  padding: var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.skeleton-header {
  width: 100%;
  height: 24px;
  margin-bottom: var(--space-md);
  background: linear-gradient(
    90deg,
    #f0f0f0 25%,
    #e0e0e0 50%,
    #f0f0f0 75%
  );
  background-size: 1000px 100%;
  border-radius: var(--radius-sm);
  animation: shimmer 2s infinite;
}

.skeleton-line {
  width: 100%;
  height: 14px;
  margin-bottom: var(--space-sm);
  background: linear-gradient(
    90deg,
    #f0f0f0 25%,
    #e0e0e0 50%,
    #f0f0f0 75%
  );
  background-size: 1000px 100%;
  border-radius: var(--radius-xs);
  animation: shimmer 2s infinite;
}

.skeleton-line.short {
  width: 60%;
}

.skeleton-container {
  width: 100%;
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  background: linear-gradient(
    90deg,
    #f0f0f0 25%,
    #e0e0e0 50%,
    #f0f0f0 75%
  );
  background-size: 1000px 100%;
  animation: shimmer 2s infinite;
}

.skeleton-button {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-md);
  background: linear-gradient(
    90deg,
    #f0f0f0 25%,
    #e0e0e0 50%,
    #f0f0f0 75%
  );
  background-size: 1000px 100%;
  animation: shimmer 2s infinite;
}

/* Dark mode */
[data-theme="dark"] .skeleton-header,
[data-theme="dark"] .skeleton-line,
[data-theme="dark"] .skeleton-avatar,
[data-theme="dark"] .skeleton-button {
  background: linear-gradient(
    90deg,
    #2a2a2a 25%,
    #3a3a3a 50%,
    #2a2a2a 75%
  );
  background-size: 1000px 100%;
}

[data-theme="dark"] .skeleton-card {
  background: var(--color-surface);
  border-color: var(--color-border);
}
</style>
