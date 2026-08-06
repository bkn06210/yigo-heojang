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

const staticCards = [
  { id: 1, name: 'KB My WE:SH', image: 'https://via.placeholder.com/280x177?text=KB' },
  { id: 2, name: '신한 SOL Pay', image: 'https://via.placeholder.com/280x177?text=Shinhan' },
  { id: 3, name: '삼성 카드', image: 'https://via.placeholder.com/280x177?text=Samsung' },
  { id: 4, name: '현대 카드', image: 'https://via.placeholder.com/280x177?text=Hyundai' },
  { id: 5, name: 'NH농협 카드', image: 'https://via.placeholder.com/280x177?text=NH' },
];

const cards = ref(staticCards);

watch(() => storeCards.value.length, (newLen) => {
  cards.value = staticCards.slice(0, newLen);
});

const selectedIndex = ref(2);
const flipProgress = ref(0); // 0 ~ 1
const flippingIndex = ref(null);
const showPasswordModal = ref(false);
const showQRModal = ref(false);
const showTimeoutModal = ref(false);
const cardUpOffset = ref(0);
const showToast = ref(false);
const toastMessage = ref('');
const isFlipping = ref(false);


let touchStartX = 0;
let touchStartY = 0;
let isDragging = false;
let swipeDirection = null;

// 부채꼴 배치 (카드 수에 따라 자동 각도 분배)
const getCardStyle = (index) => {
  const centerIndex = selectedIndex.value;

  // 플립 중인 카드: 올라가면서 점점 뒤집어짐
  if (index === flippingIndex.value && flipProgress.value > 0) {
    const flipAngle = flipProgress.value * 180;
    return {
      transform: `translate(-50%, -100%) translateY(-${80 + cardUpOffset.value}px) rotateY(${flipAngle}deg)`,
      opacity: 1,
      zIndex: 100,
    };
  }

  // 플립 중일 때 다른 카드들은 고정 (cardUpOffset 무시)
  if (flipProgress.value > 0 && index !== flippingIndex.value) {
    const offset = index - centerIndex;
    const maxOffset = Math.floor(cards.value.length / 2);
    const angleStep = 90 / maxOffset;
    const angle = offset * angleStep;
    const spread = 100;
    const x = offset * spread;
    const yCorrection = Math.abs(angle) * 0.3;
    const yLift = index === centerIndex ? 80 : 0;

    return {
      transform: `translate(-50%, -100%) translate(${x}px, -${yCorrection + yLift}px) rotate(${angle}deg) scale(${index === centerIndex ? 1 : 0.9})`,
      opacity: index === centerIndex ? 1 : 0.7,
      zIndex: 10 - Math.abs(offset),
    };
  }
  const offset = index - centerIndex;

  const maxOffset = Math.floor(cards.value.length / 2);
  const angleStep = 90 / maxOffset; // 끝은 ±90도, 가운데는 0도
  const angle = offset * angleStep;

  const spread = 100; // 좌우 간격(px)
  const x = offset * spread;

   // 각도에 따라 Y축 보정 (내려간 만큼 위로 올리기)
  const yCorrection = Math.abs(angle) * 0.3; // 각도 90도 → 약 27px 보정
  const yLift = index === centerIndex ? 80 : 0; // 중앙 카드만 40px 위로
  const upOffset = index === centerIndex ? cardUpOffset.value : 0; // 드래그할 때 위로 이동

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

const handlePasswordSuccess = () => {
  showPasswordModal.value = false;
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
</script>

<template>
  <div class="payment-page">
    <PageHeader title="결제" :show-back="false" />

    <div v-if="hasCards" class="recommend-section">
      <button class="recommend-btn" @click="$router.push('/payment/recommend')">카드 추천 받기</button>
    </div>

    <main style="flex: 1">
      <!-- 카드 없을 때 -->
      <div v-if="!hasCards" style="flex: 1; display: flex; align-items: center; justify-content: center;">
        <div style="text-align: center; display: flex; flex-direction: column; gap: 16px; width: 100%; padding: 0 var(--space-md); box-sizing: border-box;">
          <h2 style="margin: 0; font-size: 18px; font-weight: 700; color: var(--color-text-primary);">등록된 카드가 없어요</h2>
          <p style="margin: 0; font-size: 14px; color: var(--color-text-secondary);">카드를 등록하면 혜택과 소비 관리를 시작할 수 있습니다.</p>
          <button @click="$router.push('/cards/register')" style="width: 100%; padding: 12px 20px; background: var(--color-primary); color: var(--color-btn-primary-text); border: none; border-radius: var(--radius-full); font-weight: 600; cursor: pointer; box-sizing: border-box;">카드 등록</button>
        </div>
      </div>

      <!-- 부채꼴 캐러셀 -->
      <div v-else ref="cardsContainerRef" class="cards-carousel"
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
  padding: var(--space-md) var(--space-md) 0;
  text-align: center;
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

</style>
