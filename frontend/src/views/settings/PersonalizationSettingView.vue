<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePersonalizationStore } from '@/stores/personalization';
import { getPersonalization, updatePersonalization } from '@/api/personalizationApi';

import PageHeader from '@/components/common/PageHeader.vue';
import AppButton from '@/components/common/AppButton.vue';
import AppCheckbox from '@/components/common/AppCheckbox.vue';
import AppInput from '@/components/common/AppInput.vue';
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
const getSuggestions = (category) => {
  const keyword = category.inputTag.trim().toLowerCase();

  if (!keyword) return [];

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
 */
const highlightAlias = (alias, keyword) => {
  if (!alias || !keyword) return alias;

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

/**
 * 뒤로가기
 */
const goBack = () => {
  router.go(-1);
};

/**
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
};

// API에서 개인화 설정 조회 후 store 초기화
const loadPersonalization = async () => {
  try {
    const response = await getPersonalization();

    // API 응답의 그룹/서브카테고리 구조를 store 형식으로 변환
    const categoryMap = new Map();
    if (response?.groups) {
      for (const group of response.groups) {
        for (const child of group.children) {
          categoryMap.set(child.categoryCode, {
            id: child.categoryId,
            checked: child.selected,
            tags: child.brands || [],
          });
        }
      }
    }

    // store의 categories를 업데이트
    categories.value = categories.value.map(cat => ({
      ...cat,
      id: categoryMap.get(cat.key)?.id || null,
      checked: categoryMap.get(cat.key)?.checked || false,
      tags: categoryMap.get(cat.key)?.tags || [],
    }));
  } catch (error) {
    console.error('개인화 설정 조회 실패:', error);
  }
};

const savePersonalization = async () => {
  try {
    // 선택된 카테고리의 ID만 수집
    const categoryIds = categories.value
      .filter(c => c.checked && c.id)
      .map(c => c.id);

    // 모든 브랜드를 배열로 변환 (categoryId + brandName)
    const brands = [];
    for (const category of categories.value) {
      if (category.checked && category.id) {
        for (const brand of category.tags) {
          brands.push({
            categoryId: category.id,
            brandName: brand,
          });
        }
      }
    }

    await updatePersonalization({ categoryIds, brands });
    personalizationStore.updateCategories(categories.value);
    showToast('success', '개인화 설정이 저장되었습니다.');
    router.go(-1);
  } catch (error) {
    console.error('개인화 설정 저장 실패:', error);
    showToast('error', error?.message || '개인화 설정 저장에 실패했습니다.');
  }
};

// 초기 로드
onMounted(() => {
  loadPersonalization();
});


</script>

<template>
  <div class="personalization-setting-view">
    <!-- 상단 헤더 -->
    <PageHeader title="개인화 설정" @back="goBack" />

    <div class="content-container">
      <!-- 안내 문구 -->
      <h1 class="guide-title">
        관심 카테고리
      </h1>

      <p class="guide-text">
        관심 소비 영역을 선택하고, 각 항목별 관심 브랜드를 설정해주세요.
      </p>

      <!-- 카테고리 리스트 -->
      <div class="category-list">
        <div
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
            </span>
          </div>

          <!-- 체크한 카테고리만 상세 영역 표시 -->
          <div v-if="category.checked" class="tag-input-section">
            <!-- 등록 브랜드 -->
            <div class="tag-chips">
              <span
                v-for="(tag, index) in category.tags"
                :key="index"
                class="tag-chip"
              >
                {{ tag }}

                <span
                  class="remove-tag"
                  @click="removeTag(category, index)"
                >
                  ×
                </span>
              </span>
            </div>

           <!-- 브랜드 검색 -->
<div
  v-if="category.tags.length < 3"
  class="brand-search-area"
>
  <AppInput
    v-model="category.inputTag"
    placeholder="브랜드 입력"
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
    @click="selectBrand(category, brand)"
  >

    <!-- 실제 브랜드명 (하이라이트) -->
    <div class="brand-name">
      <span v-html="highlightAlias(brand.name, category.inputTag)"></span>
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
        <AppButton text="개인화 설정 완료" @click="savePersonalization" />
      </div>
    </div>
  </div>
</template>

<style scoped>

.brand-alias {
  margin-top: 3px;
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
}

.brand-alias :deep(strong) {
  color: var(--color-gold-text);
  font-weight: var(--font-bold);
}

/* 개인화 설정 전체 화면 */
.personalization-setting-view {
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--color-bg);
  box-sizing: border-box;
}

/* 본문 영역 */
.content-container {
  flex: 1;
  padding: var(--space-md);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
  /*
    하단 고정 버튼 영역과 겹치지 않도록 여유 공간 확보
    (모바일 화면 기준)
  */
}

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
}

/* 카테고리명 + 개수 표시 영역 */
.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 태그 개수 표시 */
.limit-text {
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
}

/*
  브랜드 입력 영역

  체크된 카테고리 중
  hasTags=true인 항목만 표시
*/
.tag-input-section {
  margin-top: var(--space-sm);
  padding-left: var(--space-md);
}

/* 태그 목록 */
.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
  margin-bottom: var(--space-sm);
}

/* 태그 하나 (Soft Glassmorphism, 골드 톤) */
.tag-chip {
  display: inline-flex;
  align-items: center;

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
}

/* 태그 삭제 아이콘 */
.remove-tag {
  margin-left: var(--space-xxs);

  font-size: var(--font-sm);
  line-height: 1;
  color: var(--color-text-tertiary);

  cursor: pointer;
}

.remove-tag:hover {
  color: var(--color-text-primary);
}

/* 입력창 + 추가 버튼 */
.input-with-button {
  display: flex;
  align-items: center;
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

  cursor: pointer;
}

.suggestion-item + .suggestion-item {
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
}

/*
  하단 고정 완료 버튼

  앱 화면에서 항상 보이도록 고정
*/
.footer-button-area {
  position: fixed;

  bottom: 0;
  left: 50%;
  transform: translateX(-50%);

  width: 100%;
  max-width: 480px;

  padding: var(--space-sm) var(--space-md);

  background: linear-gradient(180deg, rgba(255, 255, 255, 0.5) 0%, var(--color-surface) 40%);

  border-top: 1px solid rgba(255, 255, 255, 0.3);

  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.05);

  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);

  box-sizing: border-box;
}

[data-theme="dark"] .footer-button-area {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.04) 0%, var(--color-surface) 40%);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.3);
}

/*
  모바일 화면에서
  버튼이 너무 붙지 않도록 처리
*/
@media (max-width: 480px) {
  .content-container {
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
</style>
