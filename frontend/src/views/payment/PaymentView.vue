<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { usePaymentStore } from '@/stores/payment';
import { useAuthStore } from '@/stores/authStore';
import { useCardStore } from '@/stores/cardStore';
import { createPaymentQr, getPaymentQr } from '@/api/paymentQrApi';
import { getPayment } from '@/api/walletApi';
import { getRecommendations } from '@/api/paymentApi';
import { usePersonalizationStore } from '@/stores/personalization';
import PageHeader from '@/components/common/PageHeader.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import ToastNotification from '@/components/common/ToastNotification.vue';
import PaymentPasswordModal from '@/components/payment/PaymentPasswordModal.vue';
import PaymentQRModal from '@/components/payment/PaymentQRModal.vue';
import PaymentTimeoutModal from '@/components/payment/PaymentTimeoutModal.vue';
import PaymentAmountInput from '@/components/payment/PaymentAmountInput.vue';
import MerchantSelector from '@/components/payment/MerchantSelector.vue';

const paymentStore = usePaymentStore();
const router = useRouter();
const authStore = useAuthStore();
const cardStore = useCardStore();
const personalizationStore = usePersonalizationStore();

const { user } = storeToRefs(authStore);
const { cards: storeCards } = storeToRefs(cardStore);
const isLoggedIn = computed(() => !!user.value);
const hasCards = computed(() => storeCards.value.length > 0);

// 카드 등록 페이지로 이동
const goToCardRegister = () => {
  router.push('/cards/register');
};

const CAROUSEL_CONFIG = {
  FLIP_THRESHOLD: 100,
  HORIZONTAL_SWIPE_THRESHOLD: 50,
  MODAL_SHOW_DELAY: 400,
};

const cards = computed(() => storeCards.value);

const selectedIndexFloat = ref(0);
const selectedIndex = computed(() => Math.round(selectedIndexFloat.value) % Math.max(cards.value.length, 1));
const paymentAmount = ref(0);
const selectedCategory = ref(null);
const selectedMerchant = ref(null);
const recommendedCardIds = ref([]);
const recommendedInfo = ref({}); // { cardId: { rank, reason, benefit } }
const isRecommending = ref(false);
const showSwipeGuide = ref(false); // 스와이프 가이드 톨팁
const showRecommendationResult = ref(false); // 추천 결과 표시 여부

// 현재 선택된 카드
const currentCard = computed(() => cards.value[selectedIndex.value] || null);

// 현재 카드의 혜택 3개
const currentBenefits = computed(() => {
  return currentCard.value?.benefits?.slice(0, 3) || [];
});

// 카드 개수에 따라 selectedIndexFloat 초기화
watch(() => storeCards.value.length, (newLen) => {
  if (newLen <= 2) {
    selectedIndexFloat.value = 0;
  } else {
    selectedIndexFloat.value = Math.floor(newLen / 2);
  }
}, { immediate: true });
const flipProgress = ref(0); // 0 ~ 1
const flippingIndex = ref(null);
const showPasswordModal = ref(false);
const showQRModal = ref(false);
const showTimeoutModal = ref(false);
const qrToken = ref(null);
const qrImageUrl = ref(null);
const cardUpOffset = ref(0);
const showToast = ref(false);
const toastMessage = ref('');
const isFlipping = ref(false);
const showPaymentAmountInput = ref(false);
const isRecommendationMode = ref(false);
const isAnimatingToCenter = ref(false); // 모든 카드가 중앙에 합쳐지는 중


let touchStartX = 0;
let touchStartY = 0;
let isDragging = false;
let swipeDirection = null;

// 스와이프 진행도 (-1 ~ 1, 우측/-좌측/+)
const swipeProgress = ref(0);
const isSwipeAnimating = ref(false);

// 부채꼴 배치 (카드 수에 따라 자동 각도 분배) / 추천 모드 선형 배치
const getCardStyle = (index) => {
  const centerIndex = selectedIndex.value;
  const cardCount = cards.value.length;

  // 플립 중인 카드: 올라가면서 점점 뒤집어짐
  if (index === flippingIndex.value && flipProgress.value > 0) {
    const flipAngle = flipProgress.value * 180;
    return {
      transform: `translate(-50%, -100%) translateY(-${80 + cardUpOffset.value}px) rotateY(${flipAngle}deg)`,
      opacity: 1,
      zIndex: 100,
    };
  }

  // 부채꼴 배치: 애니메이션 상태와 모드에 따라
  let offset;
  if (isAnimatingToCenter.value) {
    // 합쳐지는 중: 모든 카드가 정중앙에 겹침
    offset = 0;
  } else {
    // 평소/추천 모드: 항상 centerIndex 기준 (스와이프로 카드 이동 가능)
    offset = index - centerIndex;
  }

  // 카드 개수별 레이아웃 결정
  let angle = 0;
  let spread = 0;
  let yLift = 0;

  if (cardCount === 1) {
    // 1장: 중앙 고정
    angle = 0;
    spread = 0;
    yLift = isRecommending.value ? 100 : 80;
  } else if (cardCount === 2) {
    // 2장: 좌우 균형 배치
    angle = offset * 40; // -40도, 40도
    spread = 100;
    yLift = isRecommending.value ? 100 : 50;
  } else {
    // 3장 이상: 부채꼴 배치
    const maxOffset = Math.floor(cardCount / 2);
    const depthFactor = Math.abs(offset);

    // 1번은 90도, 2번은 45도
    if (depthFactor === 0) {
      angle = 0;
      spread = 0;
      yLift = 80;
    } else if (depthFactor === 1) {
      angle = offset > 0 ? 45 : -45;
      spread = 70;
      yLift = 60;
    } else {
      angle = offset > 0 ? 90 : -90;
      spread = 60;
      yLift = 0;
    }
  }

  const x = offset * spread;
  const yCorrection = Math.abs(angle) * 0.3;

  // 플립 중일 때 다른 카드들은 고정 (cardUpOffset 무시)
  if (flipProgress.value > 0 && index !== flippingIndex.value) {
    return {
      transform: `translate(-50%, -100%) translate(${x}px, -${yCorrection + yLift}px) rotate(${angle}deg) scale(${index === centerIndex ? 1 : 0.9})`,
      opacity: index === centerIndex ? 1 : 0.7,
      zIndex: 10 - Math.abs(offset),
    };
  }

  // isAnimatingToCenter 상태에서는 cardUpOffset 무시
  const upOffset = isAnimatingToCenter.value ? 0 : (index === centerIndex ? cardUpOffset.value : 0);
  const isCenter = isAnimatingToCenter.value || index === centerIndex;
  const scale = isCenter ? 1 : 0.9;
  const opacity = isCenter ? 1 : 0.7;
  const zIndex = 10 - Math.abs(offset);

  return {
    transform: `translate(-50%, -100%) translate(${x}px, -${yCorrection + yLift + upOffset}px) rotate(${angle}deg) scale(${scale})`,
    opacity,
    zIndex,
  };
};

// Cubic-bezier easing 함수
const easeInOutCubic = (t) => {
  // cubic-bezier(0.22, 1, 0.36, 1) - 자연스러운 감속
  return t < 0.5
    ? 4 * t * t * t
    : 1 - Math.pow(-2 * t + 2, 3) / 2;
};

// 카드 추천 받기
const getCardRecommendations = async () => {
  if (isRecommending.value) return;

  try {
    isRecommending.value = true;

    // 카테고리 라벨로 id 찾기 (백엔드는 숫자 ID를 기대)
    const categoryId = selectedCategory.value
      ? personalizationStore.categories.find(cat => cat.label === selectedCategory.value)?.id
      : null;

    // 금액이 0보다 크면 전송, 아니면 null
    const expectedAmount = (paymentAmount.value && paymentAmount.value > 0) ? paymentAmount.value : null;

    console.log('추천 API 요청 데이터:', {
      categoryId,
      selectedCategory: selectedCategory.value,
      expectedAmount,
      paymentType: 'CARD',
    });

    // API 호출: 선택한 가맹점 정보와 함께 추천 요청
    const response = await getRecommendations({
      categoryId: categoryId,
      merchantId: null,
      expectedAmount: expectedAmount,
      paymentType: 'CARD',
    });


    // 추천 카드 ID 리스트 저장 (최대 3장) + 추천 정보
    // 응답 형식: { recommendations: [{ userCardId, rank, benefitReason, ... }] }
    const recommendations = response?.data?.recommendations || [];
    console.log('API 추천 응답:', recommendations);

    recommendedCardIds.value = recommendations
      .map(item => item.userCardId)
      .slice(0, 3);

    // 추천 정보 저장: { cardId: { rank, reason } }
    recommendedInfo.value = {};
    recommendations.slice(0, 3).forEach((item, idx) => {
      recommendedInfo.value[item.userCardId] = {
        rank: idx + 1,
        reason: item.benefitReason || item.benefitsSummary?.[0] || '최적의 선택',
        benefit: item.benefitsSummary?.[0] || '주요 혜택'
      };
    });
    console.log('저장된 추천 카드 ID들:', recommendedCardIds.value);
    console.log('추천 정보:', recommendedInfo.value);

    // 카드를 추천 순서대로 재정렬
    cardStore.reorderCardsByRecommendation(recommendedCardIds.value);

    // 스와이프 가이드 톨팁 표시 (1초 후 자동 표시, 3초 후 자동 숨김)
    setTimeout(() => {
      showSwipeGuide.value = true;
      setTimeout(() => {
        showSwipeGuide.value = false;
      }, 3000);
    }, 1000);

    // 목표 카드 인덱스 계산
    if (recommendedCardIds.value.length > 0) {
      const topRecommendedId = recommendedCardIds.value[0];

      // 디버깅: 현재 카드들 출력

      console.log('추천된 카드 ID:', topRecommendedId, '타입:', typeof topRecommendedId);
      console.log('현재 보유한 카드들:', cards.value.map(c => ({ id: c.id, id타입: typeof c.id, name: c.name })));
      const targetIndex = cards.value.findIndex(c => Number(c.id) === Number(topRecommendedId));


      if (targetIndex !== -1) {
        const cardCount = cards.value.length;
        const startIndex = selectedIndexFloat.value;
        const centerValue = Math.floor(cardCount / 2);

        // 1단계: 모든 카드가 중앙으로 합쳐지기 (800ms - 여유롭게)
        isAnimatingToCenter.value = true;
        const stage1Start = Date.now();
        const stage1Duration = 800;

        const animate1 = () => {
          const elapsed = Date.now() - stage1Start;
          const progress = Math.min(elapsed / stage1Duration, 1);
          const eased = easeInOutCubic(progress);

          const newIndex = startIndex + (centerValue - startIndex) * eased;
          selectedIndexFloat.value = newIndex;

          if (progress < 1) {
            requestAnimationFrame(animate1);
          } else {
            selectedIndexFloat.value = centerValue;
            // 1단계 완료: 잠깐 여유 후 2단계 펼쳐짐
            setTimeout(() => {
              cardStore.reorderCardsByRecommendation(recommendedCardIds.value);
              selectedIndexFloat.value = 0; // 1순위가 중앙에
              isAnimatingToCenter.value = false;
              isRecommendationMode.value = true;
              showRecommendationResult.value = true; // 추천 결과 헤더 표시
              // CSS transition이 자동으로 펼쳐짐 애니메이션 처리
            }, 800); // 1단계 duration과 같음
            isRecommending.value = false;
          }
        };

        animate1();
      } else {
        isRecommending.value = false;
        console.error('목표 카드를 찾을 수 없습니다.');
      }
    } else {
      isRecommending.value = false;
      console.error('추천 카드가 없습니다.');
    }
  } catch (error) {
    console.error('카드 추천 API 에러:', error?.response?.data || error.message);
    isRecommending.value = false;
  }
};

// 스와이프 끝났을 때 처리
const handleSwipeEnd = (deltaY) => {
  const FLIP_THRESHOLD = 120; // 위로 스와이프 임계값(px)

  if (deltaY > FLIP_THRESHOLD) {
    flippingIndex.value = selectedIndex.value;
    isFlipping.value = true;
  }
};

// 터치 이벤트
const handleTouchStart = (e) => {
  touchStartX = e.touches[0].clientX;
  touchStartY = e.touches[0].clientY;
  isDragging = true;
  swipeDirection = null;
};

const handleTouchMove = (e) => {
  if (!isDragging) return;
  const currentX = e.touches[0].clientX;
  const currentY = e.touches[0].clientY;
  const rawDeltaX = currentX - touchStartX;
  const rawDeltaY = touchStartY - currentY;

  // 스무딩: 현재 값 30% + 이전 값 70% (불규칙한 터치 입력을 부드럽게)
  const prevDeltaX = swipeDirection?.deltaX || 0;
  const prevDeltaY = swipeDirection?.deltaY || 0;
  const deltaX = rawDeltaX * 0.3 + prevDeltaX * 0.7;
  const deltaY = rawDeltaY * 0.3 + prevDeltaY * 0.7;

  swipeDirection = { deltaX, deltaY };

  // isAnimatingToCenter 상태: 스와이프만 반응
  if (isAnimatingToCenter.value) {
    if (Math.abs(deltaX) > 10) {
      swipeProgress.value = deltaX / 300;
    }
    return;
  }

  // 위로 드래그할 때: 카드 플립
  if (deltaY > 0) {
    cardUpOffset.value = Math.min(deltaY, 300);
    flipProgress.value = Math.min(deltaY / 300, 1);

    if (deltaY > CAROUSEL_CONFIG.FLIP_THRESHOLD && !flippingIndex.value) {
      flippingIndex.value = selectedIndex.value;
    }

    if (flipProgress.value >= 0.5) {
      showPasswordModal.value = true;
    }
  }
  // 좌우로 드래그할 때: 카드 회전
  else if (Math.abs(deltaX) > 10) {
    swipeProgress.value = deltaX / 300; // 300px를 기준으로 -1 ~ 1
  }
};

const handleTouchEnd = () => {
  if (!isDragging) return;
  isDragging = false;

  if (!swipeDirection) {
    cardUpOffset.value = 0;
    return;
  }
  const { deltaX, deltaY } = swipeDirection;

  console.log('🎯 Swipe detected:', { deltaX, deltaY, FLIP_THRESHOLD: CAROUSEL_CONFIG.FLIP_THRESHOLD });

  // 가로 스와이프로 카드 회전
  if (Math.abs(swipeProgress.value) > 0.2 && flipProgress.value === 0) {
    console.log('→ Horizontal swipe - rotating cards');
    isSwipeAnimating.value = true;

    // 추천 모드 진입: 모든 카드가 겹쳐있는 상태에서 스와이프하면 펼쳐짐
    if (isAnimatingToCenter.value && !isRecommendationMode.value) {
      console.log('→ Expanding recommendation cards');
      cardStore.reorderCardsByRecommendation(recommendedCardIds.value);
      selectedIndexFloat.value = 0; // 재정렬 후 1순위가 중앙에
      isAnimatingToCenter.value = false;
      isRecommendationMode.value = true;
    } else {
      // 평소 스와이프: selectedIndexFloat 변경
      if (swipeProgress.value > 0) {
        selectedIndexFloat.value = selectedIndexFloat.value - 1;
      } else {
        selectedIndexFloat.value = selectedIndexFloat.value + 1;
      }
    }

    setTimeout(() => {
      swipeProgress.value = 0;
      isSwipeAnimating.value = false;
    }, 400);
    return;
  }

  // 스와이프 진행도 복원
  if (Math.abs(swipeProgress.value) > 0) {
    swipeProgress.value = 0;
  }

  // 50% 이상 올렸으면 자동으로 1까지 완성 (spring animation)
  if (flipProgress.value >= 0.5) {
    console.log('🎉 Auto complete - showing modal!');
    cardUpOffset.value = 0;
    flipProgress.value = 1; // 자동으로 완성
    setTimeout(() => {
      showPasswordModal.value = true;
    }, CAROUSEL_CONFIG.MODAL_SHOW_DELAY);
  } else {
    // 50% 미만이면 자동으로 0으로 복원 (spring back)
    console.log('↩ Auto restore to start');
    cardUpOffset.value = 0;
    flipProgress.value = 0;
    flippingIndex.value = null;
  }
};

const selectCard = (index) => {
  selectedIndexFloat.value = index;
};

const handlePasswordSuccess = async () => {
  showPasswordModal.value = false;

  try {
    if (!currentCard.value || !currentCard.value.id) {
      throw new Error('카드를 선택해주세요');
    }

    // 디버그: 전송 데이터 확인
    console.log('QR 생성 요청:', {
      userCardId: currentCard.value.id,
      paymentAmount: paymentAmount.value,
      card: currentCard.value
    });

    // QR 코드 생성
    const response = await createPaymentQr(currentCard.value.id, paymentAmount.value);
    console.log('QR 생성 응답:', response);

    qrToken.value = response.qrToken || response.token;
    qrImageUrl.value = response.qrImage || response.imageUrl;

    showQRModal.value = true;
  } catch (error) {
    const errorMsg = error?.response?.data?.message || error?.message || 'QR 생성에 실패했습니다';
    console.error('QR 생성 실패:', {
      message: errorMsg,
      status: error?.response?.status,
      data: error?.response?.data,
      fullError: error
    });
    toastMessage.value = errorMsg;
    showToast.value = true;
  }
};

const getModalStyle = () => {
  if (flipProgress.value < 0.5) {
    return { opacity: 0, pointerEvents: 'none' };
  }
  const modalProgress = (flipProgress.value - 0.5) / 0.5; // 0.5~1을 0~1로 정규화 (더 넓은 범위 = 더 부드러움)
  return {
    opacity: modalProgress,
    transform: `translateY(${(1 - modalProgress) * 50}px)`,
    transition: 'none',
    pointerEvents: modalProgress < 1 ? 'none' : 'auto',
  };
};

const handlePasswordClose = () => {
  showPasswordModal.value = false;
  flipProgress.value = 0;
  cardUpOffset.value = 0;
  flippingIndex.value = null;
  isFlipping.value = false; //추가
  isDragging = false;
  swipeDirection = null;

  
};

const handleQRClose = () => {
  showQRModal.value = false;
  // 카드를 원위치로 복원 (간편비번 취소와 동일)
  flipProgress.value = 0;
  cardUpOffset.value = 0;
  flippingIndex.value = null;
  isFlipping.value = false;
  isDragging = false;
  swipeDirection = null;
};

const handleQRSuccess = () => {
  showQRModal.value = false;
  toastMessage.value = '결제 성공';
  showToast.value = true;
  // 토스트가 자동으로 닫힐 때까지 모달 상태 유지
};

const handleQRCancel = () => {
  // 결제 취소 처리 로직...
  showPasswordModal.value = false;
  flipProgress.value = 0;
  flippingIndex.value = null;
  cardUpOffset.value = 0;
  isFlipping.value = false; //추가
  isDragging = false;        
  swipeDirection = null;  

};

const handleQRTimeout = () => {
  showQRModal.value = false;
  showTimeoutModal.value = true;
};

const handleTimeoutRegenerate = () => {
  showTimeoutModal.value = false;
  showQRModal.value = true;
};

const handleTimeoutCancel = () => {
  showTimeoutModal.value = false;
};

const payment = async () => {
  try {
    if (!currentCard.value?.id) return;
    const qrData = await createPaymentQr(currentCard.value.id, 10000);
    flipProgress.value = 1;
    showPasswordModal.value = true;
  } catch (error) {
    console.error('결제 QR 생성 실패:', error);
  }
};

const closeModal = () => {
  showPasswordModal.value = false;
  flipProgress.value = 0;
  flippingIndex.value = null;
  cardUpOffset.value = 0;
  isFlipping.value = false;
};

onMounted(async () => {
  await cardStore.loadCards();
});
</script>

<template>
  <div class="payment-page">
    <PageHeader title="결제" :show-back="false" />

    <!-- 이용 장소 선택 -->
    <div v-if="hasCards" class="merchant-section" style="margin-top: var(--space-sd); margin-bottom: var(--space-md);">
      <h3 style="margin: 0 0 12px 0; font-size: 16px; font-weight: 700; color: var(--color-text-primary);">이용 장소</h3>
      <MerchantSelector
        v-model:category="selectedCategory"
        v-model:merchant="selectedMerchant"
      />
    </div>

    <!-- 결제 금액 입력 -->
    <div v-if="hasCards" class="payment-amount-section" style="margin-bottom: var(--space-md);">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <h3 style="margin: 0; font-size: 16px; font-weight: 700; color: var(--color-text-primary);">결제 금액</h3>
        <button
          @click="showPaymentAmountInput = !showPaymentAmountInput"
          style="background: none; border: none; font-size: 18px; cursor: pointer; padding: 4px 8px; color: var(--color-text-primary); display: flex; align-items: center; justify-content: center; transition: transform 0.3s ease;"
          :style="{ transform: showPaymentAmountInput ? 'rotate(90deg)' : 'rotate(0deg)' }"
        >
          ›
        </button>
      </div>
      <div v-if="showPaymentAmountInput">
        <PaymentAmountInput v-model="paymentAmount" />
      </div>
    </div>

    <!-- 카드 추천 받기 -->
    <div v-if="hasCards" style="margin-bottom: var(--space-md); padding: 0 var(--space-md);">
      <div style="background: linear-gradient(135deg, rgba(var(--color-primary-rgb), 0.1) 0%, rgba(var(--color-primary-rgb), 0.05) 100%); border-radius: var(--radius-lg); padding: 16px; margin-bottom: 12px;">
        <h4 style="margin: 0 0 8px 0; font-size: 14px; font-weight: 700; color: var(--color-text-primary);">🎯 추천 카드 찾기</h4>
        <p style="margin: 0 0 12px 0; font-size: 12px; color: var(--color-text-secondary);">카테고리와 금액에 맞는 최적의 카드를 추천받으세요</p>
        <button
          @click="getCardRecommendations"
          :disabled="isRecommending"
          style="width: 100%; padding: 12px 16px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-md); font-weight: 700; font-size: 14px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; gap: 8px;"
          :style="{ opacity: isRecommending ? 0.7 : 1 }"
        >
          <span v-if="!isRecommending">✨ 카드 추천</span>
          <span v-else>⏳ 추천 중...</span>
        </button>
      </div>
    </div>

    <main style="flex: 1; display: flex; flex-direction: column;">
      <!-- 카드 없을 때 -->
      <div v-if="!hasCards" style="flex: 1; display: flex; align-items: center; justify-content: center;">
        <div style="text-align: center; display: flex; flex-direction: column; gap: 16px; width: 100%; padding: 0 var(--space-md); box-sizing: border-box;">
          <h2 style="margin: 0; font-size: 18px; font-weight: 700; color: var(--color-text-primary);">등록된 카드가 없어요</h2>
          <p style="margin: 0; font-size: 14px; color: var(--color-text-secondary);">카드를 등록하면 혜택과 소비 관리를 시작할 수 있습니다.</p>
          <button @click="$router.push('/cards/register')" style="width: 100%; padding: 12px 20px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-full); font-weight: 600; cursor: pointer; box-sizing: border-box;">카드 등록</button>
        </div>
      </div>

      <!-- 추천 결과 헤더 (추천 완료 후에만) -->
      <div v-if="showRecommendationResult" style="margin-bottom: 8px; padding: 0 16px;">
        <h3 style="margin: 0; font-size: 16px; font-weight: 700; color: var(--color-text-primary);">✨ 추천 결과</h3>
        <p style="margin: 4px 0 0 0; font-size: 11px; color: var(--color-text-secondary);">당신을 위한 최적의 카드</p>
      </div>

      <!-- 부채꼴 캐러셀 -->
      <div v-if="hasCards" ref="cardsContainerRef" class="cards-carousel"
           @touchstart="handleTouchStart"
           @touchmove="handleTouchMove"
           @touchend="handleTouchEnd">
        <!-- 스와이프 가이드 톨팁 -->
        <div v-if="showSwipeGuide" style="position: absolute; top: 20px; left: 50%; transform: translateX(-50%); background: rgba(0,0,0,0.8); color: white; padding: 8px 16px; border-radius: 20px; font-size: 12px; z-index: 100; white-space: nowrap; animation: fadeInOut 3s;">
          💡 좌측으로 스와이프하면 다른 카드를 볼 수 있습니다
        </div>

        <div class="cards-container">
          <div v-for="(card, index) in cards" :key="card.id"
               class="card-item"
               :class="{
                 flipping: isFlipping && index === flippingIndex,
                 recommended: recommendedCardIds.includes(card.userCardId)
               }"
               :style="{
                 ...getCardStyle(index),
                 transitionDelay: `${index * 0.08}s`
               }"
               @click="selectCard(index)">
            <!-- 추천 배지 (카드 위) -->
            <div v-if="recommendedInfo[card.userCardId]" class="card-rank-badge">
              {{ ['🥇', '🥈', '🥉'][recommendedInfo[card.userCardId].rank - 1] }}
              {{ recommendedInfo[card.userCardId].rank }}위
            </div>

            <!-- 추천 근거 (카드 위) -->
            <div v-if="recommendedInfo[card.userCardId]" class="card-reason-text">
              {{ recommendedInfo[card.userCardId].reason }}
            </div>

            <div class="card-flip">
              <div class="card-front">
                <img :src="card.image" :alt="card.name" />
                <p class="card-name">{{ card.name }}</p>
              </div>
              <div class="card-back">
                <div class="auth-screen">인증 화면</div>
              </div>
            </div>
          </div>

          <!-- 카드 추가 버튼 -->
          <button
            class="card-add-button"
            :style="{
              ...getCardStyle(cards.length),
              transitionDelay: `${cards.length * 0.08}s`
            }"
            @click="goToCardRegister"
            type="button"
          >
            <div class="add-icon">+</div>
          </button>
        </div>

        <!-- 추천 이유 + 혜택 (추천 받았을 때만) -->
        <div v-if="currentCard && recommendedCardIds.length > 0 && recommendedInfo[currentCard.userCardId]" class="benefits-section" style="position: absolute; bottom: 0; left: 0; right: 0; width: 100%; padding: 0 var(--space-md); box-sizing: border-box;">
          <h3 class="benefits-title">{{ currentCard.name }}</h3>
          <p class="benefits-reason">💡 {{ recommendedInfo[currentCard.userCardId].reason }}</p>
          <h4 style="margin: 8px 0 6px 0; font-size: 12px; font-weight: 700; color: var(--color-text-primary);">주요 혜택</h4>
          <ul class="benefits-list">
            <li v-for="(benefit, idx) in currentBenefits.slice(0, 2)" :key="idx" class="benefits-item">
              • {{ typeof benefit === 'string' ? benefit : benefit.benefitName }}
            </li>
          </ul>
        </div>

        <!-- 주요 혜택만 (평소) -->
        <div v-else-if="currentCard && currentBenefits.length > 0" class="benefits-section" style="position: absolute; bottom: 430px; left: 140px; right: 0; width: 400%; padding: 0 var(--space-md); box-sizing: border-box;">
          <h3 class="benefits-title">{{ currentCard.name }} 주요 혜택</h3>
          <ul class="benefits-list">
            <li v-for="(benefit, idx) in currentBenefits" :key="idx" class="benefits-item">
              • {{ typeof benefit === 'string' ? benefit : benefit.benefitName }}
            </li>
          </ul>
        </div>
      </div>

      <!-- 모달들 -->
      <PaymentPasswordModal v-if="showPasswordModal"
                            :style="getModalStyle()"
                            @success="handlePasswordSuccess"
                            @close="handlePasswordClose" />
      <PaymentQRModal v-if="showQRModal"
                      :qr-token="qrToken"
                      :qr-image-url="qrImageUrl"
                      @close="handleQRClose"
                      @timeout="handleQRTimeout"
                      @success="handleQRSuccess" />
      <PaymentTimeoutModal v-if="showTimeoutModal"
                           @regenerate="handleTimeoutRegenerate"
                           @cancel="handleTimeoutCancel" />

      <!-- 토스트 알림 -->
      <ToastNotification v-if="showToast"
                         type="success"
                         :message="toastMessage"
                         @close="showToast = false" />

      <!-- 결제 버튼 -->
      <button v-if="hasCards" class="payment-btn" @click="payment">결제하기</button>
    </main>

    <BottomNavigation />
  </div>
</template>


<style scoped>
.payment-page {
  padding: var(--space-md);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
  background: var(--color-bg);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
}

main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-start;
  flex: 1;
  width: 100%;
  overflow-y: auto;
  gap: var(--space-md);
}

/* ===== 부채꼴 캐러셀 ===== */
.cards-carousel {
  width: 100%;
  min-height: 450px;
  display: flex;
  align-items: center;
  justify-content: center;
  touch-action: manipulation;
}

.cards-container {
  position: absolute;
  bottom: 80px; 
  left: 42%;
  width: 100%;
  transform: translateX(-50%);
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-item {
  position: absolute;
  width: clamp(140px, 20vw, 180px);
  height: clamp(220px, 30vh, 280px);
  transform-origin: bottom center;
  perspective: 1000px;
  cursor: pointer;
  top: 50%;
  left: 55%;
  border: 3px solid transparent;
  box-sizing: border-box;
  will-change: transform;
  backface-visibility: hidden;
  transition: transform 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94), opacity 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94), border 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.card-item.recommended {
  border-color: var(--color-primary);
  box-shadow: 0 0 12px rgba(var(--color-primary-rgb), 0.3);
}

/* 카드 추가 버튼 */
.card-add-button {
  position: absolute;
  width: clamp(140px, 20vw, 180px);
  height: clamp(220px, 30vh, 280px);
  transform-origin: bottom center;
  perspective: 1000px;
  cursor: pointer;
  top: 50%;
  left: 55%;
  border: none;
  border-radius: 16px;
  box-sizing: border-box;
  will-change: transform;
  backface-visibility: hidden;
  transition: transform 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94), opacity 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94), background 0.3s ease;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.card-add-button:hover {
  background: rgba(var(--color-primary-dark-rgb), 0.8);
}

.add-icon {
  font-size: clamp(48px, 12vw, 64px);
  font-weight: var(--font-bold);
  color: var(--color-btn-primary-text);
  line-height: 1;
}

/* 추천 배지 */
.card-rank-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--color-primary);
  color: var(--color-btn-primary-text);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(var(--color-primary-rgb), 0.3);
}

/* 추천 근거 텍스트 */
.card-reason-text {
  position: absolute;
  top: -32px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  max-width: 140px;
  text-align: center;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  z-index: 10;
}

.card-flip {
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  transform-origin: center center;
  background: var(--color-surface);
  border-radius: 12px;
}

.card-item.flipping .card-flip {
  animation: flip-card 0.8s forwards;
}

.card-item.flipped .card-flip {
  transform: rotateY(180deg);
}

@keyframes flip-card {
  0% {
    transform: rotateY(0deg);
  }
  100% {
    transform: rotateY(180deg);
  }
}

/* 카드 회전 애니메이션 (추천 받기) */
@keyframes rotate-carousel {
  0% {
    transform: rotateZ(0deg);
  }
  100% {
    transform: rotateZ(360deg);
  }
}

.card-front,
.card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  padding: var(--space-md);
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.95) 0%,
    var(--color-surface) 100%
  );
  border-radius: var(--radius-xl);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  backface-visibility: hidden;
}

[data-theme='dark'] .card-front,
[data-theme='dark'] .card-back {
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.08) 0%,
    var(--color-surface) 100%
  );
}

.card-back {
  transform: rotateY(180deg);
}

.card-front img {
  width: 100%;
  height: auto;
  border-radius: var(--radius-lg);
  aspect-ratio: 1.586;
  object-fit: cover;
}

.card-name {
  margin: 0;
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  text-align: center;
}

.auth-screen {
  font-size: var(--font-md);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

/* 카드 뒷면에서 튀어나오는 모달 */
.modal-popup {
  position: absolute;
  top: 50%;
  left: 50%;
  transform-origin: center center;
  width: 320px;
  margin-left: -160px;
  margin-top: -180px;
  margin: 0 auto;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  pointer-events: auto;
  z-index: 50;
}

.benefits-section {
  margin-top: 24px;
  margin-bottom: var(--space-md);
  padding: 0 var(--space-md);
  flex-shrink: 0;
  min-height: zz0px;
}

.benefits-title {
  margin: 0 0 var(--space-xs) 0;
  font-size: var(--font-sm);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.benefits-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.benefits-item {
  font-size: var(--font-xs);
  color: var(--color-text-secondary);
}

.recommend-btn {
  background: none;
  border: none;
  color: var(--color-primary-dark);
  font-size: var(--font-xs);
  padding: 0;
  cursor: pointer;
  text-decoration: underline;
  transition: var(--transition-fast);
  margin-top: var(--space-sm);
  font-weight: var(--font-semibold);
}

.recommend-btn:hover:not(:disabled) {
  opacity: 0.7;
}

.recommend-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.payment-btn {
   position: absolute;
  bottom: 60px;   /* 네비게이션 높이에 맞춰 조정 */
  width: clamp(80px, 12vw, 100px);
  height: clamp(80px, 12vw, 100px);
  border-radius: 50%;
  border: none;
  background: var(--color-primary);
  color: var(--color-btn-primary-text);
  font-size: var(--font-sm);
  font-weight: var(--font-bold);
  cursor: pointer;
  box-shadow: 0 12px 32px rgba(var(--color-primary-dark-rgb), 0.35);
  z-index: 20;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  margin: var(--space-lg) auto 0;
  align-self: center;
  transform: scale(1);
   margin-top: auto;   /* 자동으로 아래쪽으로 밀림 */
    margin-bottom: 20px; /* 네비게이션 위에 여백 */
}

.payment-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 16px 40px rgba(255, 195, 0, 0.45);
}

.payment-btn:active {
  transform: scale(0.95);
}

.pupil {
  width: 10px;
  height: 10px;
  background: var(--color-bg);
  border-radius: 50%;
  transition: transform 0.1s ease-out;
}

/* 인증 모달 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.password-modal {
  background: white;
  border-radius: var(--radius-xl);
  padding: var(--space-2xl);
  width: 90%;
  max-width: 320px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

[data-theme='dark'] .password-modal {
  background: var(--color-surface);
  color: var(--color-text-primary);
}

.password-modal h2 {
  margin: 0 0 var(--space-sm) 0;
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
}

.password-modal p {
  margin: 0 0 var(--space-xl) 0;
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

.password-input {
  width: 100%;
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-md);
  margin-bottom: var(--space-md);
  box-sizing: border-box;
  letter-spacing: 0.2em;
}

.password-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.modal-btn {
  width: 100%;
  padding: var(--space-md);
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-weight: var(--font-bold);
  cursor: pointer;
  transition: all 0.2s;
}

.modal-btn:hover {
  opacity: 0.9;
}

.modal-btn:active {
  transform: scale(0.98);
}

/* 비밀번호 표시 */
.password-display {
  display: flex;
  justify-content: center;
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
}

.password-dot {
  font-size: var(--font-2xl);
  color: var(--color-primary);
}

/* 숫자패드 */
.keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-sm);
  margin-bottom: var(--space-xl);
}

.keypad-btn {
  aspect-ratio: 1;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  cursor: pointer;
  transition: all 0.2s;
}

.keypad-btn:hover:not(:disabled) {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.keypad-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.keypad-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-btn {
  font-size: var(--font-xl);
}

.recommend-section {
  width: 100%;
}

.recommend-btn {
  background: none;
  border: none;
  color: var(--color-text-primary);
  font-size: var(--font-xs);
  font-weight: var(--font-semibold);
  cursor: pointer;
  text-decoration: underline;
  padding: var(--space-sm) 0;
  transition: all 0.2s;
}

.recommend-btn:hover {
  opacity: 0.7;
}

.recommend-btn:active {
  opacity: 0.5;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-lg);
  text-align: center;
  width: 100%;
}

.empty-state h2 {
  margin: 0;
  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.empty-state p {
  margin: 0;
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

.register-card-btn {
  padding: 12px 20px;
  background: var(--color-primary);
  color: var(--color-btn-primary-text);
  border: none;
  border-radius: var(--radius-full);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.register-card-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.register-card-btn:active {
  transform: translateY(0);
  opacity: 0.8;
}

/* 톨팁 페이드인/아웃 애니메이션 */
@keyframes fadeInOut {
  0% {
    opacity: 0;
    transform: translateX(-50%) translateY(-8px);
  }
  10% {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
  90% {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
  100% {
    opacity: 0;
    transform: translateX(-50%) translateY(-8px);
  }
}

</style>
