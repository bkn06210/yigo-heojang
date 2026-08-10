<script setup>
defineOptions({
  inheritAttrs: false
})

defineProps({
  clickable: {
    type: Boolean,
    default: false
  },
  shadow: {
    type: String,
    default: 'md', // sm, md, lg
  }
})

defineEmits([
  'click'
])
</script>

<template>
  <section
    class="base-card"
    :class="[
      {
        clickable,
        [`shadow-${shadow}`]: shadow
      },
      $attrs.class
    ]"
    @click="$emit('click')"
  >
    <slot />
  </section>
</template>


<style scoped>
.base-card {
  width: 100%;
  padding: var(--space-lg);

  background: var(--color-surface);

  border: 1px solid var(--color-border);

  border-radius: var(--radius-lg);

  box-shadow: var(--shadow-md);

  transition: var(--transition-fast);

  overflow: hidden;

  word-break: break-word;

  box-sizing: border-box;
}

.base-card.shadow-sm {
  box-shadow: var(--shadow-sm);
}

.base-card.shadow-md {
  box-shadow: var(--shadow-md);
}

.base-card.shadow-lg {
  box-shadow: var(--shadow-lg);
}


.base-card.my-card {
  background: linear-gradient(
    135deg,
    rgba(var(--color-primary-dark-rgb), 0.08),
    rgba(var(--color-primary-dark-rgb), 0.04)
  );

  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.15);

  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);

}


.base-card.clickable {
  cursor: pointer;
}


.base-card.clickable:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}


[data-theme="dark"] .base-card.my-card {
  background: linear-gradient(
    135deg,
    rgba(var(--color-primary-dark-rgb), 0.12),
    rgba(var(--color-primary-dark-rgb), 0.06)
  );

  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);

  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);

}
</style>