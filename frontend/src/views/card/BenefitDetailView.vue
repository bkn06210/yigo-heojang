<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import BenefitItem from '@/components/card/BenefitItem.vue';

const router = useRouter();

// 임시 데이터
// 추후 API 연결 예정
const card = ref({
  name: 'KB My WE:SH 카드',
  image: '/images/cards/kb.png',
});

const benefits = ref([
  {
    id: 1,
    category: '카페',
    description: '혜택 내용이 표시됩니다.',
  },
  {
    id: 2,
    category: '쇼핑',
    description: '혜택 내용이 표시됩니다.',
  },
  {
    id: 3,
    category: '교통',
    description: '혜택 내용이 표시됩니다.',
  },
  {
    id: 4,
    category: '생활',
    description: '혜택 내용이 표시됩니다.',
  },
]);
</script>

<template>
  <div class="benefit-detail-page">
<<<<<<< HEAD
    <PageHeader title="혜택 상세" @back="router.back()" />
=======
    <PageHeader title="혜택 상세" />
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

    <!-- 카드 정보 -->
    <section class="card-section">
      <img
        :src="card.image"
        :alt="card.name"
        class="card-image"
      />

      <h2>{{ card.name }}</h2>
    </section>

    <!-- 혜택 목록 -->
    <section class="benefit-section">
      <BenefitItem
        v-for="benefit in benefits"
        :key="benefit.id"
        :benefit="benefit"
      />
    </section>
  </div>
</template>

<style scoped>
.benefit-detail-page {
<<<<<<< HEAD
  padding: var(--space-md);
  box-sizing: border-box;
=======
  padding: 20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.card-section {
  display: flex;
  flex-direction: column;
  align-items: center;
<<<<<<< HEAD
  margin-bottom: var(--space-2xl);
=======
  margin-bottom: 32px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.card-image {
  width: 220px;
<<<<<<< HEAD
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-md);
}

.card-section h2 {
  font-size: var(--font-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
=======
  border-radius: 12px;
  margin-bottom: 16px;
}

.card-section h2 {
  font-size: 20px;
  font-weight: 700;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.benefit-section {
  display: flex;
  flex-direction: column;
<<<<<<< HEAD
  gap: var(--space-md);
=======
  gap: 16px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}
</style> 
