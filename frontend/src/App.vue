<script setup>
import { onMounted } from 'vue'

import { getMyInfo } from '@/api/memberApi'
import { useAuthStore } from '@/stores/authStore'

const authStore = useAuthStore()

onMounted(async () => {
  if (!authStore.token) return

  try {
    const response = await getMyInfo()
    authStore.updateUser(response.data.data)
  } catch (error) {
    console.error('로그인 회원 정보 복원 실패:', error)
  }
})

</script>

<template>
  <router-view />
</template>

<style>

</style>
