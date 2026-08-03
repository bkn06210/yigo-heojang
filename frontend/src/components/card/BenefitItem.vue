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
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  background: #fff;
}

h3 {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
}

p {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}
</style>