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
    <PageHeader title="혜택 상세" />

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
  padding: 20px;
}

.card-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.card-image {
  width: 220px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.card-section h2 {
  font-size: 20px;
  font-weight: 700;
}

.benefit-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style> 
