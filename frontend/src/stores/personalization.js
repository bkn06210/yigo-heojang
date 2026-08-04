import { defineStore } from 'pinia';
import { ref } from 'vue';

export const usePersonalizationStore = defineStore(
  'personalization',
  () => {
    // 개인화 카테고리 설정 (현장 결제 기준)
    const categories = ref([
      {
        key: 'cafe',
        label: '카페',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        key: 'convenience',
        label: '편의점',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        key: 'food',
        label: '음식점',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        key: 'mart',
        label: '마트',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        key: 'shopping',
        label: '쇼핑',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        key: 'beauty',
        label: '뷰티',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        key: 'culture',
        label: '문화/여가',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        key: 'transport',
        label: '교통',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        key: 'medical',
        label: '의료',
        checked: false,
        tags: [],
        inputTag: '',
      },
    ]);

    // 활성화된 카테고리만 가져오기
    const getActiveCategories = () => {
      return categories.value.filter(cat => cat.checked);
    };

    // 카테고리 업데이트
    const updateCategories = (newCategories) => {
      categories.value = newCategories;
    };

    return {
      categories,
      getActiveCategories,
      updateCategories
    };
  }
);
