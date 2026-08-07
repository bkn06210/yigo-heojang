<script setup>

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['back']);

const handleBack = () => {
  emit('back');
};

</script>


<template>

<header class="page-header">

  <button
    v-if="showBack"
    class="back-button"
    @click="handleBack"
    type="button"
  >
    <span class="back-icon">‹</span>
  </button>

  <h1>
    {{ title }}
  </h1>

  <div class="actions">

    <slot />

  </div>

</header>

</template>


<style scoped>

.page-header {

  display: flex;

  align-items: center;

  justify-content: space-between;

  padding: var(--space-lg) 0;

  margin-bottom: var(--space-md);

  gap: var(--space-md);

}

.back-button {

  flex-shrink: 0;

  width: 40px;

  height: 40px;

  border: none;

  background: transparent;

  cursor: pointer;

  display: flex;

  align-items: center;

  justify-content: center;

  color: var(--color-text-primary);

  transition: var(--transition-fast);

  border-radius: var(--radius-md);

}

.back-button:hover {

  background: var(--color-bg);

}

.back-icon {

  font-size: var(--font-2xl);

  font-weight: var(--font-bold);

  line-height: 1;

}

h1 {

  margin: 0;

  color: var(--color-text-primary);

  font-size: var(--font-title);

  font-weight: var(--font-bold);

  flex: 1;

}

.actions {

  display: flex;

  align-items: center;

  gap: var(--space-sm);

  flex-shrink: 0;

}



</style>