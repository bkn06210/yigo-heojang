<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  getPersonalization,
  updatePersonalization,
} from '@/api/personalizationApi';

import PageHeader from '@/components/common/PageHeader.vue';
import AppButton from '@/components/common/AppButton.vue';
import AppCheckbox from '@/components/common/AppCheckbox.vue';
import AppInput from '@/components/common/AppInput.vue';

const router = useRouter();
const isSaving = ref(false);

// 기존 category 테이블의 대분류·소분류·가맹점 계층을 API 응답 그대로 보관한다.
const categoryGroups = ref([]);

// 검색 결과
const getSuggestions = (category) => {
  const keyword = category.inputTag.trim().toLowerCase();

  if (!keyword) return [];

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
 */
const highlightAlias = (alias, keyword) => {
  if (!alias || !keyword) return alias;

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

/**
 * 뒤로가기
 */
const goBack = () => {
  router.go(-1);
};

/**
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
};

/**
 * 개인화 설정 저장
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

</script>

<template>
  <div class="personalization-setting-view">
    <!-- 상단 헤더 -->
    <PageHeader title="개인화 설정" @back="goBack" />

    <div class="content-container">
      <!-- 안내 문구 -->
      <p class="guide-text">
        관심 소비 영역을 선택하고, 각 항목별 관심 브랜드를 설정해주세요.
      </p>

      <!-- 카테고리 리스트 -->
      <div class="category-list">
        <div
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
            </span>
          </div>

          <!-- 체크한 카테고리만 상세 영역 표시 -->
          <div v-if="category.selected" class="tag-input-section">
            <!-- 등록 브랜드 -->
            <div class="tag-chips">
              <span
                v-for="(tag, index) in category.brands"
                :key="`${category.categoryId}-${tag}`"
                class="tag-chip"
              >
                {{ tag }}

                <span
                  class="material-icons remove-tag"
                  @click="removeBrand(category, index)"
                >
                  close
                </span>
              </span>
            </div>

           <!-- 브랜드 검색 -->
<div
  v-if="category.brands.length < 3"
  class="brand-search-area"
>
  <!-- [개인화 API 연동] Enter 입력으로 merchant에 없는 브랜드도 문자열 태그로 추가한다. -->
  <AppInput
    v-model="category.inputTag"
    placeholder="브랜드 입력"
    @keyup.enter="addBrand(category)"
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
    @click="selectSuggestedBrand(category, brand)"
  >

    <!-- 실제 브랜드명 -->
    <div class="brand-name">
      {{ brand.name }}
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
        <AppButton
          text="개인화 설정 완료"
          :disabled="isSaving"
          @click="savePersonalization"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>

.brand-alias {
  margin-top: 3px;
  font-size: 0.75rem;
  color: #999;
}

.brand-alias strong {
  color: #6c5ce7;
  font-weight: 700;
}

/* 개인화 설정 전체 화면 */
.personalization-setting-view {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f9f9f9;
}

/* 본문 영역 */
.content-container {
  flex: 1;
  padding: 20px;
  padding-bottom: 110px;
  /*
    하단 고정 버튼 영역과 겹치지 않도록 여유 공간 확보
    (모바일 화면 기준)
  */
}

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
}

/* 카테고리명 + 개수 표시 영역 */
.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 태그 개수 표시 */
.limit-text {
  font-size: 0.8rem;
  color: #888;
}

/*
  브랜드 입력 영역

  체크된 카테고리 중
  hasTags=true인 항목만 표시
*/
.tag-input-section {
  margin-top: 14px;
  padding-left: 24px;
}

/* 태그 목록 */
.tag-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

/* 태그 하나 */
.tag-chip {
  display: inline-flex;
  align-items: center;

  padding: 6px 10px;

  background-color: #f1f1f1;
  border-radius: 16px;

  font-size: 0.85rem;
  color: #333;
}

/* 태그 삭제 아이콘 */
.remove-tag {
  margin-left: 5px;

  font-size: 1rem;
  color: #999;

  cursor: pointer;
}

.remove-tag:hover {
  color: #333;
}

/* 입력창 + 추가 버튼 */
.input-with-button {
  display: flex;
  align-items: center;
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

  cursor: pointer;
}

.suggestion-item + .suggestion-item {
  border-top: 1px solid #f3f3f3;
}

.suggestion-item:hover {
  background: #f8f8f8;
}

.suggestion-item strong {
  color: #6c5ce7;

  font-weight: 700;
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

  padding: 15px 20px;

  background-color: white;

  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);

  box-sizing: border-box;
}

/*
  모바일 화면에서
  버튼이 너무 붙지 않도록 처리
*/
@media (max-width: 480px) {
  .content-container {
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
</style>
<!-- 07_25 연동 변경: 기존 개인화 UI를 유지하면서 설정 조회·저장 API를 연결한다. -->
