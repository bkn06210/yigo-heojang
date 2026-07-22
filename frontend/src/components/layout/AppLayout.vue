<template>
  <div class="layout">
    <AppHeader :title="title" />

    <main class="content">
      <slot />
    </main>

    <BottomNavigation />
  </div>
</template>

<script setup>
import AppHeader from './AppHeader.vue'
import BottomNavigation from './BottomNavigation.vue'

// 페이지 제목
defineProps({
  title: {
    type: String,
    default: '',
  },
})
</script>

<style scoped>
.layout {
  height: 100vh;

  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
</style>