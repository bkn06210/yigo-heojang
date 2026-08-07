<script setup>

<<<<<<< HEAD
import { computed, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useAuthStore } from '@/stores/authStore';
import { useCardStore } from '@/stores/cardStore';

import PageHeader from '@/components/common/PageHeader.vue';
=======
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useCardStore } from '@/stores/cardStore';
import { useAuthStore } from '@/stores/authStore';

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
import CardItem from '@/components/card/CardItem.vue';
import CardCompanyGroup from '@/components/card/CardCompanyGroup.vue';
import EmptyStateCard from '@/components/common/EmptyStateCard.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
<<<<<<< HEAD
import { useToast } from '@/composables/useToast';
import Icon from '@/components/common/Icon.vue';
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e



const router = useRouter();
<<<<<<< HEAD
const { showToast } = useToast();

// 로그인 상태
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const isLogin = computed(() => !!user.value);

// 카드 Store 연결
const cardStore = useCardStore();

const { cards } = storeToRefs(cardStore);

=======


// 카드 Store 연결
const cardStore = useCardStore();
const authStore = useAuthStore();

const { cards } = storeToRefs(cardStore);

// 테스트 로그 추가
console.log('카드 목록 진입:', cards.value);



// 로그인 상태
// TODO: 추후 authStore 연결
// PR #25 연동: 로그인 상태일 때만 인증이 필요한 카드 현황 API를 호출한다.
const isLogin = computed(() => Boolean(authStore.token || localStorage.getItem('token')));

// PR #32 연동: 기본 보유카드 목록을 조회한 뒤 PR #25 실적 정보를 카드 ID로 결합한다.
onMounted(async () => {
  if (!isLogin.value) return;
  try {
    await cardStore.loadCards();
  } catch (error) {
    console.error('카드 현황 조회 실패', error);
  }
});

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


// 로그인 이동
const goLogin = () => {

  router.push('/auth/login');

};


// 카드 등록 이동
const goRegister = () => {

  router.push('/cards/register');

};


<<<<<<< HEAD
// 초기화
onMounted(() => {
  // mock 데이터는 추가하지 않음
});


=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
// 고정 카드 목록
const pinnedCards = computed(() => {


  return [...cards.value]

    .filter(card => card.pinned)

    .sort((a, b) =>

      a.name.localeCompare(b.name, 'ko')

    );


});



// 카드사별 그룹
const cardCompanies = computed(() => {


  const groups = {};



  cards.value.forEach(card => {


    if (!groups[card.company]) {


      groups[card.company] = {

        name: card.company,

        cards: [],

      };


    }



    groups[card.company].cards.push(card);

  });


  return Object.values(groups)

    .sort((a, b) =>

      a.name.localeCompare(b.name, 'ko')

    );


});


// 카드 고정
const togglePin = (id) => {


  const target = cards.value.find(

    card => card.id === id

  );


  if (!target) return;


  const pinnedCount =

    cards.value.filter(

      card => card.pinned

    ).length;



  if (!target.pinned && pinnedCount >= 3) {

<<<<<<< HEAD
    showToast('warning', '고정 카드는 최대 3개까지 가능합니다.');

    return;

=======

    alert(

      '고정 카드는 최대 3개까지 가능합니다.'

    );


    return;


>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  }


  target.pinned = !target.pinned;

};

<<<<<<< HEAD
// 추천 카드 숨기기
const hideRecommendedCard = ref(
  localStorage.getItem('hideRecommendedCard') === 'true'
);

const closeRecommendedCard = () => {
  hideRecommendedCard.value = true;
  localStorage.setItem('hideRecommendedCard', 'true');
};
=======

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

</script>

<template>

<div class="card-list-page">

<<<<<<< HEAD
  <!-- 페이지 헤더 -->
  <PageHeader title="카드 목록" :show-back="false" @back="router.back()" />

  <!-- 비로그인 또는 카드 없음 -->
  <main v-if="!isLogin || cards.length === 0" style="flex: 1; display: flex; align-items: center; justify-content: center;">
    <div style="text-align: center; display: flex; flex-direction: column; gap: 16px;">
      <h2 v-if="!isLogin" style="margin: 0; font-size: 18px; font-weight: 700; color: var(--color-text-primary);">로그인이 필요합니다</h2>
      <h2 v-else style="margin: 0; font-size: 18px; font-weight: 700; color: var(--color-text-primary);">등록된 카드가 없어요</h2>

      <p v-if="!isLogin" style="margin: 0; font-size: 14px; color: var(--color-text-secondary);">카드를 등록하고 관리하려면 로그인해주세요.</p>
      <p v-else style="margin: 0; font-size: 14px; color: var(--color-text-secondary);">카드를 등록하면 혜택과 소비 관리를 시작할 수 있습니다.</p>

      <button v-if="!isLogin" @click="goLogin" style="padding: 12px 20px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-full); font-weight: 600; cursor: pointer;">로그인</button>
      <button v-else @click="goRegister" style="padding: 12px 20px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-full); font-weight: 600; cursor: pointer;">카드 등록</button>
    </div>
  </main>
=======


  <!-- 비로그인 -->

  <EmptyStateCard

    v-if="!isLogin"

    title="로그인 후 이용할 수 있어요"

    description="로그인하면 내 카드를 등록하고 관리할 수 있습니다."

    buttonText="로그인"

    @click="goLogin"

  />



  <!-- 로그인 + 카드 없음 -->

  <EmptyStateCard

    v-else-if="cards.length === 0"

    title="등록된 카드가 없어요"

    description="카드를 등록하면 혜택과 소비 관리를 시작할 수 있습니다."

    buttonText="카드 등록"

    @click="goRegister"

  />
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e




  <!-- 로그인 + 카드 있음 -->

  <template v-else>



<<<<<<< HEAD

    <!-- 고정 카드 목록 -->

    <section v-if="pinnedCards.length > 0" class="card-section pinned-section">

      <h2>
        <Icon name="star-filled" size="sm" /> 고정 카드
      </h2>

      <div class="pinned-cards-list">
        <CardItem
          v-for="card in pinnedCards"
          :key="card.id"
          :card="card"
          @toggle-pin="togglePin"
        />
      </div>
=======
    <header class="header">


      <h1>
        내 카드
      </h1>




      <button

        class="register-button"

        @click="goRegister"

      >

        + 카드 등록

      </button>



    </header>




    <!-- 고정 카드 -->

    <section class="card-section">


      <h2>
        📌 고정 카드
      </h2>




      <CardItem

        v-for="card in pinnedCards"

        :key="card.id"

        :card="card"

        @toggle-pin="togglePin"

      />


>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

    </section>




<<<<<<< HEAD
    <!-- 전체 카드 (Grid Layout) -->

    <section class="card-section all-cards-section">


      <div class="all-cards-header">
        <h2>
          전체 카드
        </h2>
        <button type="button" class="add-card-btn" @click="goRegister">
          + 카드 등록
        </button>
      </div>
=======
    <!-- 전체 카드 -->

    <section class="card-section">


      <h2>
        전체 카드
      </h2>


>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


      <CardCompanyGroup

        v-for="company in cardCompanies"

        :key="company.name"

        :company="company"

        @toggle-pin="togglePin"

      />


<<<<<<< HEAD
=======

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
    </section>



  </template>
<<<<<<< HEAD

=======
  
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
<BottomNavigation />

</div>



</template>



<style scoped>


.card-list-page {

<<<<<<< HEAD
  padding: var(--space-md);

  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));

  min-height: 100vh;

  background: var(--color-bg);

  margin: 0 auto;

  max-width: 480px;

  box-sizing: border-box;

  overflow: hidden visible;

  display: flex;

  flex-direction: column;
=======
  padding:20px;

  min-height:100vh;

  background:#fafafa;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}




<<<<<<< HEAD
.header {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: var(--space-xl);
=======

.header {

  display:flex;

  justify-content:space-between;

  align-items:center;

  margin-bottom:24px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}




<<<<<<< HEAD
.header h1 {

  margin: 0;

  font-size: var(--typo-display-medium-size);

  font-weight: var(--typo-display-medium-weight);

  line-height: var(--typo-display-medium-line-height);

  letter-spacing: var(--typo-display-medium-letter-spacing);

  color: var(--color-text-primary);
=======

.header h1 {

  margin:0;

  font-size:24px;

  font-weight:700;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}




<<<<<<< HEAD
.register-button {

  border: none;

  background: var(--color-primary);

  color: var(--color-btn-primary-text);

  padding: var(--space-xs) var(--space-md);

  border-radius: var(--radius-md);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

}

.register-button:hover {

  opacity: 0.9;

  transform: translateY(-1px);

}



.all-cards-header {

  display: flex;

  align-items: center;

  justify-content: space-between;

  margin-bottom: var(--space-md);

}

.all-cards-header h2 {

  margin: 0;

}

.add-card-btn {

  border: none;

  background: var(--color-primary);

  color: var(--color-btn-primary-text);

  padding: var(--space-xs) var(--space-md);

  border-radius: var(--radius-md);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

  white-space: nowrap;

}

.add-card-btn:hover {

  opacity: 0.9;

  transform: translateY(-1px);

}
=======

.register-button {


  border:none;

  background:#4F46E5;

  color:white;


  padding:11px 16px;


  border-radius:14px;


  font-size:14px;


  font-weight:600;


  cursor:pointer;


}

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e




.card-section {

<<<<<<< HEAD
  margin-bottom: var(--space-lg);
=======

  margin-bottom:32px;

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


<<<<<<< HEAD
.pinned-section {

  margin-top: -21px;

  margin-bottom: var(--space-lg);

  display: flex;

  flex-direction: column;

}

.pinned-section h2 {

  align-self: flex-start;

}


/* 고정 카드 목록 */
.pinned-cards-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.card-section h2 {

  font-size: var(--font-lg);

  margin-bottom: var(--space-sm);

  font-weight: var(--font-bold);

  color: var(--color-text-primary);

  letter-spacing: -0.3px;
=======



.card-section h2 {


  font-size:18px;


  margin-bottom:16px;


  font-weight:700;

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


<<<<<<< HEAD
.recommended-slide {

  display: flex !important;

  justify-content: center;

  align-items: center;

}

.recommended-card-content {

  width: 100%;

  padding: var(--space-lg);

  background: linear-gradient(135deg, rgba(200, 220, 240, 0.4) 0%, rgba(220, 240, 255, 0.2) 100%);

  border: 1px solid rgba(100, 150, 200, 0.15);

  border-radius: var(--radius-lg);

  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.3);

  backdrop-filter: blur(6px);

  -webkit-backdrop-filter: blur(6px);

  position: relative;

}

.recommended-card-content .close-btn {

  position: absolute;

  top: var(--space-sm);

  right: var(--space-sm);

  width: 28px;

  height: 28px;

  display: flex;

  align-items: center;

  justify-content: center;

  background: rgba(255, 255, 255, 0.6);

  border: 1px solid rgba(0, 0, 0, 0.1);

  border-radius: var(--radius-md);

  font-size: 18px;

  cursor: pointer;

  color: var(--color-text-secondary);

  transition: var(--transition-fast);

}

.recommended-card .close-btn:hover {

  background: rgba(255, 255, 255, 0.8);

  color: var(--color-text-primary);

}

.recommended-header {

  display: flex;

  align-items: center;

  gap: var(--space-sm);

  margin-bottom: var(--space-md);

}

.recommended-header .icon {

  font-size: 20px;

}

.recommended-header h3 {

  margin: 0;

  font-size: var(--font-md);

  font-weight: var(--font-semibold);

  color: var(--color-text-primary);

}

.recommended-subtitle {

  margin: 0 0 var(--space-md) 0;

  font-size: var(--font-sm);

  color: var(--color-text-secondary);

}

.recommended-preview {

  width: 100%;

  margin-bottom: var(--space-md);

}

.recommended-preview img {

  width: 100%;

  aspect-ratio: 1.586 / 1;

  border-radius: var(--radius-md);

  object-fit: contain;

  background: rgba(255, 255, 255, 0.3);

}

.recommended-info {

  margin-bottom: var(--space-md);

}

.recommended-name {

  margin: 0 0 var(--space-xs) 0;

  font-size: var(--font-sm);

  font-weight: var(--font-bold);

  color: var(--color-text-primary);

}

.recommended-benefit {

  margin: 0;

  font-size: var(--font-xs);

  color: var(--color-text-tertiary);

}

.explore-btn {

  width: 100%;

  padding: var(--space-md);

  background: rgba(100, 150, 200, 0.15);

  border: 1px solid rgba(100, 150, 200, 0.3);

  border-radius: var(--radius-md);

  color: var(--color-text-primary);

  font-size: var(--font-sm);

  font-weight: var(--font-semibold);

  cursor: pointer;

  transition: var(--transition-fast);

}

.explore-btn:hover {

  background: rgba(100, 150, 200, 0.25);

  border-color: rgba(100, 150, 200, 0.4);

}
=======


>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


:deep(.empty-card) {

<<<<<<< HEAD
  background: var(--color-surface);

  border-radius: var(--radius-lg);

  padding: var(--space-2xl) var(--space-md);

  box-shadow: var(--shadow-card);
=======

  margin-top:40px;


  background:white;


  border-radius:20px;


  padding:36px 20px;


  box-shadow:

    0 4px 12px rgba(0,0,0,0.06);

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}




:deep(.empty-card button:first-child) {

<<<<<<< HEAD
  background: var(--color-primary);

  color: var(--color-btn-primary-text);
=======

  background:#4F46E5;


  color:white;

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


<<<<<<< HEAD
=======


>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</style>
<!-- 07_25 연동 변경: 로그인 회원의 실제 보유카드 목록 API를 화면 진입 시 호출한다. -->
