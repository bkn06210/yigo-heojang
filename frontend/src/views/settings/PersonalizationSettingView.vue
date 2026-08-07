<<<<<<< HEAD
﻿<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { usePersonalizationStore } from '@/stores/personalization';
=======
<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  getPersonalization,
  updatePersonalization,
} from '@/api/personalizationApi';
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

import PageHeader from '@/components/common/PageHeader.vue';
import AppButton from '@/components/common/AppButton.vue';
import AppCheckbox from '@/components/common/AppCheckbox.vue';
import AppInput from '@/components/common/AppInput.vue';
<<<<<<< HEAD
import { useToast } from '@/composables/useToast';

const router = useRouter();
const personalizationStore = usePersonalizationStore();
const { showToast } = useToast();

/**
 * 개인화 설정 카테고리 상태
 * - checked : 관심 영역 선택 여부
 * - tags : 세부 브랜드/키워드
 * - hasTags : 직접 입력 가능한 카테고리 여부
 *
 */

// 브랜드 검색 목업 데이터
const brandList = {
  cafe: [
    {
      name: '스타벅스',
      alias: ['스타벅스', 'starbucks'],
    },
    {
      name: '투썸플레이스',
      alias: ['투썸플레이스', 'twosome', 'twosomeplace'],
    },
    {
      name: '메가커피',
      alias: ['메가커피', 'mega coffee'],
    },
    {
      name: '이디야',
      alias: ['이디야', 'ediya'],
    },
  ],

  convenience: [
    {
      name: 'GS25',
      alias: ['gs25', '지에스25', '지에스'],
    },
    {
      name: 'CU',
      alias: ['cu', '씨유'],
    },
    {
      name: '이마트24',
      alias: ['이마트24', 'emart24'],
    },
  ],

  food: [
    {
      name: '맥도날드',
      alias: ['맥도날드', 'mcdonald', 'mcd'],
    },
    {
      name: '롯데리아',
      alias: ['롯데리아', 'lotteria'],
    },
    {
      name: '버거킹',
      alias: ['버거킹', 'burgerking'],
    },
  ],

  mart: [
    {
      name: '이마트',
      alias: ['이마트', 'emart'],
    },
    {
      name: '홈플러스',
      alias: ['홈플러스', 'homeplus'],
    },
    {
      name: '롯데마트',
      alias: ['롯데마트', 'lottemart'],
    },
  ],

  beauty: [
    {
      name: '올리브영',
      alias: ['올리브영', 'oliveyoung'],
    },
  ],

  culture: [
    {
      name: 'CGV',
      alias: ['cgv', '씨지브이'],
    },
    {
      name: '롯데시네마',
      alias: ['롯데시네마', 'lottecinema'],
    },
  ],
};

// 한글 초성만 추출 (예: "스타벅스" → "ㅅㅌㅂㅅ")
const extractChoseong = (text) => {
  const choseong = ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'];

  let result = '';
  for (let char of text) {
    const code = char.charCodeAt(0);
    if (code >= 0xAC00 && code <= 0xD7A3) {
      const temp = code - 0xAC00;
      const cho = Math.floor(temp / (28 * 21));
      result += choseong[cho];
    } else if ((code >= 0x1100 && code <= 0x11FF) || (code >= 0x3130 && code <= 0x318F)) {
      // 이미 자모인 경우 그대로 추가
      result += char;
    } else {
      result += char;
    }
  }
  return result;
};

// 검색 결과 (한글 자모 + 완성된 한글 포함)
=======

const router = useRouter();
const isSaving = ref(false);

// 기존 category 테이블의 대분류·소분류·가맹점 계층을 API 응답 그대로 보관한다.
const categoryGroups = ref([]);

// 검색 결과
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
const getSuggestions = (category) => {
  const keyword = category.inputTag.trim().toLowerCase();

  if (!keyword) return [];

<<<<<<< HEAD
  // 검색어의 초성 추출
  const keywordChoseong = extractChoseong(keyword);

  return (brandList[category.key] || []).filter((brand) => {
    return brand.alias.some((word) => {
      const lowerWord = word.toLowerCase();
      const wordChoseong = extractChoseong(lowerWord);

      // 완성된 한글, 자모, 초성 모두로 검색
      return (
        lowerWord.includes(keyword) ||
        wordChoseong.includes(keywordChoseong) ||
        wordChoseong.includes(keyword)
      );
    });
  });
};

// 브랜드 선택
const selectBrand = (category, brand) => {
  if (category.tags.length >= 3) {
    showToast('warning', '최대 3개까지만 입력할 수 있습니다.');
    return;
  }

  category.tags.push(brand.name);

  category.inputTag = '';
};

/**
 * 검색 alias 하이라이트
 * 예)
 * 검색어: ㅅ
 * alias: 스타벅스
 * 결과: <strong>스</strong>타벅<strong>스</strong>
=======
  // [개인화 API 연동] DB merchant는 추천어로만 사용하고 직접 입력 문자열과 중복되지 않게 표시한다.
  return category.merchants.filter((brand) => {
    const alreadyAdded = category.brands.some((name) => name.toLowerCase() === brand.name.toLowerCase());
    return !alreadyAdded && brand.alias.some((word) => word.toLowerCase().includes(keyword));
  });
};

/**
 * 검색 alias 하이라이트
 * 예)
 * 검색어: lotte
 * alias: lottemart
 * 결과: <strong>lotte</strong>mart
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
 */
const highlightAlias = (alias, keyword) => {
  if (!alias || !keyword) return alias;

<<<<<<< HEAD
  const lowerKeyword = keyword.toLowerCase();
  const lowerAlias = alias.toLowerCase();
  const keywordChoseong = extractChoseong(lowerKeyword);
  const aliasChoseong = extractChoseong(lowerAlias);

  // 완성된 한글로 직접 매칭 (예: "lotte" in "lottemart")
  if (lowerAlias.includes(lowerKeyword)) {
    const regex = new RegExp(
      `(${lowerKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`,
      'gi'
    );
    return alias.replace(regex, '<strong>$1</strong>');
  }

  // 초성으로 매칭 (예: "ㅅ" or "스" in "스타벅스")
  if (aliasChoseong.includes(keywordChoseong) || aliasChoseong.includes(lowerKeyword)) {
    let result = '';
    let choseongIndex = 0;

    for (let i = 0; i < alias.length; i++) {
      const char = alias[i];
      const charChoseong = extractChoseong(char.toLowerCase());

      if (
        aliasChoseong.substring(choseongIndex, choseongIndex + keywordChoseong.length) === keywordChoseong ||
        aliasChoseong.substring(choseongIndex, choseongIndex + lowerKeyword.length) === lowerKeyword
      ) {
        result += `<strong>${char}</strong>`;
        choseongIndex += charChoseong.length;
      } else {
        result += char;
        choseongIndex += charChoseong.length;
      }
    }
    return result;
  }

  return alias;
};

// 검색어와 매칭되는 alias 반환 (완성형 + 초성 검색 모두 지원)
const getMatchedAlias = (brand, keyword) => {
  const lowerKeyword = keyword.toLowerCase();
  const keywordChoseong = extractChoseong(lowerKeyword);

  return brand.alias.find((a) => {
    const lowerA = a.toLowerCase();
    const aChoseong = extractChoseong(lowerA);

    return (
      lowerA.includes(lowerKeyword) ||
      aChoseong.includes(keywordChoseong) ||
      aChoseong.includes(lowerKeyword)
    );
  });
};

// Store의 categories 참조 (양방향 바인딩)
const categories = computed(() => personalizationStore.categories);

=======
  // [직접 입력 복구] 정규식 특수문자를 브랜드 검색어로 입력해도 화면이 깨지지 않게 이스케이프한다.
  const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const regex = new RegExp(
    `(${escapedKeyword})`,
    'gi'
  );

  return alias.replace(
    regex,
    '<strong>$1</strong>'
  );
};

// 검색어와 매칭되는 alias 반환
const getMatchedAlias = (brand, keyword) => {
  return brand.alias.find((alias) =>
    alias.toLowerCase().includes(keyword.toLowerCase())
  ) || brand.name;
};

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
/**
 * 뒤로가기
 */
const goBack = () => {
  router.go(-1);
};

/**
<<<<<<< HEAD
 * 태그 추가
 */
const addTag = (category) => {
  const value = category.inputTag.trim();

  if (!value) return;

  if (category.tags.length >= 3) {
    showToast('warning', '최대 3개까지만 입력할 수 있습니다.');
    return;
  }

  category.tags.push(value);
  category.inputTag = '';
};

/**
 * 태그 삭제
 */
const removeTag = (category, index) => {
  category.tags.splice(index, 1);
=======
 * 저장된 개인화 설정 조회
 */
const loadPersonalization = async () => {
  try {
    const response = await getPersonalization();
    // 프론트 하드코딩 대신 DB category/merchant와 회원 선택 상태를 화면 모델로 변환한다.
    categoryGroups.value = (response?.groups || []).map((group) => ({
      ...group,
      children: (group.children || []).map((category, index) => ({
        ...category,
        groupName: group.categoryName,
        isFirstInGroup: index === 0,
        selected: Boolean(category.selected),
        inputTag: '',
        // [개인화 API 연동] member_personalization_brand에서 조회한 문자열을 태그로 복원한다.
        brands: [...(category.brands || [])],
        merchants: (category.merchants || []).map((merchant) => ({
          ...merchant,
          name: merchant.merchantName,
          alias: [merchant.merchantName],
        })),
      })),
    }));
  } catch (error) {
    alert(error.response?.data?.message || '개인화 설정을 불러오지 못했습니다.');
  }
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
};

/**
 * 개인화 설정 저장
<<<<<<< HEAD
 * TODO: 백엔드 API 연결 예정
 */
const savePersonalization = () => {
  // Store에 명시적으로 업데이트
  personalizationStore.updateCategories(categories.value);

  console.log('개인화 설정 저장:', categories.value);

  router.go(-1);
};

=======
 */
const savePersonalization = async () => {
  if (isSaving.value) return;

  isSaving.value = true;
  try {
    // [개인화 API 연동] 선택 카테고리 ID와 직접 입력 브랜드 문자열을 함께 저장한다.
    const selectedCategories = categoryGroups.value
      .flatMap((group) => group.children)
      .filter((category) => category.selected);
    await updatePersonalization({
      categoryIds: selectedCategories.map((category) => category.categoryId),
      brands: selectedCategories.flatMap((category) => category.brands.map((brandName) => ({
        categoryId: category.categoryId,
        brandName,
      }))),
    });

    router.go(-1);
  } catch (error) {
    alert(error.response?.data?.message || '개인화 설정을 저장하지 못했습니다.');
  } finally {
    isSaving.value = false;
  }
};

// [개인화 API 연동] 입력값은 merchant 등록 여부와 관계없이 문자열 브랜드로 최대 3개까지 추가한다.
const addBrand = (category, value = category.inputTag) => {
  if (!category.selected || category.brands.length >= 3) return;
  const brandName = String(value || '').trim();
  if (!brandName) return;
  if (brandName.length > 100) {
    alert('브랜드명은 100자 이하로 입력해주세요.');
    return;
  }
  if (category.brands.some((name) => name.toLowerCase() === brandName.toLowerCase())) {
    alert('이미 추가한 브랜드입니다.');
    return;
  }
  category.brands.push(brandName);
  category.inputTag = '';
};

// [개인화 API 연동] 추천 브랜드를 눌러도 동일한 문자열 저장 목록에 추가한다.
const selectSuggestedBrand = (category, merchant) => {
  addBrand(category, merchant.name);
};

// [개인화 API 연동] 직접 입력 브랜드 태그를 화면과 저장 요청에서 제거한다.
const removeBrand = (category, index) => {
  category.brands.splice(index, 1);
};

onMounted(loadPersonalization);
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

</script>

<template>
  <div class="personalization-setting-view">
    <!-- 상단 헤더 -->
    <PageHeader title="개인화 설정" @back="goBack" />

    <div class="content-container">
      <!-- 안내 문구 -->
<<<<<<< HEAD
      <h1 class="guide-title">
        관심 카테고리
      </h1>

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
      <p class="guide-text">
        관심 소비 영역을 선택하고, 각 항목별 관심 브랜드를 설정해주세요.
      </p>

      <!-- 카테고리 리스트 -->
      <div class="category-list">
        <div
<<<<<<< HEAD
          v-for="category in categories"
          :key="category.key"
          class="category-item"
          :class="{ 'is-active': category.checked }"
        >
          <!-- 카테고리 헤더 -->
          <div class="category-header">
            <AppCheckbox v-model="category.checked" :label="category.label" />

            <span class="limit-text">
              3개 중 {{ category.tags.length }}개 사용 중
=======
          v-for="category in categoryGroups.flatMap((group) => group.children)"
          :key="category.categoryId"
          class="category-item"
        >
          <!-- 대분류명은 각 그룹의 첫 소분류 앞에 표시한다. -->
          <h2 v-if="category.isFirstInGroup">{{ category.groupName }}</h2>

          <!-- 카테고리 헤더 -->
          <div class="category-header">
            <AppCheckbox
              v-model="category.selected"
              :label="category.categoryName"
            />

            <span class="limit-text">
              브랜드 3개 중 {{ category.brands.length }}개 사용 중
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
            </span>
          </div>

          <!-- 체크한 카테고리만 상세 영역 표시 -->
<<<<<<< HEAD
          <div v-if="category.checked" class="tag-input-section">
            <!-- 등록 브랜드 -->
            <div class="tag-chips">
              <span
                v-for="(tag, index) in category.tags"
                :key="index"
=======
          <div v-if="category.selected" class="tag-input-section">
            <!-- 등록 브랜드 -->
            <div class="tag-chips">
              <span
                v-for="(tag, index) in category.brands"
                :key="`${category.categoryId}-${tag}`"
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
                class="tag-chip"
              >
                {{ tag }}

                <span
<<<<<<< HEAD
                  class="remove-tag"
                  @click="removeTag(category, index)"
                >
                  ×
=======
                  class="material-icons remove-tag"
                  @click="removeBrand(category, index)"
                >
                  close
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
                </span>
              </span>
            </div>

           <!-- 브랜드 검색 -->
<div
<<<<<<< HEAD
  v-if="category.tags.length < 3"
  class="brand-search-area"
>
  <AppInput
    v-model="category.inputTag"
    placeholder="브랜드 입력"
=======
  v-if="category.brands.length < 3"
  class="brand-search-area"
>
  <!-- [개인화 API 연동] Enter 입력으로 merchant에 없는 브랜드도 문자열 태그로 추가한다. -->
  <AppInput
    v-model="category.inputTag"
    placeholder="브랜드 입력"
    @keyup.enter="addBrand(category)"
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  />

<!-- 자동완성 -->
<div
  v-if="getSuggestions(category).length"
  class="suggestion-list"
>

  <div
    v-for="brand in getSuggestions(category)"
    :key="brand.name"
    class="suggestion-item"
<<<<<<< HEAD
    @click="selectBrand(category, brand)"
  >

    <!-- 실제 브랜드명 (하이라이트) -->
    <div class="brand-name">
      <span v-html="highlightAlias(brand.name, category.inputTag)"></span>
=======
    @click="selectSuggestedBrand(category, brand)"
  >

    <!-- 실제 브랜드명 -->
    <div class="brand-name">
      {{ brand.name }}
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
    </div>


    <!-- 검색 alias 표시 -->
    <div
      v-if="
        getMatchedAlias(
          brand,
          category.inputTag
        ) !== brand.name
      "
      class="brand-alias"
    >

      (
      <span
        v-html="
          highlightAlias(
            getMatchedAlias(
              brand,
              category.inputTag
            ),
            category.inputTag
          )
        "
      ></span>
      )

    </div>

  </div>

</div>

</div>
          </div>
        </div>
      </div>

      <!-- 완료 버튼 -->
      <div class="footer-button-area">
<<<<<<< HEAD
        <AppButton text="개인화 설정 완료" @click="savePersonalization" />
=======
        <AppButton
          text="개인화 설정 완료"
          :disabled="isSaving"
          @click="savePersonalization"
        />
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
      </div>
    </div>
  </div>
</template>

<style scoped>

.brand-alias {
  margin-top: 3px;
<<<<<<< HEAD
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
}

.brand-alias :deep(strong) {
  color: var(--color-gold-text);
  font-weight: var(--font-bold);
=======
  font-size: 0.75rem;
  color: #999;
}

.brand-alias strong {
  color: #6c5ce7;
  font-weight: 700;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

/* 개인화 설정 전체 화면 */
.personalization-setting-view {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
<<<<<<< HEAD
  background-color: var(--color-bg);
  margin: 0 auto;
  max-width: 480px;
  box-sizing: border-box;
=======
  background-color: #f9f9f9;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

/* 본문 영역 */
.content-container {
  flex: 1;
<<<<<<< HEAD
  padding: var(--space-md);
  padding-bottom: 120px;
=======
  padding: 20px;
  padding-bottom: 110px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  /*
    하단 고정 버튼 영역과 겹치지 않도록 여유 공간 확보
    (모바일 화면 기준)
  */
}

<<<<<<< HEAD
/* 안내 타이틀 (Display Typography) */
.guide-title {
  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  line-height: var(--typo-display-medium-line-height);
  letter-spacing: var(--typo-display-medium-letter-spacing);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-xs);
}

/* 안내 문구 */
.guide-text {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin: 0 0 var(--space-xl);
}

/* 카테고리 카드 영역 (Bento: 카드별 독립 배치) */
.category-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

/* 카테고리 한 줄 (미선택: 얇은 리스트 행) */
.category-item {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  transition: var(--transition-fast);
}

/* 선택된 카테고리 (Bento 강조 + Soft Glassmorphism) */
.category-item.is-active {
  padding: var(--space-md);
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.12) 0%, rgba(var(--color-primary-dark-rgb), 0.04) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.22);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

[data-theme="dark"] .category-item.is-active {
  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.22) 0%, rgba(var(--color-primary-dark-rgb), 0.08) 100%);
  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.06);
=======
/* 안내 문구 */
.guide-text {
  font-size: 0.9rem;
  color: #666;
  line-height: 1.5;
  margin-bottom: 20px;
}

/* 카테고리 카드 영역 */
.category-list {
  background-color: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
}

/* 카테고리 한 줄 */
.category-item {
  padding: 16px;
  border-bottom: 1px solid #eee;
}

/* 마지막 항목은 구분선 제거 */
.category-item:last-child {
  border-bottom: none;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

/* 카테고리명 + 개수 표시 영역 */
.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 태그 개수 표시 */
.limit-text {
<<<<<<< HEAD
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
=======
  font-size: 0.8rem;
  color: #888;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

/*
  브랜드 입력 영역

  체크된 카테고리 중
  hasTags=true인 항목만 표시
*/
.tag-input-section {
<<<<<<< HEAD
  margin-top: var(--space-sm);
  padding-left: var(--space-md);
=======
  margin-top: 14px;
  padding-left: 24px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

/* 태그 목록 */
.tag-chips {
  display: flex;
  flex-wrap: wrap;
<<<<<<< HEAD
  gap: var(--space-xs);
  margin-bottom: var(--space-sm);
}

/* 태그 하나 (Soft Glassmorphism, 골드 톤) */
=======
  gap: 8px;
  margin-bottom: 12px;
}

/* 태그 하나 */
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
.tag-chip {
  display: inline-flex;
  align-items: center;

<<<<<<< HEAD
  padding: var(--space-xxs) var(--space-sm);

  background: linear-gradient(135deg, rgba(230, 217, 77, 0.22) 0%, rgba(230, 217, 77, 0.08) 100%);
  border: 1px solid rgba(230, 217, 77, 0.35);
  border-radius: var(--radius-full);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);

  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
}

[data-theme="dark"] .tag-chip {
  background: linear-gradient(135deg, rgba(228, 218, 103, 0.22) 0%, rgba(228, 218, 103, 0.08) 100%);
  border: 1px solid rgba(228, 218, 103, 0.3);
=======
  padding: 6px 10px;

  background-color: #f1f1f1;
  border-radius: 16px;

  font-size: 0.85rem;
  color: #333;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

/* 태그 삭제 아이콘 */
.remove-tag {
<<<<<<< HEAD
  margin-left: var(--space-xxs);

  font-size: var(--font-sm);
  line-height: 1;
  color: var(--color-text-tertiary);
=======
  margin-left: 5px;

  font-size: 1rem;
  color: #999;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  cursor: pointer;
}

.remove-tag:hover {
<<<<<<< HEAD
  color: var(--color-text-primary);
=======
  color: #333;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

/* 입력창 + 추가 버튼 */
.input-with-button {
  display: flex;
  align-items: center;
<<<<<<< HEAD
  gap: var(--space-xs);
}

/* 브랜드 자동완성 영역 (Soft Glassmorphism) */
.suggestion-list {
  width: 100%;

  margin-top: var(--space-xs);

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.5) 0%, rgba(255, 255, 255, 0.2) 100%);

  border-radius: var(--radius-md);

  border: 1px solid rgba(255, 255, 255, 0.3);

  overflow: hidden;

  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.4);

  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

[data-theme="dark"] .suggestion-list {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.suggestion-item {
  padding: var(--space-sm);

  font-size: var(--font-sm);

  color: var(--color-text-primary);
=======
  gap: 8px;
}

/* 브랜드 자동완성 영역 */
.suggestion-list {
  width: 100%;

  margin-top: 8px;

  background: white;

  border-radius: 10px;

  border: 1px solid #eee;

  overflow: hidden;

  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.suggestion-item {
  padding: 12px 14px;

  font-size: 0.9rem;

  color: #333;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  cursor: pointer;
}

.suggestion-item + .suggestion-item {
<<<<<<< HEAD
  border-top: 1px solid var(--color-border);
}

.suggestion-item:hover {
  background: rgba(255, 255, 255, 0.15);
}

[data-theme="dark"] .suggestion-item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.suggestion-item :deep(strong) {
  color: var(--color-gold-text);

  font-weight: var(--font-bold);
}

.brand-name :deep(strong) {
  color: var(--color-gold-text);

  font-weight: var(--font-bold);
=======
  border-top: 1px solid #f3f3f3;
}

.suggestion-item:hover {
  background: #f8f8f8;
}

.suggestion-item strong {
  color: #6c5ce7;

  font-weight: 700;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

/*
  하단 고정 완료 버튼

  앱 화면에서 항상 보이도록 고정
*/
.footer-button-area {
  position: fixed;

  bottom: 0;
  left: 0;

  width: 100%;

<<<<<<< HEAD
  padding: var(--space-sm) var(--space-md);

  background: linear-gradient(180deg, rgba(255, 255, 255, 0.5) 0%, var(--color-surface) 40%);

  border-top: 1px solid rgba(255, 255, 255, 0.3);

  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.05);

  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
=======
  padding: 15px 20px;

  background-color: white;

  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  box-sizing: border-box;
}

<<<<<<< HEAD
[data-theme="dark"] .footer-button-area {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.04) 0%, var(--color-surface) 40%);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.3);
}

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
/*
  모바일 화면에서
  버튼이 너무 붙지 않도록 처리
*/
@media (max-width: 480px) {
  .content-container {
<<<<<<< HEAD
    padding: var(--space-md);
    padding-bottom: 100px;
  }

  .guide-title {
    font-size: 24px;
  }

  .category-item {
    padding: var(--space-sm);
  }

  .category-item.is-active {
    padding: var(--space-md);
  }

  .guide-text {
    font-size: var(--font-xs);
  }
}
=======
    padding: 16px;
    padding-bottom: 100px;
  }

  .category-item {
    padding: 14px;
  }

  .guide-text {
    font-size: 0.85rem;
  }
}

.suggestion-list {
  margin-top: 8px;

  background: white;

  border: 1px solid #eee;

  border-radius: 8px;

  overflow: hidden; 
}

.suggestion-item {
  padding: 12px;

  cursor: pointer;

  font-size: 0.9rem;
}

.suggestion-item:hover {
  background: #f7f7f7;
}

.suggestion-item strong {
  color: #6c5ce7;

  font-weight: 700;
}
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</style>
<!-- 07_25 연동 변경: 기존 개인화 UI를 유지하면서 설정 조회·저장 API를 연결한다. -->
