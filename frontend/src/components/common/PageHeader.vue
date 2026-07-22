<script setup>

defineProps({
  title: {
    type: String,
    default: ''
  }
})

</script>

<template>
  <header class="page-header">
    {{ title }}
  </header>
</template>

<style scoped>

.page-header {
  padding: 20px;
  font-size: 20px;
  font-weight: 700;
}

</style>