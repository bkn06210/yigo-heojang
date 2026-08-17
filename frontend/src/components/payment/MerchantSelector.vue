<script setup>

import { ref, computed } from 'vue';
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

const emit = defineEmits([
  'update:category',
  'update:merchant'
]);

// 검색 인풋과 모드
const searchInput = ref('');
const showSearchInput = ref(false);
// 사용자가 직접 입력한 임시 가맹점
const customMerchants = ref({});

// Store에서 활성화된 카테고리만 가져오기
const activeCategories = computed(() => {
  return personalizationStore.getActiveCategories();
});

// Store의 카테고리 정보를 merchants로 변환
const getMerchants = () => {
  const result = {};
  personalizationStore.getActiveCategories().forEach(category => {
    // 기본 가맹점
    result[category.label] = [...category.tags];
    // 커스텀 가맹점 추가
    if (customMerchants.value[category.label]) {
      result[category.label] = [
        ...result[category.label],
        ...customMerchants.value[category.label]
      ];
    }
  });
  return result;
};

const merchants = computed(() => getMerchants());

// 한글 초성만 추출
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
      result += char;
    } else {
      result += char;
    }
  }
  return result;
};

// 검색 결과 필터링
const getSuggestions = () => {
  const keyword = searchInput.value.trim().toLowerCase();

  if (!keyword || !props.category) return [];

  const keywordChoseong = extractChoseong(keyword);
  const merchantList = merchants.value[props.category] || [];

  return merchantList.filter((merchant) => {
    const lowerMerchant = merchant.toLowerCase();
    const merchantChoseong = extractChoseong(lowerMerchant);

    return (
      lowerMerchant.includes(keyword) ||
      merchantChoseong.includes(keywordChoseong) ||
      merchantChoseong.includes(keyword)
    );
  });
};

// 하이라이트 처리
const highlightMatch = (text, keyword) => {
  if (!text || !keyword) return text;

  const lowerKeyword = keyword.toLowerCase();
  const lowerText = text.toLowerCase();
  const keywordChoseong = extractChoseong(lowerKeyword);
  const textChoseong = extractChoseong(lowerText);

  // 완성된 한글로 직접 매칭
  if (lowerText.includes(lowerKeyword)) {
    const regex = new RegExp(
      `(${lowerKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`,
      'gi'
    );
    return text.replace(regex, '<strong>$1</strong>');
  }

  // 초성으로 매칭
  if (textChoseong.includes(keywordChoseong) || textChoseong.includes(lowerKeyword)) {
    let result = '';
    let choseongIndex = 0;

    for (let i = 0; i < text.length; i++) {
      const char = text[i];
      const charChoseong = extractChoseong(char.toLowerCase());

      if (
        textChoseong.substring(choseongIndex, choseongIndex + keywordChoseong.length) === keywordChoseong ||
        textChoseong.substring(choseongIndex, choseongIndex + lowerKeyword.length) === lowerKeyword
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

  return text;
};

// 업종 선택
const selectCategory = (category) => {
  emit(
    'update:merchant',
    null
  );

  emit(
    'update:category',
    category
  );

  searchInput.value = '';
  showSearchInput.value = false;
};

// 가맹점 선택 (토글)
const selectMerchant = (merchant) => {
  // 이미 선택된 가맹점을 클릭하면 선택 취소
  if (props.merchant === merchant) {
    emit('update:merchant', null);
  } else {
    emit('update:merchant', merchant);
  }

  searchInput.value = '';
  showSearchInput.value = false;
};

// 검색 모드 토글
const toggleSearchMode = () => {
  showSearchInput.value = !showSearchInput.value;
  searchInput.value = '';
};

// 섹션 클릭 (검색 모드 종료)
const handleSectionClick = () => {
  if (showSearchInput.value) {
    showSearchInput.value = false;
    searchInput.value = '';
  }
};

// 검색어로 가맹점 추가
const addMerchantFromSearch = () => {
  const value = searchInput.value.trim();
  if (value && props.category) {
    // customMerchants에 추가
    if (!customMerchants.value[props.category]) {
      customMerchants.value[props.category] = [];
    }
    if (!customMerchants.value[props.category].includes(value)) {
      customMerchants.value[props.category].push(value);
    }
    selectMerchant(value);
  }
};

// 커스텀 가맹점 제거
const removeCustomMerchant = (merchantName) => {
  if (props.category && customMerchants.value[props.category]) {
    customMerchants.value[props.category] = customMerchants.value[props.category].filter(
      m => m !== merchantName
    );
    // 선택된 가맹점이 제거된 것이면 선택 취소
    if (props.merchant === merchantName) {
      emit('update:merchant', null);
    }
  }
};

// 커스텀 가맹점 여부 확인
const isCustomMerchant = (merchantName) => {
  return (
    customMerchants.value[props.category] &&
    customMerchants.value[props.category].includes(merchantName)
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
    </button>

  </div>

</section>



<!-- 가맹점 선택 영역 -->
<section
  v-if="category"
  class="merchant-section"
  @click="handleSectionClick"
>

  <h3>
    {{ category }} 가맹점 선택
  </h3>

  <!-- 가맹점 버튼들 (항상 표시) -->
  <div class="merchant-list">

    <button
      v-for="mct in merchants[category] || []"
      :key="mct"
      @click="selectMerchant(mct)"
      :class="{ active: mct === merchant }"
      class="merchant-button"
    >
      {{ mct }}
      <!-- 커스텀 가맹점에만 X 표시 -->
      <span v-if="isCustomMerchant(mct)" class="remove-icon" @click.stop="removeCustomMerchant(mct)">✕</span>
    </button>

    <!-- 기타 버튼 -->
    <button
      class="other-btn"
      @click.stop="toggleSearchMode"
      title="다른 가맹점 직접 입력"
    >
      + 기타
    </button>

  </div>

  <!-- 검색 모드 (가맹점 아래) -->
  <div
    v-if="showSearchInput"
    class="search-area-wrapper"
    @click.stop
  >
    <div class="search-input-group">
      <input
        v-model="searchInput"
        type="text"
        class="search-input"
        :placeholder="`${category} 검색`"
        @keyup.enter="addMerchantFromSearch"
      />
      <button
        class="close-btn"
        @click="toggleSearchMode"
        title="닫기"
      >
        ✕
      </button>
    </div>

    <!-- 자동완성 리스트 -->
    <div
      v-if="getSuggestions().length"
      class="suggestion-list"
    >
      <div
        v-for="suggestion in getSuggestions()"
        :key="suggestion"
        class="suggestion-item"
        @click="selectMerchant(suggestion)"
      >
        <span v-html="highlightMatch(suggestion, searchInput)"></span>
      </div>
    </div>
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

/* X 제거 아이콘 */
.remove-icon {
  margin-left: 2px;
  cursor: pointer;
  font-size: 10px;
  opacity: 0.6;
  transition: var(--transition-fast);
  vertical-align: middle;
  display: inline-block;
  line-height: 1;
}

.merchant-button:hover .remove-icon {
  opacity: 1;
}

/* 기타 버튼 */
.other-btn {
  border-style: dashed;
  opacity: 0.6;
  transition: var(--transition-fast);
}

.other-btn:hover {
  opacity: 1;
  background: var(--color-bg);
}

/* 검색 영역 */
.search-area-wrapper {
  margin-top: var(--space-md);
  margin-bottom: var(--space-md);
}

.search-input-group {
  display: flex;
  gap: var(--space-xs);
  align-items: center;
  position: relative;
}

.search-input {
  flex: 1;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  font-size: var(--font-sm);
  color: var(--color-text-primary);
  box-sizing: border-box;
  transition: var(--transition-fast);
}

.search-input:focus {
  outline: none;
  border-color: var(--color-primary-dark);
  background: var(--color-bg);
}

[data-theme="dark"] .search-input:focus {
  border-color: rgba(214, 186, 110, 0.6);
}

.close-btn {
  padding: var(--space-xs) var(--space-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--font-sm);
  cursor: pointer;
  transition: var(--transition-fast);
  flex-shrink: 0;
}

.close-btn:hover {
  background: var(--color-bg);
  color: var(--color-text-primary);
}

/* 자동완성 리스트 */
.suggestion-list {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-top: none;
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
  margin-top: -1px;
}

.suggestion-item {
  padding: var(--space-sm) var(--space-md);
  font-size: var(--font-sm);
  color: var(--color-text-primary);
  cursor: pointer;
  transition: var(--transition-fast);
  border-bottom: 1px solid rgba(var(--color-border-rgb), 0.5);
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-item:hover {
  background: var(--color-bg);
}

.suggestion-item strong {
  color: var(--color-primary-dark);
  font-weight: var(--font-bold);
}

/* 다크모드 */
[data-theme="dark"] .merchant-chip.active {
  background: linear-gradient(135deg, rgba(214, 186, 110, 0.24) 0%, rgba(214, 186, 110, 0.08) 100%);
  border: 1px solid rgba(214, 186, 110, 0.45);
}

[data-theme="dark"] .merchant-chip.active .merchant-btn {
  color: var(--color-primary-dark);
}

</style>