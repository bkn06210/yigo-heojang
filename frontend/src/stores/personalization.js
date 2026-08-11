import { defineStore } from 'pinia';
import { ref } from 'vue';

export const usePersonalizationStore = defineStore(
  'personalization',
  () => {
    // 개인화 카테고리 설정 (현장 결제 기준)
    const categories = ref([
      {
        id: 102,
        key: 'cafe',
        label: '카페',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        id: 201,
        key: 'convenience',
        label: '편의점',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        id: 101,
        key: 'food',
        label: '음식점',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        id: 202,
        key: 'mart',
        label: '마트',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        id: 204,
        key: 'shopping',
        label: '쇼핑',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        id: 205,
        key: 'beauty',
        label: '뷰티',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        id: 501,
        key: 'culture',
        label: '문화/여가',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        id: 301,
        key: 'transport',
        label: '교통',
        checked: false,
        tags: [],
        inputTag: '',
      },
      {
        id: 601,
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
