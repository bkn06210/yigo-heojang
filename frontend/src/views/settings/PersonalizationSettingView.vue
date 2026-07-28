<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import PageHeader from '@/components/common/PageHeader.vue';
import AppButton from '@/components/common/AppButton.vue';
import AppCheckbox from '@/components/common/AppCheckbox.vue';
import AppInput from '@/components/common/AppInput.vue';

const router = useRouter();

/**
 * 개인화 설정 카테고리 상태
 * - checked : 관심 영역 선택 여부
 * - tags : 세부 브랜드/키워드
 * - hasTags : 직접 입력 가능한 카테고리 여부
 */
const categories = ref([
  {
    key: 'cafe',
    label: '카페',
    checked: true,
    hasTags: true,
    tags: ['스타벅스'],
    inputTag: '',
  },
  {
    key: 'convenience',
    label: '편의점',
    checked: true,
    hasTags: true,
    tags: ['GS25', 'CU', '이마트24'],
    inputTag: '',
  },
  {
    key: 'transport',
    label: '교통',
    checked: false,
    hasTags: false,
    tags: [],
    inputTag: '',
  },
  {
    key: 'telecom',
    label: '통신',
    checked: false,
    hasTags: false,
    tags: [],
    inputTag: '',
  },
  {
    key: 'shopping',
    label: '온라인 쇼핑',
    checked: false,
    hasTags: false,
    tags: [],
    inputTag: '',
  },
  {
    key: 'mart',
    label: '마트',
    checked: false,
    hasTags: false,
    tags: [],
    inputTag: '',
  },
  {
    key: 'culture',
    label: '음식/카페',
    checked: false,
    hasTags: false,
    tags: [],
    inputTag: '',
  },
]);


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
    alert('최대 3개까지만 입력할 수 있습니다.');
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


/**
 * 개인화 설정 저장
 * TODO: 백엔드 API 연결 예정
 */
const savePersonalization = () => {
  console.log(
    '개인화 설정 저장:',
    categories.value
  );

  router.go(-1);
};

</script>


<template>
  <div class="personalization-setting-view">

    <!-- 상단 헤더 -->
    <PageHeader
      title="개인화 설정"
      @back="goBack"
    />


    <div class="content-container">

      <!-- 안내 문구 -->
      <p class="guide-text">
        관심 소비 영역을 선택하고,
        각 항목별 관심 브랜드를 설정해주세요.
      </p>


      <!-- 카테고리 리스트 -->
      <div class="category-list">


        <div
          v-for="category in categories"
          :key="category.key"
          class="category-item"
        >

          <!-- 카테고리 헤더 -->
          <div class="category-header">

            <AppCheckbox
              v-model="category.checked"
              :label="category.label"
            />


            <!-- 태그 사용 카테고리만 표시 -->
            <span
              v-if="category.hasTags"
              class="limit-text"
            >
              3개 중 {{ category.tags.length }}개 사용 중
            </span>

          </div>



          <!-- 상세 태그 입력 영역 -->
          <div
            v-if="category.checked && category.hasTags"
            class="tag-input-section"
          >

            <!-- 등록 태그 -->
            <div class="tag-chips">

              <span
                v-for="(tag,index) in category.tags"
                :key="index"
                class="tag-chip"
              >

                {{ tag }}

                <span
                  class="material-icons remove-tag"
                  @click="removeTag(category,index)"
                >
                  close
                </span>

              </span>

            </div>



            <!-- 태그 입력 -->
            <div
              v-if="category.tags.length < 3"
              class="input-with-button"
            >

              <AppInput
                v-model="category.inputTag"
                placeholder="브랜드 또는 키워드 입력"
                @keyup.enter="addTag(category)"
              />


              <AppButton
                variant="secondary"
                size="small"
                @click="addTag(category)"
              >
                <span class="material-icons">
                  add
                </span>
              </AppButton>

            </div>


          </div>


        </div>


      </div>



      <!-- 완료 버튼 -->
      <div class="footer-button-area">

        <AppButton
          @click="savePersonalization"
        >
          개인화 설정 완료
        </AppButton>

      </div>


    </div>

  </div>
</template>

<style scoped>
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
  브랜드/키워드 입력 영역

  체크된 카테고리 중
  hasTags=true인 항목만 표시
*/
tag-input-section {
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
</style>