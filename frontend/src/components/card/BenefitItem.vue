<script setup>
defineProps({
  benefit: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <div class="benefit-item">
    <h3>{{ benefit.category }}</h3>

    <p>{{ benefit.description }}</p>
  </div>
</template>

<style scoped>
.benefit-item {
  padding: 16px;
<<<<<<< HEAD
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-surface);
=======
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  background: #fff;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

h3 {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
<<<<<<< HEAD
  color: var(--color-text-primary);
}

p {
  color: var(--color-text-secondary);
=======
}

p {
  color: #666;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  font-size: 14px;
  line-height: 1.5;
}
</style>