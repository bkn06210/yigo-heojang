<script setup>
import { ref } from 'vue'

const agreeAll = ref(false)
const terms = ref([
  {
    id: 1,
    title: '서비스 이용약관',
    required: true,
    checked: false
  },
  {
    id: 2,
    title: '개인정보 수집 및 이용',
    required: true,
    checked: false
  },
  {
    id: 3,
    title: '마케팅 정보 수신',
    required: false,
    checked: false
  }
])
</script>

<template>
  <div>
    <h1>약관 동의</h1>

    <label>
      <input type="checkbox" v-model="agreeAll">
      전체 동의
    </label>

    <div
      v-for="term in terms"
      :key="term.id"
    >
      <label>
        <input type="checkbox" v-model="term.checked">
        {{ term.required ? '(필수)' : '(선택)' }}
        {{ term.title }}
      </label>
    </div>

    <button>
      다음
    </button>
  </div>
</template>

<style scoped>

</style>
