<script setup>

import { computed, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { Swiper, SwiperSlide } from 'swiper/vue';
import { Pagination } from 'swiper/modules';
import 'swiper/css';
import 'swiper/css/pagination';

import { useAuthStore } from '@/stores/authStore';
import { useCardStore } from '@/stores/cardStore';

import PageHeader from '@/components/common/PageHeader.vue';
import CardItem from '@/components/card/CardItem.vue';
import CardCompanyGroup from '@/components/card/CardCompanyGroup.vue';
import EmptyStateCard from '@/components/common/EmptyStateCard.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import { useToast } from '@/composables/useToast';
import Icon from '@/components/common/Icon.vue';



const router = useRouter();
const { showToast } = useToast();

// 로그인 상태
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);
const isLogin = computed(() => !!user.value);

// 카드 Store 연결
const cardStore = useCardStore();

const { cards } = storeToRefs(cardStore);



// 로그인 이동
const goLogin = () => {

  router.push('/auth/login');

};


// 카드 등록 이동
const goRegister = () => {

  router.push('/cards/register');

};


// 테스트용 mock 카드 추가
const addMockCards = () => {
  if (cards.value.length === 0) {
    cardStore.addCard({
      id: 1,
      name: '테스트 카드',
      cardNumber: '4111111111111111',
      company: 'KB국민카드',
      image: '',
      pinned: false,
      achievementRate: 65
    });
  }
};

// 초기화
onMounted(() => {
  addMockCards();
});


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

    showToast('warning', '고정 카드는 최대 3개까지 가능합니다.');

    return;

  }


  target.pinned = !target.pinned;

};

// 추천 카드 숨기기
const hideRecommendedCard = ref(
  localStorage.getItem('hideRecommendedCard') === 'true'
);

const closeRecommendedCard = () => {
  hideRecommendedCard.value = true;
  localStorage.setItem('hideRecommendedCard', 'true');
};

</script>

<template>

<div class="card-list-page">

  <!-- 페이지 헤더 -->
  <PageHeader title="카드 목록" :show-back="false" @back="router.back()" />

  <!-- 비로그인 -->
  <main v-if="!isLogin" style="flex: 1; display: flex; align-items: center; justify-content: center;">
    <div style="text-align: center; display: flex; flex-direction: column; gap: 16px;">
      <h2 style="margin: 0; font-size: 18px; font-weight: 700; color: var(--color-text-primary);">로그인이 필요합니다</h2>
      <p style="margin: 0; font-size: 14px; color: var(--color-text-secondary);">카드를 등록하고 관리하려면 로그인해주세요.</p>
      <button @click="goLogin" style="padding: 12px 20px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-full); font-weight: 600; cursor: pointer;">로그인</button>
    </div>
  </main>



  <!-- 로그인 + 카드 없음 -->

  <EmptyStateCard

    v-else-if="cards.length === 0"

    title="등록된 카드가 없어요"

    description="카드를 등록하면 혜택과 소비 관리를 시작할 수 있습니다."

    buttonText="카드 등록"

    @click="goRegister"

  />




  <!-- 로그인 + 카드 있음 -->

  <template v-else>




    <!-- 고정 카드 Carousel -->

    <section v-if="pinnedCards.length > 0" class="card-section pinned-section">


      <h2>
        <Icon name="star-filled" size="sm" /> 고정 카드
      </h2>


      <!-- Carousel (1개: 고정카드 + 추천카드, 2개 이상: 고정카드들) -->
      <Swiper
        v-if="pinnedCards.length > 0"
        :modules="[Pagination]"
        :slides-per-view="1.25"
        :centered-slides="true"
        :space-between="16"
        :pagination="{ clickable: true, el: '.swiper-pagination-custom' }"
        :grab-cursor="true"
        class="pinned-carousel"
      >
        <!-- 고정 카드들 -->
        <SwiperSlide v-for="card in pinnedCards" :key="card.id" class="pinned-slide stack-slide">
          <CardItem

            :card="card"

            @toggle-pin="togglePin"

          />
        </SwiperSlide>

        <!-- 추천 카드 (1개일 때만) -->
        <SwiperSlide
          v-if="pinnedCards.length === 1 && !hideRecommendedCard"
          class="pinned-slide recommended-slide"
        >
          <div class="recommended-card-content">
            <button class="close-btn" @click="closeRecommendedCard" type="button" title="닫기">
              <Icon name="close" size="xs" />
            </button>

            <div class="recommended-header">
              <span class="icon"><Icon name="lightbulb" size="sm" /></span>
              <h3>발견</h3>
            </div>

            <p class="recommended-subtitle">당신을 위한 새로운 카드</p>

            <div class="recommended-preview">
              <img src="https://via.placeholder.com/280x177?text=New+Card" alt="추천 카드" />
            </div>

            <div class="recommended-info">
              <p class="recommended-name">새로운 카드</p>
              <p class="recommended-benefit">더 많은 혜택을 누려보세요</p>
            </div>

            <button class="explore-btn">살펴보기</button>
          </div>
        </SwiperSlide>
      </Swiper>

      <!-- 페이지네이션 인디케이터 -->
      <div class="swiper-pagination-custom"></div>


    </section>




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


      <CardCompanyGroup

        v-for="company in cardCompanies"

        :key="company.name"

        :company="company"

        @toggle-pin="togglePin"

      />


    </section>



  </template>

<BottomNavigation />

</div>



</template>



<style scoped>


.card-list-page {

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

}




.header {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: var(--space-xl);

}




.header h1 {

  margin: 0;

  font-size: var(--typo-display-medium-size);

  font-weight: var(--typo-display-medium-weight);

  line-height: var(--typo-display-medium-line-height);

  letter-spacing: var(--typo-display-medium-letter-spacing);

  color: var(--color-text-primary);

}




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




.card-section {

  margin-bottom: var(--space-2xl);

}


.pinned-section {

  margin-bottom: var(--space-md);

  display: flex;

  flex-direction: column;

  min-height: 420px;

}

.pinned-section h2 {

  align-self: flex-start;

}


/* Swiper Carousel 스타일 */
.pinned-carousel {

  padding: var(--space-lg) var(--space-md);

  overflow: hidden;

  cursor: grab;

  height: 320px;

  box-sizing: border-box;

  width: 100%;

}

.pinned-carousel:active {

  cursor: grabbing;

}


:deep(.swiper-wrapper) {

  align-items: center;

}

:deep(.swiper-slide) {

  width: auto;

  height: auto;

  min-width: 280px;

}


.pinned-slide {

  display: flex !important;

  justify-content: center;

  align-items: center;

  width: auto;

  height: 100%;

}


/* 페이지네이션 커스텀 스타일 */
.swiper-pagination-custom {

  display: flex;

  gap: var(--space-xs);

  justify-content: center;

  margin-top: var(--space-lg);

  min-height: 24px;

}


:deep(.swiper-pagination-bullet) {

  width: 8px;

  height: 8px;

  background: var(--color-border);

  opacity: 1;

  transition: var(--transition-fast);

}


:deep(.swiper-pagination-bullet-active) {

  width: 24px;

  background: var(--color-primary);

  border-radius: var(--radius-full);

}

:deep(.swiper-scrollbar) {

  background: rgba(0, 0, 0, 0.08);

}

:deep(.swiper-scrollbar-drag) {

  background: var(--color-primary);

  opacity: 0.8;

}

[data-theme="dark"] :deep(.swiper-scrollbar) {

  background: rgba(255, 255, 255, 0.15);

}

[data-theme="dark"] :deep(.swiper-scrollbar-drag) {

  background: var(--color-primary);

  opacity: 1;

}

.card-section h2 {

  font-size: var(--font-lg);

  margin-bottom: var(--space-md);

  font-weight: var(--font-bold);

  color: var(--color-text-primary);

  letter-spacing: -0.3px;

}


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


:deep(.empty-card) {

  background: var(--color-surface);

  border-radius: var(--radius-lg);

  padding: var(--space-2xl) var(--space-md);

  box-shadow: var(--shadow-card);

}




:deep(.empty-card button:first-child) {

  background: var(--color-primary);

  color: var(--color-btn-primary-text);

}


</style>
