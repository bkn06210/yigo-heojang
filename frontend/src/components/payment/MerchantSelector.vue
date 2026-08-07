<script setup>

import { ref } from 'vue';


const emit = defineEmits([
  'update:category',
  'update:merchant'
]);


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
  v-if="selectedCategory"
  class="merchant-section"
>

  <h3>
    {{ selectedCategory }} 가맹점 선택
  </h3>


  <div class="merchant-list">

    <button
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
<!-- 07_25 연동 변경: 결제 추천 API가 요구하는 가맹점 정보를 선택 결과로 전달한다. -->
