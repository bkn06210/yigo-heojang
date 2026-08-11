<template>
  <header class="app-header">
    <h1>{{ title }}</h1>
  </header>
</template>

<script setup>
// 헤더 제목을 부모에서 전달받음
defineProps({
  title: {
    type: String,
    default: '',
  },
})
</script>

<style scoped>
.app-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #e5e5e5;
  background: white;
}

h1 {
  font-size: 20px;
  font-weight: 700;
}
</style>