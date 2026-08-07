<script setup>

import { computed } from 'vue';
import { usePersonalizationStore } from '@/stores/personalization';

const personalizationStore = usePersonalizationStore();

const props = defineProps({
  category: {
    type: String,
    default: null
  },
  merchant: {
    type: String,
    default: null
  }
});
<script setup>

import { ref } from 'vue';


const emit = defineEmits([
  'update:category',
  'update:merchant'
]);

// Store에서 활성화된 카테고리만 가져오기
const activeCategories = computed(() => {
  return personalizationStore.getActiveCategories();
});


// Store의 카테고리 정보를 merchants로 변환
const getMerchants = () => {
  const result = {};
  personalizationStore.getActiveCategories().forEach(category => {
    result[category.label] = [...category.tags];
  });
  return result;
};

const merchants = computed(() => getMerchants());

const categories = [
  '카페',
  '음식점',
  '쇼핑',
  '교통',
  '문화'
];


const merchants = {

  카페:[
    '스타벅스',
    '투썸플레이스'
  ],

  음식점:[
    '배달의민족',
    '쿠팡이츠'
  ]

};


const selectedCategory = ref(null);
const selectedMerchant = ref(null);



// 업종 선택
const selectCategory = (category) => {

  selectedCategory.value = category;


  // 기존 가맹점 선택 초기화
  emit(
    'update:merchant',
    null
  );
  selectedMerchant.value = null;


  emit(
    'update:category',
    category
  );

};



// 가맹점 선택
const selectMerchant = (merchant) => {

  selectedMerchant.value = merchant;

  emit(
    'update:merchant',
    merchant
  );

};

</script>


<template>

<!-- 카테고리 선택 영역 -->
<section class="category-section">

  <h3>
    업종 선택
  </h3>

  <div class="category-buttons">

    <button
      v-for="cat in activeCategories"
      :key="cat.key"
      @click="selectCategory(cat.label)"
      :class="{ active: cat.label === category }"
    >
      {{ cat.label }}
      v-for="category in categories"
      :key="category"
      @click="selectCategory(category)"
      :class="{ active: selectedCategory === category }"
    >
      {{ category }}
    </button>

  </div>

</section>



<!-- 가맹점 선택 영역 -->
<section
  v-if="category"
  v-if="selectedCategory"
  class="merchant-section"
>

  <h3>
    {{ category }} 가맹점 선택
    {{ selectedCategory }} 가맹점 선택
  </h3>


  <div class="merchant-list">

    <button
      v-for="mct in merchants[category] || []"
      :key="mct"
      @click="selectMerchant(mct)"
      :class="{ active: mct === merchant }"
    >
      {{ mct }}
      v-for="merchant in merchants[selectedCategory] || []"
      :key="merchant"
      @click="selectMerchant(merchant)"
      :class="{ active: selectedMerchant === merchant }"
    >
      {{ merchant }}

    </button>

  </div>

</section>

</template>


<style scoped>

.category-section h3,
.merchant-section h3{

  margin: 0 0 var(--space-sm);

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);

}

.merchant-section{

  margin-top: var(--space-lg);

}

.category-buttons,
.merchant-list{

  display: flex;

  flex-wrap: wrap;

  gap: var(--space-xs);

}

.category-buttons button,
.merchant-list button{

  padding: var(--space-xs) var(--space-md);

  border-radius: var(--radius-full);

  background: var(--color-surface);
  border: 1px solid var(--color-border);

  color: var(--color-text-primary);

  font-size: var(--font-sm);
  font-weight: var(--font-medium);

  cursor: pointer;

  transition: var(--transition-fast);

}

.category-buttons button:hover,
.merchant-list button:hover{

  background: var(--color-bg);

}

.category-buttons button.active,
.merchant-list button.active{

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.16) 0%, rgba(var(--color-primary-dark-rgb), 0.05) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.4);

  color: var(--color-primary-dark);
  font-weight: var(--font-bold);

}

[data-theme="dark"] .category-buttons button.active,
[data-theme="dark"] .merchant-list button.active{

  background: linear-gradient(135deg, rgba(214, 186, 110, 0.24) 0%, rgba(214, 186, 110, 0.08) 100%);
  border: 1px solid rgba(214, 186, 110, 0.45);

  color: var(--color-primary-dark);

}

</style>
<!-- 07_25 연동 변경: 결제 추천 API가 요구하는 가맹점 정보를 선택 결과로 전달한다. -->
