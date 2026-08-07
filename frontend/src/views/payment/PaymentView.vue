<script setup>
import { ref, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { usePaymentStore } from '@/stores/payment';
import { useAuthStore } from '@/stores/authStore';
import { useCardStore } from '@/stores/cardStore';
import PageHeader from '@/components/common/PageHeader.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';
import ToastNotification from '@/components/common/ToastNotification.vue';
import PaymentPasswordModal from '@/components/payment/PaymentPasswordModal.vue';
import PaymentQRModal from '@/components/payment/PaymentQRModal.vue';
import PaymentTimeoutModal from '@/components/payment/PaymentTimeoutModal.vue';
import { createPaymentQr, getPaymentQr } from '@/api/paymentQrApi';
import { getPayment } from '@/api/walletApi';
import QRCode from 'qrcode';
const paymentStore = usePaymentStore();
const router = useRouter();
const authStore = useAuthStore();
const cardStore = useCardStore();

const { user } = storeToRefs(authStore);
const { cards: storeCards } = storeToRefs(cardStore);
const isLoggedIn = computed(() => !!user.value);
const hasCards = computed(() => storeCards.value.length > 0);

const CAROUSEL_CONFIG = {
  FLIP_THRESHOLD: 100,
  HORIZONTAL_SWIPE_THRESHOLD: 50,
  MODAL_SHOW_DELAY: 400,
};

const cards = computed(() => storeCards.value);

const selectedIndex = ref(0);

// 현재 선택된 카드
const currentCard = computed(() => cards.value[selectedIndex.value] || null);

// 현재 카드의 혜택 3개
const currentBenefits = computed(() => {
  return currentCard.value?.benefits?.slice(0, 3) || [];
});

// 카드 개수에 따라 selectedIndex 초기화
watch(() => storeCards.value.length, (newLen) => {
  if (newLen <= 2) {
    selectedIndex.value = 0;
  } else {
    selectedIndex.value = Math.floor(newLen / 2);
  }
}, { immediate: true });
const flipProgress = ref(0); // 0 ~ 1
const flippingIndex = ref(null);
const showPasswordModal = ref(false);
const showQRModal = ref(false);
const showTimeoutModal = ref(false);
const cardUpOffset = ref(0);
const showToast = ref(false);
const toastMessage = ref('');
const isFlipping = ref(false);

// QR 관련 상태
const qrToken = ref('');
const qrSeconds = ref(300);
const qrImage = ref('');
const qrLoading = ref(false);
let qrTimer = null;
let statusTimer = null;

let touchStartX = 0;
let touchStartY = 0;
let isDragging = false;
let swipeDirection = null;

// 부채꼴 배치 (카드 수에 따라 자동 각도 분배)
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

  const offset = index - centerIndex;

  // 카드 개수별 레이아웃 결정
  let angle = 0;
  let spread = 0;
  let yLift = 0;

  if (cardCount === 1) {
    // 1장: 중앙 고정
    angle = 0;
    spread = 0;
    yLift = 80;
  } else if (cardCount === 2) {
    // 2장: 좌우 균형 배치
    angle = offset * 25; // -25도, 25도
    spread = 80;
    yLift = 40;
  } else {
    // 3장 이상: 부채꼴 배치
    const maxOffset = Math.floor(cardCount / 2);
    const angleStep = 90 / maxOffset;
    angle = offset * angleStep;
    spread = 100;
    yLift = index === centerIndex ? 80 : 0;
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

  const upOffset = index === centerIndex ? cardUpOffset.value : 0;
  const scale = index === centerIndex ? 1 : 0.9;
  const opacity = index === centerIndex ? 1 : 0.7;
  const zIndex = 10 - Math.abs(offset);

  return {
    transform: `translate(-50%, -100%) translate(${x}px, -${yCorrection + yLift + upOffset}px) rotate(${angle}deg) scale(${scale})`,
    opacity,
    zIndex,
  };
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

  // 위로 드래그할 때만 카드 이동
  if (deltaY > 0) {
    cardUpOffset.value = Math.min(deltaY, 300); // 최대 300px까지 (모달 높이 기준)
    flipProgress.value = Math.min(deltaY / 300, 1); // 0~1로 정규화, 서서히 뒤집어짐

    // 플립 시작 (100px 이상일 때)
    if (deltaY > CAROUSEL_CONFIG.FLIP_THRESHOLD && !flippingIndex.value) {
      flippingIndex.value = selectedIndex.value;
    }

    // 카드 50% 도는 순간부터 모달 나타나기 시작 (더 일찍, 더 여유롭게)
    if (flipProgress.value >= 0.5) {
      showPasswordModal.value = true;
    }
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

  if (Math.abs(deltaX) > CAROUSEL_CONFIG.HORIZONTAL_SWIPE_THRESHOLD && flipProgress.value === 0) {
    console.log('→ Horizontal swipe');
    cardUpOffset.value = 0;
    if (deltaX > 0) {
      selectedIndex.value = (selectedIndex.value - 1 + cards.value.length) % cards.value.length;
    } else {
      selectedIndex.value = (selectedIndex.value + 1) % cards.value.length;
    }
    return;
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
  selectedIndex.value = index;
};

const handlePasswordSuccess = async () => {
  showPasswordModal.value = false;
  await issueQr();
  showQRModal.value = true;
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
  clearInterval(qrTimer);
  clearInterval(statusTimer);
  qrToken.value = '';
  qrImage.value = '';
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

const handleTimeoutRegenerate = async () => {
  showTimeoutModal.value = false;
  await issueQr();
  showQRModal.value = true;
};

const handleTimeoutCancel = () => {
  showTimeoutModal.value = false;
};

const payment = () => {
  flipProgress.value = 1; // 모달이 보이도록 설정
  showPasswordModal.value = true;
};

const closeModal = () => {
  showPasswordModal.value = false;
  flipProgress.value = 0;
  flippingIndex.value = null;
  cardUpOffset.value = 0;
  isFlipping.value = false;
};

// QR 타이머 시작
const startQRTimer = () => {
  clearInterval(qrTimer);
  qrTimer = setInterval(() => {
    qrSeconds.value--;
    if (qrSeconds.value <= 0) {
      clearInterval(qrTimer);
      clearInterval(statusTimer);
      showTimeoutModal.value = true;
    }
  }, 1000);
};

// QR 상태 확인
const checkQrStatus = async () => {
  if (!qrToken.value) return;
  try {
    const status = await getPaymentQr(qrToken.value);
    if (status.status === 'USED') {
      clearInterval(statusTimer);
      clearInterval(qrTimer);
      if (status.paymentId) await getPayment(status.paymentId);
      showQRModal.value = false;
      toastMessage.value = '결제 성공';
      showToast.value = true;
    } else if (status.status === 'EXPIRED' || status.status === 'FAILED') {
      clearInterval(statusTimer);
      clearInterval(qrTimer);
      showTimeoutModal.value = true;
    }
  } catch (error) {
    console.error('QR 상태 조회 실패:', error);
  }
};

// QR 발급
const issueQr = async () => {
  if (!currentCard.value?.id) {
    alert('카드를 선택해주세요');
    return;
  }

  qrLoading.value = true;
  qrImage.value = '';
  clearInterval(statusTimer);

  try {
    const data = await createPaymentQr(Number(currentCard.value.id), Number(paymentStore.paymentAmount));
    if (!data?.qrToken) throw new Error('QR 토큰을 발급받지 못했습니다.');

    qrToken.value = data.qrToken;
    qrImage.value = await QRCode.toDataURL(data.qrToken, { width: 220, margin: 2 });
    qrSeconds.value = data.expiresAt
      ? Math.max(1, Math.floor((new Date(data.expiresAt).getTime() - Date.now()) / 1000))
      : 300;

    startQRTimer();
    statusTimer = setInterval(checkQrStatus, 2000);
  } catch (error) {
    alert(error.message || 'QR 생성에 실패했습니다.');
    showPasswordModal.value = false;
  } finally {
    qrLoading.value = false;
  }
};
</script>

<template>
  <div class="payment-page">
    <PageHeader title="결제" :show-back="false" />

    <div v-if="hasCards" class="recommend-section" style="display: flex; flex-direction: column; align-items: flex-end; margin-top: -55px; margin-bottom: 0; padding-right: var(--space-md); padding-top: 0;">
      <p style="margin: 0 0 2px 0; font-size: var(--font-sm); color: var(--color-text-secondary);">더 좋은 혜택을 찾기 원하시나요?</p>
      <button class="recommend-btn" @click="$router.push('/payment/recommend')" style="font-size: var(--font-sm); padding: 6px 12px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-md); cursor: pointer; text-decoration: none; font-weight: 500;">카드 추천 받기</button>
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

      <!-- 현재 카드의 혜택 표시 -->
      <div v-if="hasCards && currentBenefits.length > 0" class="benefits-section">
        <h3 class="benefits-title">{{ currentCard?.name }} 주요 혜택</h3>
        <ul class="benefits-list">
          <li v-for="(benefit, idx) in currentBenefits" :key="idx" class="benefits-item">• {{ benefit }}</li>
        </ul>
      </div>

      <!-- 부채꼴 캐러셀 -->
      <div v-if="hasCards" ref="cardsContainerRef" class="cards-carousel"
           @touchstart="handleTouchStart"
           @touchmove="handleTouchMove"
           @touchend="handleTouchEnd">
        <div class="cards-container">
          <div v-for="(card, index) in cards" :key="card.id"
               class="card-item"
               :class="{ flipping: isFlipping && index === flippingIndex }"
               :style="getCardStyle(index)"
               @click="selectCard(index)">
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
        </div>
      </div>

      <!-- 모달들 -->
      <PaymentPasswordModal v-if="showPasswordModal"
                            :style="getModalStyle()"
                            @success="handlePasswordSuccess"
                            @close="handlePasswordClose" />
      <PaymentQRModal v-if="showQRModal"
                      :qr-image="qrImage"
                      :qr-seconds="qrSeconds"
                      :qr-loading="qrLoading"
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
  overflow: hidden;   /* 불필요한 스크롤 제거 */
  display: flex;
  flex-direction: column;
  width: 100%;
   max-width: 480px;   /* 모바일 기준 고정 너비 */
   margin: 0 auto;     /* 중앙 정렬 */
     height: 100vh;      /* 화면 전체 높이 */
}

main {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  width: 100%;
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
  transform-origin: bottom center; /* 회전축을 아래쪽으로 */
  transition: transform 0.4s ease, opacity 0.4s ease; /* 부드러운 애니메이션 */
  perspective: 1000px;
  cursor: pointer;
  top: 50%;
  left: 55%;
}

.card-flip {
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  transform-origin: center center;
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
  /* 작업 관리자에서는 250px가 낫고(모바일용)/vs code에서는 65 */
  margin-top: 65px;  
  margin-bottom: var(--space-md);
  flex-shrink: 0;
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
  background: var(--color-primary);
  color: var(--color-btn-primary-text);
  border: none;
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s ease;
}

.recommend-btn:hover {
  opacity: 0.9;
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
=======
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePaymentStore } from '@/stores/payment';
import { computed } from 'vue';

import PageHeader from '@/components/common/PageHeader.vue';

import PaymentQR from '@/components/payment/PaymentQR.vue';
import PaymentTimer from '@/components/payment/PaymentTimer.vue';
import PaymentSecurity from '@/components/payment/PaymentSecurity.vue';

import PaymentResultModal from '@/components/payment/PaymentResultModal.vue';
import PaymentExpireModal from '@/components/payment/PaymentExpireModal.vue';
import QRCode from 'qrcode';
import { createPaymentQr, getPaymentQr } from '@/api/paymentQrApi';
import { getPayment } from '@/api/walletApi';


const paymentStore = usePaymentStore();

const router = useRouter();

const amount = computed(() => paymentStore.paymentAmount);

const card = computed(() => paymentStore.selectedCard);

const membershipBenefit = computed(() => paymentStore.membershipBenefit);

// 결제 결과 모달 상태
const showResult = ref(false);

const qrSeconds = ref(300);
let qrTimer = null;
let statusTimer = null;
const qrImage = ref('');
const qrToken = ref('');
const qrLoading = ref(false);

const showExpireModal = ref(false);

const success = ref(true);

/*
| QR 타이머 시작
|
| 1초마다 감소
|
| 0초가 되면 QR 만료 처리
*/
const startQRTimer = () => {


  // 기존 타이머 제거
  clearInterval(qrTimer);

  qrTimer = setInterval(() => {

    qrSeconds.value--;

    // QR 만료
    if(qrSeconds.value <= 0){


      clearInterval(qrTimer);
      clearInterval(statusTimer);


      showExpireModal.value = true;

    }

  },1000);


};


const checkQrStatus = async () => {
  if (!qrToken.value) return;
  try {
    const status = await getPaymentQr(qrToken.value);
    if (status.status === 'USED') {
      clearInterval(statusTimer);
      clearInterval(qrTimer);
      if (status.paymentId) await getPayment(status.paymentId);
      success.value = true;
      showResult.value = true;
    } else if (status.status === 'EXPIRED' || status.status === 'FAILED') {
      clearInterval(statusTimer);
      clearInterval(qrTimer);
      showExpireModal.value = true;
    }
  } catch (error) {
    console.error('QR 상태 조회 실패', error);
  }
};

const issueQr = async () => {
  if (!card.value?.id) {
    router.replace('/payment/recommend');
    return;
  }

  qrLoading.value = true;
  qrImage.value = '';
  clearInterval(statusTimer);
  try {
    const data = await createPaymentQr(Number(card.value.id), Number(amount.value)); // 07_25 연동 수정: QR 발급 시 결제금액을 확정한다.
    if (!data?.qrToken) throw new Error('QR 토큰을 발급받지 못했습니다.');
    qrToken.value = data.qrToken;
    qrImage.value = await QRCode.toDataURL(data.qrToken, { width: 220, margin: 2 }); // 07_25 연동 수정: QR 이미지는 프론트에서 직접 생성한다.
    qrSeconds.value = data.expiresAt
      ? Math.max(1, Math.floor((new Date(data.expiresAt).getTime() - Date.now()) / 1000))
      : 300;
    startQRTimer();
    statusTimer = setInterval(checkQrStatus, 2000);
  } catch (error) {
    alert(error.response?.data?.message || error.message || 'QR 생성에 실패했습니다.');
    router.replace('/payment/recommend');
  } finally {
    qrLoading.value = false;
  }
};

/*
| 결제 취소
*/
const cancelPayment = () => {

  showExpireModal.value = false;

  router.back();

};

// QR 재발급
const refreshQR = async () => {

  showExpireModal.value = false;

  await issueQr();

};

/*
| 페이지 진입 시 QR 타이머 시작
*/
onMounted(issueQr);

/*
| 페이지 종료 시 타이머 제거
*/
onUnmounted(()=>{

  clearInterval(qrTimer);
  clearInterval(statusTimer);

});


</script>

<template>

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

<div class="payment-page">


<PageHeader title="결제"/>


<main>


<section class="amount">

<p>
결제 금액
</p>

<h1>
{{amount.toLocaleString()}}원
</h1>

</section>

<!--
  선택한 카드 정보

  card가 없는 상태에서 화면이 먼저 렌더링될 수 있으므로
  v-if로 null 체크

  API 연결 후에도 동일하게 사용 가능
-->
<section
  v-if="card"
  class="card"
>

  <img
    v-if="card.image"
    :src="card.image"
    :alt="card.name"
  />

  <p>
    {{ card.name }}
  </p>

</section>


<PaymentQR :image="qrImage" :loading="qrLoading" />


<PaymentTimer

  :seconds="qrSeconds"

/>

<p class="scan-message">

QR 코드를 스캔해 결제하세요

</p>


<PaymentSecurity />

</main>


<button
class="cancel"

@click="cancelPayment"
>
결제 취소
</button>


<!--
  결제 결과 모달

  close 이벤트:
  모달 닫기 처리
-->
<PaymentResultModal

  v-if="showResult"

  :success="success"

  :amount="amount"

  :card="card"

  :membershipBenefit="membershipBenefit"

  @close="showResult = false"

/>

<!--
  QR 유효시간 종료 모달
-->
<PaymentExpireModal

  v-if="showExpireModal"

  @refresh="refreshQR"

  @cancel="cancelPayment"

/>



</div>


</template> 
<!-- 07_25 연동 변경: 결제 요청·추천·결과 정산 API를 기존 결제 UI에 연결한다. -->
