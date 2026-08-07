<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

import PageHeader from '@/components/common/PageHeader.vue'
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMembershipProviders, registerMembership } from '@/api/walletApi'
import { getPartnerUsagePlaces } from '@/utils/partnerUsagePlaces'
import { getPartnerLogo } from '@/utils/partnerLogos'


const router = useRouter()


// 검색어
const keyword = ref('')


// 팝업 상태
const showModal = ref(false)


// 선택한 멤버십
const selectedMembership = ref(null)


// 임시 멤버십 데이터
// 추후 API 응답 데이터로 교체 예정
const membershipList = ref([
  {
    id: 1,
    name: 'CJ ONE',
    alias: ['cj one', 'cjone', '씨제이원', '씨제이'],
    mainUses: [
      '뚜레쥬르',
      '올리브영',
      'CGV'
    ]
  },
  {
    id: 2,
    name: '해피포인트',
    alias: ['happy point', 'happypoint'],
    mainUses: [
      '파리바게뜨',
      '던킨',
      '배스킨라빈스'
    ]
  },
  {
    id: 3,
    name: 'KT 멤버십',
    alias: ['kt membership', 'kt멤버십', '케이티', '케이티 멤버십'],
    mainUses: [
      '편의점',
      '영화관',
      '카페'
    ]
const membershipList = ref([])
const errorMessage = ref('')

const loadProviders = async () => {
  try {
    const data = await getMembershipProviders()
    membershipList.value = (data?.providers || data || [])
      .filter((provider) => !provider.isRegistered)
      .map((provider) => ({
        id: Number(provider.pointProviderId),
        name: provider.providerName,
        logo: getPartnerLogo(provider.providerName, provider.logoImageUrl),
        mainUses: getPartnerUsagePlaces(provider.providerName).slice(0, 3).map((place) => place.placeName),
      }))
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '제휴 멤버십을 불러오지 못했습니다.'
  }
}


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
    } else {
      result += char;
    }
  }
  return result;
};

// 이름 또는 alias가 검색어와 매칭되는지 확인 (완성형 + 초성)
const isMatched = (text, lowerKeyword, keywordChoseong) => {
  const lowerText = text.toLowerCase();
  const textChoseong = extractChoseong(lowerText);

  return (
    lowerText.includes(lowerKeyword) ||
    textChoseong.includes(keywordChoseong) ||
    textChoseong.includes(lowerKeyword)
  );
};

// 검색 결과 (완성형 + 초성 + 영문/한글 별칭 검색 모두 지원)
const searchResult = computed(() => {

  if (!keyword.value.trim()) {
    return []
  }

  const lowerKeyword = keyword.value.trim().toLowerCase();
  const keywordChoseong = extractChoseong(lowerKeyword);

  return membershipList.value.filter((membership) => {
    const nameMatched = isMatched(membership.name, lowerKeyword, keywordChoseong);
    const aliasMatched = (membership.alias || []).some((a) =>
      isMatched(a, lowerKeyword, keywordChoseong)
    );

    return nameMatched || aliasMatched;
  })

})

// 검색어와 매칭되는 alias 반환 (이름 자체와 다를 때 부가 표시용)
const getMatchedAlias = (membership, searchText) => {
  if (!searchText) return null;

  const lowerKeyword = searchText.trim().toLowerCase();
  const keywordChoseong = extractChoseong(lowerKeyword);

  return (membership.alias || []).find((a) =>
    isMatched(a, lowerKeyword, keywordChoseong)
  );
};

// 검색어 강조 표시 (완성형 + 초성 매칭 모두 지원)
const highlightKeyword = (name, searchText) => {

  if (!searchText) {
    return name
  }

  const lowerKeyword = searchText.toLowerCase();
  const lowerName = name.toLowerCase();

  // 완성형 직접 매칭
  if (lowerName.includes(lowerKeyword)) {
    const regex = new RegExp(
      `(${lowerKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`,
      'gi'
    );
    return name.replace(regex, '<span class="highlight">$1</span>');
  }

  // 초성 매칭
  const keywordChoseong = extractChoseong(lowerKeyword);
  const nameChoseong = extractChoseong(lowerName);

  if (nameChoseong.includes(keywordChoseong)) {
    let result = '';
    let choseongIndex = 0;

    for (let i = 0; i < name.length; i++) {
      const char = name[i];
      const charChoseong = extractChoseong(char.toLowerCase());

      if (
        nameChoseong.substring(choseongIndex, choseongIndex + keywordChoseong.length) === keywordChoseong
      ) {
        result += `<span class="highlight">${char}</span>`;
        choseongIndex += charChoseong.length;
      } else {
        result += char;
        choseongIndex += charChoseong.length;
      }
    }
    return result;
  }

  return name;

}


// 많이 사용하는 멤버십 — 상위 5개만 표시
const popularMemberships = computed(() => {
  return membershipList.value.slice(0, 5)
})


// 멤버십 추가 클릭
// TODO: 백엔드 연동 필요 - 현재는 완료 팝업만 띄우고 실제로 저장하지 않음.
// PointListView.vue의 membershipList(로컬 빈 배열)와 연결되어 있지 않아서
// 여기서 "추가"를 눌러도 혜택 목록 페이지에는 반영되지 않음.
// 혜택 API 연동 작업(다른 팀원 담당)이 끝나면, 그 응답을 받아오는 방식으로 교체할 것.
const addMembership = (membership) => {

  selectedMembership.value = membership

  showModal.value = true

const addMembership = async (membership) => {
  try {
    await registerMembership(membership.id)
    selectedMembership.value = membership
    showModal.value = true
    membershipList.value = membershipList.value.filter((item) => item.id !== membership.id)
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || error?.message || '멤버십 추가에 실패했습니다.'
  }
}


// 팝업 닫기
const closeModal = () => {

  showModal.value = false

  selectedMembership.value = null

  keyword.value = ''

}

// 멤버십 목록 이동
const goMembershipList = () => {

  showModal.value = false

  keyword.value = ''

  router.push('/points')

}

//선택 함수 추가
const selectedSearchMembership = ref(null)

const selectMembership = (membership) => {

  selectedSearchMembership.value = membership

  keyword.value = membership.name

}

// 자동완성 선택
const selectSuggestion = (membership) => {

  keyword.value = membership.name

}

onMounted(loadProviders)

</script>


<template>

  <div class="membership-register">

    <!-- 헤더 -->
    <PageHeader title="멤버십 추가" @back="router.back()" />



    <!-- 안내 문구 -->
    <p class="description">
      자주 사용하는 멤버십을 추가해보세요
    </p>

    <p v-if="errorMessage" class="description">{{ errorMessage }}</p>



    <!-- 검색 -->
    <div class="search-box">

      <input
       :value="keyword"
        @input="keyword = $event.target.value"
         placeholder="멤버십 이름을 입력해주세요"
      />

    </div>



   <!-- 자동완성 제안 -->
<div
  v-if="searchResult.length"
  class="suggestion-box"
>

  <div
  v-for="membership in searchResult"
  :key="membership.id"
  class="suggestion-item"
  @click="selectSuggestion(membership)"
>

    <div class="suggestion-text">
      <span
        v-html="highlightKeyword(membership.name, keyword)"
      ></span>

      <span
        v-if="getMatchedAlias(membership, keyword)"
        class="suggestion-alias"
      >
        (
        <span
          v-html="highlightKeyword(getMatchedAlias(membership, keyword), keyword)"
        ></span>
        )
      </span>
    </div>


    <button
      @click.stop="addMembership(membership)"
    >
      추가
    </button>

  </div>

</div>




    <!-- 인기 멤버십 -->
    <section class="section">


      <h2>
        많이 사용하는 멤버십
      </h2>


      <div
        v-for="(membership, index) in popularMemberships"
        :key="membership.id"
        class="membership-item"
        :class="{ 'is-featured': index === 0 }"
      >

        <span class="membership-label">
          <img
            v-if="membership.logo"
            :src="membership.logo"
            :alt="`${membership.name} 로고`"
            class="membership-logo"
          />
          <span>{{ membership.name }}</span>
        </span>


        <button
          @click="addMembership(membership)"
        >
          추가
        </button>


      </div>


    </section>




    <!-- 완료 팝업 -->
    <div
      v-if="showModal"
      class="modal-background"
    >

      <div class="modal">


        <h2>
          {{ selectedMembership.name }} 추가 완료!
        </h2>



        <p class="modal-title">
          주요 사용처
        </p>


        <ul>

          <li
            v-for="use in selectedMembership.mainUses"
            :key="use"
          >
            {{ use }}
          </li>

        </ul>


        <button
  class="confirm-button"
  @click="goMembershipList"
>
  멤버십 보러 가기
</button>


<button
  class="more-button"
  @click="closeModal"
>
  다른 멤버십 추가
</button>


      </div>

    </div>



  </div>

</template>



<style scoped>

.membership-register {

  padding: var(--space-lg);

  padding-bottom: var(--space-2xl);
  margin: 0 auto;
  max-width: 480px;
  box-sizing: border-box;

}



/* 타이틀 (Display Typography) */
h1 {

  font-size: var(--typo-display-medium-size);

  font-weight: var(--typo-display-medium-weight);

  line-height: var(--typo-display-medium-line-height);

  letter-spacing: var(--typo-display-medium-letter-spacing);

  color: var(--color-text-primary);

}



.description {

  margin: var(--space-xs) 0 var(--space-xl);

  color: var(--color-text-secondary);

  font-size: var(--font-sm);

}



.search-box {

  width: 100%;

  box-sizing: border-box;

}

.search-box input {

  width: 100%;

  height: 48px;

  padding: 0 var(--space-md);

  border-radius: var(--radius-md);

  border: 1px solid var(--color-input-border);

  background: var(--color-surface);

  color: var(--color-text-primary);

  box-sizing: border-box;

  transition: var(--transition-fast);

}

.search-box input:focus {

  outline: none;

  border-color: var(--color-input-focus);

}

.search-box input::placeholder {

  color: var(--color-text-tertiary);

}


.section {

  margin-top: var(--space-2xl);

}



h2 {

  margin: 0 0 var(--space-md);

  color: var(--color-text-primary);

  font-size: var(--font-lg);

  font-weight: var(--font-bold);

  letter-spacing: -0.3px;

}



/* 멤버십 카드 (Bento: 차등 크기) */
.membership-item {

  display: flex;

  justify-content: space-between;

  align-items: center;

  padding: var(--space-md);

  margin-bottom: var(--space-sm);

  border-radius: var(--radius-md);

  background: var(--color-surface);

  border: 1px solid var(--color-border);

  color: var(--color-text-primary);

  transition: var(--transition-fast);

}

/* 1위 멤버십 (Soft Glassmorphism 강조) */
.membership-item.is-featured {

  padding: var(--space-lg);

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.12) 0%, rgba(var(--color-primary-dark-rgb), 0.04) 100%);

  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.22);

  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.4);

  backdrop-filter: blur(10px);

  -webkit-backdrop-filter: blur(10px);

}

.membership-item.is-featured span {

  font-weight: var(--font-bold);

}

[data-theme="dark"] .membership-item.is-featured {

  background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.22) 0%, rgba(var(--color-primary-dark-rgb), 0.08) 100%);

  border: 1px solid rgba(var(--color-primary-dark-rgb), 0.3);

  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.06);

}

.membership-label {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.membership-logo {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  border-radius: 12px;
  object-fit: contain;
  background: #fff;
}



button {

  border: none;

  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));

  color: var(--color-btn-primary-text);

  border-radius: var(--radius-xs);

  padding: var(--space-xs) var(--space-sm);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

}

button:hover {

  opacity: 0.9;

}



.modal-background {

  position: fixed;

  inset: 0;

  background: rgba(0,0,0,0.4);

  display: flex;

  justify-content: center;

  align-items: center;

  z-index: var(--z-modal);

}



.modal {

  width: 80%;

  border-radius: var(--radius-lg);

  padding: var(--space-xl);

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, var(--color-surface) 60%);

  border: 1px solid rgba(255, 255, 255, 0.3);

  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.4);

  backdrop-filter: blur(12px);

  -webkit-backdrop-filter: blur(12px);

}

[data-theme="dark"] .modal {

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, var(--color-surface) 60%);

  border: 1px solid rgba(255, 255, 255, 0.08);

  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.05);

}

.modal h2 {

  color: var(--color-text-primary);

  font-weight: var(--font-bold);

}

.modal ul {

  color: var(--color-text-primary);

  list-style: none;

  padding: 0;

}

.modal li {

  padding: var(--space-xs) 0;

}


.modal-title {

  margin-top: var(--space-lg);

  font-weight: var(--font-semibold);

  color: var(--color-text-primary);

}



.confirm-button {
  width: 100%;

  height: 48px;

  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));

  color: var(--color-btn-primary-text);

  border: none;

  font-weight: var(--font-semibold);

  border-radius: var(--radius-md);

  cursor: pointer;

  transition: var(--transition-fast);

}

.confirm-button:hover {

  opacity: 0.9;

}

.more-button {

  width: 100%;

  height: 44px;

  margin-top: var(--space-xs);

  background: none;

  color: var(--color-text-secondary);

  border: none;

  font-weight: var(--font-medium);

  cursor: pointer;

  transition: var(--transition-fast);

}

.more-button:hover {

  opacity: 1;

  color: var(--color-text-primary);

}

/* 자동완성 (Soft Glassmorphism) */
.suggestion-box {

  width: 100%;

  margin-top: var(--space-xs);

  border-radius: var(--radius-md);

  overflow: hidden;

  box-sizing: border-box;

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.5) 0%, rgba(255, 255, 255, 0.2) 100%);

  border: 1px solid rgba(255, 255, 255, 0.3);

  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.4);

  backdrop-filter: blur(10px);

  -webkit-backdrop-filter: blur(10px);

}

[data-theme="dark"] .suggestion-box {

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%);

  border: 1px solid rgba(255, 255, 255, 0.08);

  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.05);

}


.suggestion-item {

  padding: var(--space-sm) var(--space-md);

  border-bottom: 1px solid var(--color-border);

  cursor: pointer;

  display: flex;

  justify-content: space-between;

  align-items: center;

  color: var(--color-text-primary);

  transition: var(--transition-fast);

}

.suggestion-text {

  display: flex;

  flex-direction: column;

  gap: 2px;

}

.suggestion-alias {

  font-size: var(--font-xs);

  color: var(--color-text-tertiary);

}


.suggestion-item:last-child {

  border-bottom: none;

}


.suggestion-item:hover {

  background: rgba(255, 255, 255, 0.15);

}

[data-theme="dark"] .suggestion-item:hover {

  background: rgba(255, 255, 255, 0.04);

  background: #f8f8f8;
 
}


:deep(.highlight) {

  color: var(--color-gold-text);

  font-weight: var(--font-bold);

}

</style>
<!-- 07_25 연동 변경: 제휴 멤버십 목록 조회와 멤버십 추가 API를 연결한다. -->
