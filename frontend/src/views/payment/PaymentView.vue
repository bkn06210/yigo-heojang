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
import { useToast } from '@/composables/useToast';
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
const { showToast: displayToast } = useToast();

const { user } = storeToRefs(authStore);
const { cards: storeCards } = storeToRefs(cardStore);
const hasCards = computed(() => storeCards.value.length > 0);

// 로그인 상태
const isLogin = computed(() => authStore.isLogin());

// 카드 등록 페이지로 이동
const goToCardRegister = () => {
  router.push('/cards/register');
};

// 로그인 페이지로 이동
const goLogin = () => {
  router.push('/auth/login');
};

const CAROUSEL_CONFIG = {
  // Flip 관련 임계값
  FLIP_THRESHOLD: 100,
  FLIP_COMPLETE_THRESHOLD: 0.5,
  FLIP_MAX_OFFSET: 300,

  // Swipe 관련 임계값
  HORIZONTAL_SWIPE_THRESHOLD: 50,
  SWIPE_DIRECTION_THRESHOLD: 10,
  SWIPE_COMPLETE_THRESHOLD: 0.2,
  SWIPE_BASE_DIVISOR: 300,

  // 타이밍 (ms)
  MODAL_SHOW_DELAY: 400,
  SWIPE_GUIDE_DELAY: 1000,
  SWIPE_GUIDE_DURATION: 3000,
  SWIPE_ANIMATION_DURATION: 400,
  RECOMMENDATION_MERGE_DURATION: 800,
  PASSWORD_SUBMIT_DELAY: 200,

  // 카드 각도 (degrees)
  CARD_ANGLE_TWO_SIDE: 40,
  CARD_ANGLE_DEPTH_1: 45,
  CARD_ANGLE_DEPTH_2: 90,

  // 카드 간격 (px)
  CARD_SPREAD_TWO: 100,
  CARD_SPREAD_DEPTH_1: 70,
  CARD_SPREAD_DEPTH_2: 60,

  // 카드 높이 (px)
  CARD_LIFT_DEFAULT: 80,
  CARD_LIFT_RECOMMEND: 100,
  CARD_LIFT_TWO: 50,
  CARD_LIFT_DEPTH_1: 60,
  CARD_LIFT_DEPTH_2: 0,

  // 기타
  CARD_SCALE_INACTIVE: 0.9,
  CARD_OPACITY_INACTIVE: 0.7,
  SWIPE_SMOOTHING_FACTOR: 0.3,
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

// 모달 상태 통합
const modalState = ref({
  password: false,
  qr: false,
  timeout: false,
});

// 플립/인터랙션 상태 통합
const flipState = ref({
  progress: 0,
  index: null,
  offset: 0,
  isActive: false,
});

const cardUpOffset = ref(0);
const showToast = ref(false);
const toastMessage = ref('');
const qrToken = ref(null);
const qrImageUrl = ref(null);
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

/**
 * 카드 도안 방향을 표시한다.
 *
 * 슬롯은 세로인데 카드 도안은 대부분 가로다. 그대로 넣으면 가운데 띠처럼 들어가고 위아래가 빈다.
 * 가로 도안만 세워서 슬롯을 채우고, 이미 세로인 도안(삼성 iD ON 등)은 건드리지 않는다.
 * 방향을 코드에 적어두지 않고 실제 이미지 크기로 판정한다 — 카드가 추가돼도 손댈 필요가 없다.
 */
const markCardOrientation = (event) => {
  const img = event.target;
  img.classList.toggle('is-landscape', img.naturalWidth > img.naturalHeight);
};

// 부채꼴 배치 (카드 수에 따라 자동 각도 분배) / 추천 모드 선형 배치
const getCardStyle = (index) => {
  const centerIndex = selectedIndex.value;
  const cardCount = cards.value.length;

  // 플립 중인 카드: 올라가면서 점점 뒤집어짐
  if (index === flipState.value.index && flipState.value.progress > 0) {
    const flipAngle = flipState.value.progress * 180;
    return {
      transform: `translate(-50%, -100%) translateY(-${CAROUSEL_CONFIG.CARD_LIFT_DEFAULT + cardUpOffset.value}px) rotateY(${flipAngle}deg)`,
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
    yLift = isRecommending.value ? CAROUSEL_CONFIG.CARD_LIFT_RECOMMEND : CAROUSEL_CONFIG.CARD_LIFT_DEFAULT;
  } else if (cardCount === 2) {
    // 2장: 좌우 균형 배치
    angle = offset * CAROUSEL_CONFIG.CARD_ANGLE_TWO_SIDE;
    spread = CAROUSEL_CONFIG.CARD_SPREAD_TWO;
    yLift = isRecommending.value ? CAROUSEL_CONFIG.CARD_LIFT_RECOMMEND : CAROUSEL_CONFIG.CARD_LIFT_TWO;
  } else {
    // 3장 이상: 부채꼴 배치
    const maxOffset = Math.floor(cardCount / 2);
    const depthFactor = Math.abs(offset);

    if (depthFactor === 0) {
      angle = 0;
      spread = 0;
      yLift = CAROUSEL_CONFIG.CARD_LIFT_DEFAULT;
    } else if (depthFactor === 1) {
      angle = offset > 0 ? CAROUSEL_CONFIG.CARD_ANGLE_DEPTH_1 : -CAROUSEL_CONFIG.CARD_ANGLE_DEPTH_1;
      spread = CAROUSEL_CONFIG.CARD_SPREAD_DEPTH_1;
      yLift = CAROUSEL_CONFIG.CARD_LIFT_DEPTH_1;
    } else {
      angle = offset > 0 ? CAROUSEL_CONFIG.CARD_ANGLE_DEPTH_2 : -CAROUSEL_CONFIG.CARD_ANGLE_DEPTH_2;
      spread = CAROUSEL_CONFIG.CARD_SPREAD_DEPTH_2;
      yLift = CAROUSEL_CONFIG.CARD_LIFT_DEPTH_2;
    }
  }

  const x = offset * spread;
  const yCorrection = Math.abs(angle) * 0.3;

  // 플립 중일 때 다른 카드들은 고정 (cardUpOffset 무시)
  if (flipState.value.progress > 0 && index !== flipState.value.index) {
    return {
      transform: `translate(-50%, -100%) translate(${x}px, -${yCorrection + yLift}px) rotate(${angle}deg) scale(${index === centerIndex ? 1 : CAROUSEL_CONFIG.CARD_SCALE_INACTIVE})`,
      opacity: index === centerIndex ? 1 : CAROUSEL_CONFIG.CARD_OPACITY_INACTIVE,
      zIndex: 10 - Math.abs(offset),
    };
  }

  // isAnimatingToCenter 상태에서는 cardUpOffset 무시
  const upOffset = isAnimatingToCenter.value ? 0 : (index === centerIndex ? cardUpOffset.value : 0);
  const isCenter = isAnimatingToCenter.value || index === centerIndex;
  const scale = isCenter ? 1 : CAROUSEL_CONFIG.CARD_SCALE_INACTIVE;
  const opacity = isCenter ? 1 : CAROUSEL_CONFIG.CARD_OPACITY_INACTIVE;
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

// 추천 응답을 파싱하여 필요한 정보 추출
const parseRecommendationResponse = (recommendations) => {
  const cardIds = recommendations.map(item => item.userCardId).slice(0, 3);
  const info = {};

  recommendations.slice(0, 3).forEach((item, idx) => {
    info[item.userCardId] = {
      rank: item.rank ?? idx + 1,
      reason: item.reason || '',
      expectedBenefit: Number(item.expectedBenefit || 0),
      isEstimate: Boolean(item.isEstimate),
    };
  });

  return { cardIds, info };
};

// 추천 상태 업데이트 및 카드 재정렬
const updateRecommendationState = (cardIds, info) => {
  recommendedCardIds.value = cardIds;
  recommendedInfo.value = info;
  cardStore.reorderCardsByRecommendation(cardIds);
};

// 추천 애니메이션 실행
const animateToRecommendation = (targetCardId) => {
  if (recommendedCardIds.value.length === 0) {
    isRecommending.value = false;
    console.error('추천 카드가 없습니다.');
    return;
  }

  const targetIndex = cards.value.findIndex(c => Number(c.id) === Number(targetCardId));

  if (targetIndex === -1) {
    isRecommending.value = false;
    console.error('목표 카드를 찾을 수 없습니다.');
    return;
  }

  const cardCount = cards.value.length;
  const startIndex = selectedIndexFloat.value;
  const centerValue = Math.floor(cardCount / 2);

  isAnimatingToCenter.value = true;
  const stage1Start = Date.now();
  const stage1Duration = CAROUSEL_CONFIG.RECOMMENDATION_MERGE_DURATION;

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
      setTimeout(() => {
        cardStore.reorderCardsByRecommendation(recommendedCardIds.value);
        selectedIndexFloat.value = 0;
        isAnimatingToCenter.value = false;
        isRecommendationMode.value = true;
        showRecommendationResult.value = true;
      }, CAROUSEL_CONFIG.RECOMMENDATION_MERGE_DURATION);
      isRecommending.value = false;
    }
  };

  animate1();
};

// 카드 추천 받기
const getCardRecommendations = async () => {
  if (isRecommending.value) return;

  try {
    isRecommending.value = true;
    showRecommendationResult.value = true;

    const categoryId = selectedCategory.value
      ? personalizationStore.categories.find(cat => cat.label === selectedCategory.value)?.id
      : null;

    const expectedAmount = (paymentAmount.value && paymentAmount.value > 0) ? paymentAmount.value : null;

    const response = await getRecommendations({
      categoryId,
      merchantId: null,
      expectedAmount,
      paymentType: 'CARD',
    });

    const recommendations = response?.data?.recommendations || [];
    const { cardIds, info } = parseRecommendationResponse(recommendations);

    updateRecommendationState(cardIds, info);

    // 스와이프 가이드 톨팁 표시
    setTimeout(() => {
      showSwipeGuide.value = true;
      setTimeout(() => {
        showSwipeGuide.value = false;
      }, CAROUSEL_CONFIG.SWIPE_GUIDE_DURATION);
    }, CAROUSEL_CONFIG.SWIPE_GUIDE_DELAY);

    // 추천 애니메이션 시작
    if (cardIds.length > 0) {
      animateToRecommendation(cardIds[0]);
    }
  } catch (error) {
    console.error('카드 추천 API 에러:', error?.response?.data || error.message);
    isRecommending.value = false;
  }
};

// 스와이프 끝났을 때 처리
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
  const smoothFactor = CAROUSEL_CONFIG.SWIPE_SMOOTHING_FACTOR;
  const deltaX = rawDeltaX * smoothFactor + prevDeltaX * (1 - smoothFactor);
  const deltaY = rawDeltaY * smoothFactor + prevDeltaY * (1 - smoothFactor);

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
    cardUpOffset.value = Math.min(deltaY, CAROUSEL_CONFIG.FLIP_MAX_OFFSET);
    flipState.value.progress = Math.min(deltaY / CAROUSEL_CONFIG.FLIP_MAX_OFFSET, 1);

    if (deltaY > CAROUSEL_CONFIG.FLIP_THRESHOLD && !flipState.value.index) {
      flipState.value.index = selectedIndex.value;
    }

    if (flipState.value.progress >= 0.5) {
      modalState.value.password = true;
    }
  }
  // 좌우로 드래그할 때: 카드 회전
  else if (Math.abs(deltaX) > CAROUSEL_CONFIG.SWIPE_DIRECTION_THRESHOLD) {
    swipeProgress.value = deltaX / CAROUSEL_CONFIG.SWIPE_BASE_DIVISOR;
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


  // 가로 스와이프로 카드 회전
  if (Math.abs(swipeProgress.value) > CAROUSEL_CONFIG.SWIPE_COMPLETE_THRESHOLD && flipState.value.progress === 0) {
    isSwipeAnimating.value = true;

    // 추천 모드 진입: 모든 카드가 겹쳐있는 상태에서 스와이프하면 펼쳐짐
    if (isAnimatingToCenter.value && !isRecommendationMode.value) {
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
      isRecommendationMode.value = false;
      showRecommendationResult.value = false;
    }

    setTimeout(() => {
      swipeProgress.value = 0;
      isSwipeAnimating.value = false;
    }, CAROUSEL_CONFIG.SWIPE_ANIMATION_DURATION);
    return;
  }

  // 스와이프 진행도 복원
  if (Math.abs(swipeProgress.value) > 0) {
    swipeProgress.value = 0;
  }

  // 50% 이상 올렸으면 자동으로 1까지 완성 (spring animation)
  if (flipState.value.progress >= 0.5) {
    cardUpOffset.value = 0;
    flipState.value.progress = 1;
    setTimeout(() => {
      modalState.value.password = true;
    }, CAROUSEL_CONFIG.MODAL_SHOW_DELAY);
  } else {
    // 50% 미만이면 자동으로 0으로 복원 (spring back)
    cardUpOffset.value = 0;
    flipState.value.progress = 0;
    flipState.value.index = null;
  }
};

const selectCard = (index) => {
  selectedIndexFloat.value = index;
  isRecommendationMode.value = false;
  showRecommendationResult.value = false;
};

const handlePasswordSuccess = async () => {
  modalState.value.password = false;

  try {
    if (!currentCard.value || !currentCard.value.id) {
      throw new Error('카드를 선택해주세요');
    }

    // QR 코드 생성
    const response = await createPaymentQr(currentCard.value.id, paymentAmount.value);

    qrToken.value = response.qrToken || response.token;
    qrImageUrl.value = response.qrImage || response.imageUrl;

    modalState.value.qr = true;
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
  if (flipState.value.progress < CAROUSEL_CONFIG.FLIP_COMPLETE_THRESHOLD) {
    return { opacity: 0, pointerEvents: 'none' };
  }
  const modalProgress = (flipState.value.progress - CAROUSEL_CONFIG.FLIP_COMPLETE_THRESHOLD) / CAROUSEL_CONFIG.FLIP_COMPLETE_THRESHOLD;
  return {
    opacity: modalProgress,
    transform: `translateY(${(1 - modalProgress) * 50}px)`,
    transition: 'none',
    pointerEvents: modalProgress < 1 ? 'none' : 'auto',
  };
};

// 인터랙션 상태 초기화 (중복 제거)
const resetInteractionState = () => {
  flipState.value = { progress: 0, index: null, offset: 0, isActive: false };
  cardUpOffset.value = 0;
  isDragging = false;
  swipeDirection = null;
};

const resetModalState = () => {
  modalState.value = { password: false, qr: false, timeout: false };
};

const handlePasswordClose = () => {
  modalState.value.password = false;
  resetInteractionState();
};

const handleQRClose = () => {
  modalState.value.qr = false;
  resetInteractionState();
};

const handleQRSuccess = () => {
  modalState.value.qr = false;
  resetInteractionState(); // 카드를 원래 자리로 돌려놓기
  toastMessage.value = '결제 성공';
  showToast.value = true;
};

const handleQRCancel = () => {
  modalState.value.password = false;
  resetInteractionState();
};

const handleQRTimeout = () => {
  modalState.value.qr = false;
  modalState.value.timeout = true;
};

const handleTimeoutRegenerate = () => {
  modalState.value.timeout = false;
  modalState.value.qr = true;
};

const handleTimeoutCancel = () => {
  modalState.value.timeout = false;
};

const payment = async () => {
  try {
    if (!currentCard.value?.id) return;
    const qrData = await createPaymentQr(currentCard.value.id, 10000);
    flipState.value.progress = 1;
    modalState.value.password = true;
  } catch (error) {
    console.error('결제 QR 생성 실패:', error);
  }
};

const closeModal = () => {
  modalState.value.password = false;
  resetInteractionState();
};

onMounted(async () => {
  if (!isLogin.value) return;

  await cardStore.loadCards();

  // 카드 스와이프 힌트 (처음 1회만)
  if (!localStorage.getItem('paymentSwipeHintShown') && cards.value.length > 0) {
    displayToast('info', '💡 카드를 스와이프해서 선택하세요');
    localStorage.setItem('paymentSwipeHintShown', 'true');
  }
});
</script>

<template>
  <div class="payment-page">
    <PageHeader title="결제" :show-back="false" />

    <!-- 이용 장소 선택 -->
    <div v-if="isLogin && hasCards" class="merchant-section" style="margin-top: var(--space-sd); margin-bottom: var(--space-md);">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <h3 style="margin: 0; font-size: 16px; font-weight: 700; color: var(--color-text-primary);">이용 장소</h3>
        <button
          @click="router.push('/settings/personalization')"
          style="background: none; border: none; color: var(--color-text-secondary); font-size: 12px; cursor: pointer; padding: 4px 8px; text-decoration: underline; transition: color 0.2s;"
          @mouseover="$event.target.style.color = 'var(--color-text-primary)'"
          @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'"
        >
          개인화 설정
        </button>
      </div>
      <!-- 업종 선택 (개인화 설정에서 카테고리를 저장한 경우만 표시) -->
      <MerchantSelector
        v-if="personalizationStore.getActiveCategories().length > 0"
        v-model:category="selectedCategory"
        v-model:merchant="selectedMerchant"
      />
    </div>

    <!-- 결제 금액 입력 -->
    <div v-if="hasCards" id="payment-info-section" class="payment-amount-section" style="margin-bottom: var(--space-md);">
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
    <div id="recommendation-section" v-if="hasCards" style="margin-bottom: var(--space-md); padding: 0 var(--space-md);">
      <div style="background: linear-gradient(135deg, rgba(var(--color-primary-rgb), 0.1) 0%, rgba(var(--color-primary-rgb), 0.05) 100%); border-radius: var(--radius-lg); padding: 16px; margin-bottom: 12px;">
        <h4 style="margin: 0 0 8px 0; font-size: 14px; font-weight: 700; color: var(--color-text-primary);">🎯 추천 카드 찾기</h4>
        <p style="margin: 0 0 12px 0; font-size: 12px; color: var(--color-text-secondary);">카테고리와 금액에 맞는 최적의 카드를 추천받으세요</p>
        <button
          @click="getCardRecommendations"
          :disabled="isRecommending"
          style="width: 100%; padding: 12px 16px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-md); font-weight: 700; font-size: 14px; cursor: pointer; transition: all 0.2s;"
          :style="{ opacity: isRecommending ? 0.7 : 1 }"
        >
          {{ isRecommending ? '추천 중...' : '카드 추천' }}
        </button>
      </div>
    </div>

    <!-- 비로그인 -->
    <div v-if="!isLogin" style="flex: 1; display: flex; align-items: center; justify-content: center;">
      <div style="text-align: center; display: flex; flex-direction: column; gap: 16px;">
        <h2 style="margin: 0; font-size: 18px; font-weight: 700; color: var(--color-text-primary);">로그인이 필요합니다</h2>
        <p style="margin: 0; font-size: 14px; color: var(--color-text-secondary);">결제하려면 로그인해주세요.</p>
        <button @click="goLogin" style="padding: 12px 20px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-full); font-weight: 600; cursor: pointer;">로그인</button>
      </div>
    </div>

    <main v-else style="flex: 1; display: flex; flex-direction: column;">
      <!-- 카드 없을 때 -->
      <div v-if="!hasCards" style="flex: 1; display: flex; align-items: center; justify-content: center;">
        <div style="text-align: center; display: flex; flex-direction: column; gap: 16px; width: 100%; padding: 0 var(--space-md); box-sizing: border-box;">
          <h2 style="margin: 0; font-size: 18px; font-weight: 700; color: var(--color-text-primary);">등록된 카드가 없어요</h2>
          <p style="margin: 0; font-size: 14px; color: var(--color-text-secondary);">카드를 등록하면 혜택과 소비 관리를 시작할 수 있습니다.</p>
          <button @click="$router.push('/cards/register')" style="width: 100%; padding: 12px 20px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-full); font-weight: 600; cursor: pointer; box-sizing: border-box;">카드 등록</button>
        </div>
      </div>

      <!-- 혜택 패널.
           캐러셀 안에 두면 .cards-carousel 에 position 이 없어 훨씬 위쪽 조상 기준으로 붙는다 —
           그래서 화면 맨 위 "결제" 제목까지 밀고 올라갔다.
           정상 흐름으로 빼면 카드 위에 자연스럽게 쌓이고 겹칠 일이 없다. -->

      <!-- 혜택 + 캐러셀 통합 영역 -->
      <div class="payment-carousel-area">
        <!-- 추천 결과 (추천 받았을 때).
             카드가 원래 가진 "주요 혜택" 목록이 아니라, 이번 결제 조건으로 엔진이 계산한 값이다. -->
        <div v-show="hasCards && recommendedCardIds.length > 0 && currentCard && recommendedInfo?.[currentCard?.id]"
             class="benefits-section">
          <p style="margin: 0 0 12px 0; font-size: 14px; font-weight: 600; color: var(--color-text-primary);">추천 결과</p>
          <h3 class="benefits-title">
            {{ recommendedInfo?.[currentCard?.id]?.rank }}위 · {{ currentCard?.name }}
            <span style="font-size: 12px; font-weight: 500; color: var(--color-text-secondary); margin-left: 8px;">
              [본인] {{ currentCard?.cardNumber?.slice(-4, -1) }}*
            </span>
          </h3>
          <p class="benefits-benefit">
            예상 혜택 {{ recommendedInfo?.[currentCard?.id]?.expectedBenefit.toLocaleString('ko-KR') }}원
            <span v-if="recommendedInfo?.[currentCard?.id]?.isEstimate" class="benefits-estimate">(예상)</span>
          </p>
          <p v-if="recommendedInfo?.[currentCard?.id]?.reason" class="benefits-reason">
            💡 {{ recommendedInfo?.[currentCard?.id]?.reason }}
          </p>
        </div>

        <!-- 주요 혜택 (추천 전에만).
             추천을 받은 뒤에는 추천 카드가 아닌 카드로 넘겨도 이 패널이 튀어나오지 않게 한다 —
             추천 결과와 카드 소개가 번갈아 뜨면 무엇을 보고 있는지 알 수 없다. -->
        <div v-show="hasCards && recommendedCardIds.length === 0 && currentCard"
             class="benefits-section">
          <p style="margin: 0 0 12px 0; font-size: 14px; font-weight: 600; color: var(--color-text-primary);">카드 주요 혜택</p>
          <h3 class="benefits-title">
            {{ currentCard?.name }}
            <span style="font-size: 12px; font-weight: 500; color: var(--color-text-secondary); margin-left: 8px;">
              [본인] {{ currentCard?.cardNumber?.slice(-4, -1) }}*
            </span>
          </h3>
          <ul v-if="currentBenefits.length > 0" class="benefits-list">
            <li v-for="(benefit, idx) in currentBenefits" :key="idx" class="benefits-item">
              💡 {{ typeof benefit === 'string' ? benefit : benefit.benefitName }}
            </li>
          </ul>
          <p v-else class="benefits-reason">
            💡 적용 가능한 혜택 없음
          </p>
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

        <div id="cards-container" class="cards-container">
          <div v-for="(card, index) in cards" :key="card.id"
               class="card-item"
               :class="{
                 flipping: flipState.isActive && index === flipState.index,
                 recommended: recommendedCardIds.some(id => Number(id) === Number(card.id))
               }"
               :style="{
                 ...getCardStyle(index),
                 transitionDelay: `${index * 0.08}s`
               }"
               @click="selectCard(index)">
            <!-- 추천 배지 (카드 위) -->
            <div v-if="recommendedInfo?.[card?.id]" class="card-rank-badge">
              {{ ['🥇', '🥈', '🥉'][recommendedInfo?.[card?.id]?.rank - 1] }}
              {{ recommendedInfo?.[card?.id]?.rank }}위
            </div>

            <div class="card-flip">
              <div class="card-front">
                <div class="card-art">
                  <img :src="card.image" :alt="card.name" @load="markCardOrientation" />
                </div>
              </div>
              <div class="card-back">
                <div class="auth-screen">인증 화면</div>
              </div>
            </div>
          </div>

          <!-- 카드 추가 버튼 (추천 중이 아닐 때만 표시) -->
          <button
            v-if="!isRecommending && !showRecommendationResult"
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

      </div>
      </div>

      <!-- 모달들 -->
      <PaymentPasswordModal v-if="modalState.password"
                            :style="getModalStyle()"
                            @success="handlePasswordSuccess"
                            @close="handlePasswordClose" />
      <PaymentQRModal v-if="modalState.qr"
                      :qr-token="qrToken"
                      :qr-image-url="qrImageUrl"
                      @close="handleQRClose"
                      @timeout="handleQRTimeout"
                      @success="handleQRSuccess" />
      <PaymentTimeoutModal v-if="modalState.timeout"
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

/* ===== 섹션 스타일 ===== */
.merchant-section,
.payment-amount-section {
  width: 100%;
  padding: var(--space-md);
  border-bottom: 1px solid var(--color-border);
  box-sizing: border-box;
}

.merchant-section {
  background: rgba(var(--color-primary-rgb), 0.02);
}

.recommendation-section {
  width: 100%;
  padding: var(--space-md);
  background: rgba(var(--color-primary-rgb), 0.05);
  border-radius: var(--radius-md);
  margin-top: var(--space-md);
  margin-bottom: var(--space-md);
  box-sizing: border-box;
}

.section-title {
  margin: 0 0 var(--space-sm) 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}

/* ===== 부채꼴 캐러셀 ===== */
.cards-carousel {
  width: 100%;
  min-height: 350px;
  display: flex;
  align-items: center;
  justify-content: center;
  touch-action: manipulation;
}

.cards-container {
  position: absolute;
  bottom: 60px; 
  left: 44.5%;   
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
  /* 테두리를 두면 카드 둘레에 회색 액자가 생긴다. 추천 강조는 카드 도안에 직접 준다. */
  border: none;
  box-sizing: border-box;
  will-change: transform;
  backface-visibility: hidden;
  transition: transform 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94), opacity 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

/* 추천 카드 강조. border 대신 box-shadow 링이라 크기가 밀리지 않고, 슬롯이 아니라 카드에 붙는다. */
.card-item.recommended .card-art {
  box-shadow:
    0 0 0 3px var(--color-primary),
    0 0 12px rgba(var(--color-primary-rgb), 0.3),
    0 12px 32px rgba(0, 0, 0, 0.18);
}

/* 카드 추가 버튼 - 약한 강조 */
.card-add-button {
  position: absolute;
  width: clamp(140px, 20vw, 180px);
  height: clamp(220px, 30vh, 280px);
  transform-origin: bottom center;
  perspective: 1000px;
  cursor: pointer;
  top: 50%;
  left: 55%;
  border: 2px solid var(--color-primary);
  border-radius: 16px;
  box-sizing: border-box;
  will-change: transform;
  backface-visibility: hidden;
  transition: transform 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94), opacity 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94), background 0.3s ease;
  background: rgba(var(--color-primary-rgb), 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.card-add-button:hover {
  background: rgba(var(--color-primary-rgb), 0.15);
  border-color: var(--color-primary-dark);
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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  backface-visibility: hidden;
}

/* 앞면은 카드 도안만 보이게 둔다. 흰 판을 깔면 카드가 액자에 끼워진 것처럼 보인다.
   그림자와 둥근 모서리는 도안(.card-art)이 직접 갖는다. */
.card-front {
  background: none;
  padding: 0;
  box-shadow: none;
}

/* 뒷면은 카드가 뒤집힌 자리라 판이 있어야 내용이 읽힌다 */
.card-back {
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
  transform: rotateY(180deg);
}

[data-theme='dark'] .card-back {
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.08) 0%,
    var(--color-surface) 100%
  );
}

/* 카드 도안 영역. 카드명 줄을 뺀 나머지 세로를 쓰되, 폭은 카드 비율로 정한다.
   상자를 슬롯 폭에 꽉 맞추면 카드 비율과 어긋나 좌우가 잘리거나 여백이 남는다.
   세운 카드 비율(1 / 1.586)로 잡아 두면 도안이 잘리지 않고 여백도 생기지 않는다.
   container-type: size 를 줘야 아래에서 cqw/cqh(이 상자의 폭·높이)를 쓸 수 있다. */
.card-art {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  width: auto;
  aspect-ratio: 1 / 1.586;
  margin: 0 auto;
  border-radius: var(--radius-lg);
  container-type: size;
  /* 흰 판을 없앴으므로 그림자를 도안이 직접 갖는다. 없으면 배경에 그대로 붙어 보인다. */
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
}

/* contain 이라 어떤 비율이 와도 도안이 잘리지 않는다.
   위 상자가 이미 카드 비율이라 표준 도안에서는 여백이 사실상 생기지 않는다. */
.card-art img {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: 100%;
  object-fit: contain;
  transform: translate(-50%, -50%);
}

/* 가로 도안은 90도 세워 세로 슬롯을 채운다.
   transform 은 레이아웃 크기를 바꾸지 않으므로, 회전 전에 폭·높이를 미리 뒤바꿔 잡는다.
   폭 = 상자 높이(100cqh), 높이 = 상자 폭(100cqw) 이라야 세운 뒤 상자에 꼭 맞는다. */
.card-art img.is-landscape {
  width: 100cqh;
  height: 100cqw;
  transform: translate(-50%, -50%) rotate(90deg);
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
  margin-top: var(--space-md);
  margin-bottom: var(--space-md);
  padding: 0 var(--space-md);
  flex-shrink: 0;
  min-height: 150px;
  overflow: hidden;
}

/* 추천 결과의 예상 혜택액. 카드 이름 다음으로 눈에 들어와야 하는 값이다. */
.benefits-benefit {
  margin: 0 0 var(--space-xs) 0;
  font-size: var(--font-md);
  font-weight: var(--font-bold);
  color: var(--color-primary-dark);
}

/* 확정이 아니라 예상이라는 표시. 금액과 같은 크기로 두면 확정처럼 읽힌다. */
.benefits-estimate {
  font-size: var(--font-xs);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
}

.benefits-title {
  margin: 0 0 var(--space-sm) 0;
  font-size: var(--font-md);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.benefits-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.benefits-item {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.benefits-reason {
  margin: 0;
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
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
  /* 카드와 붙여 놓는다. 값을 키울수록 버튼이 위로(카드 쪽으로) 올라온다. */
  bottom: 65px;
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
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.password-modal {
  background: white;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 320px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-sizing: border-box;
  max-height: 85vh;
  overflow-y: auto;
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
