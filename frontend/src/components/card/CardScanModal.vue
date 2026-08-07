<script setup>
import { ref } from 'vue';
<<<<<<< HEAD
import Icon from '@/components/common/Icon.vue';
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

const emit = defineEmits(['close', 'complete']);

// 촬영 진행 상태
const isScanning = ref(false);

// 인식 결과 표시 여부
const showResult = ref(false);

// 임시 OCR 결과
// 실제 서비스에서는 OCR API 응답 데이터
const scanResult = ref({
  cardName: 'KB My WE:SH 카드',
<<<<<<< HEAD
  cardNumber: '1234567890121123',
=======
  // PR #29 연동: 촬영 목 결과도 서버 BIN/Luhn 검증을 통과하는 시연용 KB 카드번호를 사용한다.
  cardNumber: '2228790000000008',
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  expiryDate: '12/28',
});

// 카드번호 표시용 포맷
const formatCardNumber = (number) => {
  return number.replace(/[^0-9]/g, '').replace(/(\d{4})(?=\d)/g, '$1-');
};

// 닫기
const close = () => {
  emit('close');
};

// 촬영 버튼
const scanCard = () => {
  // 분석 시작
  isScanning.value = true;

  /*
    실제 서비스에서는 여기에서:

    사진 촬영
      ↓
    OCR API 요청
      ↓
    카드 정보 반환

    과정이 들어감
  */

  // 임시 OCR 처리
  setTimeout(() => {
    // 분석 종료
    isScanning.value = false;

    // 결과 화면 표시
    showResult.value = true;
  }, 1500);
};

// 등록 정보 전달
const complete = () => {
  emit('complete', {
    cardName: scanResult.value.cardName,

    cardNumber: scanResult.value.cardNumber,

    expiryDate: scanResult.value.expiryDate,
  });
};
</script>

<template>
  <div class="overlay">
    <section class="modal">
      <div class="header">
        <h2>카드 촬영</h2>

<<<<<<< HEAD
        <button @click="close"><Icon name="close" size="sm" /></button>
=======
        <button @click="close">✕</button>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
      </div>

      <!-- 촬영 화면 -->
      <div v-if="!showResult" class="camera-area">
        <div class="card-guide">
          <div v-if="isScanning" class="scan-line"></div>

          <span> 카드 영역 </span>
        </div>

        <button class="scan-button" @click="scanCard" :disabled="isScanning">
<<<<<<< HEAD
          <template v-if="isScanning">카드 정보를 분석 중입니다</template>
          <template v-else><Icon name="camera" size="sm" /> 촬영</template>
=======
          {{ isScanning ? '카드 정보를 분석 중입니다' : '📷 촬영' }}
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
        </button>
      </div>

      <!-- 인식 완료 화면 -->
      <div v-else class="result">
        <h3>카드 정보 인식 완료</h3>

        <div class="info">
          <p>
            카드명
            <strong>
              {{ scanResult.cardName }}
            </strong>
          </p>

          <p>
            카드번호
            <strong>
              {{ formatCardNumber(scanResult.cardNumber) }}
            </strong>
          </p>

          <p>
            만료일
            <strong>
              {{ scanResult.expiryDate }}
            </strong>
          </p>
        </div>

        <button class="complete-button" @click="complete">
          등록 정보 사용
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;

  background: rgba(0, 0, 0, 0.4);

  display: flex;
  align-items: center;
  justify-content: center;

  z-index: 2000;
}

.modal {
  width: 90%;

<<<<<<< HEAD
  background: var(--color-surface);
=======
  background: white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  border-radius: 24px;

  padding: 20px;
}

.header {
  display: flex;

  justify-content: space-between;

  align-items: center;
}

.header button {
  border: none;

  background: none;

  font-size: 20px;
<<<<<<< HEAD

  color: var(--color-text-primary);
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.camera-area {
  margin-top: 30px;

  text-align: center;
}

.card-guide {
  position: relative;
  height: 180px;

<<<<<<< HEAD
  border: 2px dashed var(--color-border);
=======
  border: 2px dashed #4f46e5;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  border-radius: 20px;

  display: flex;
  align-items: center;
  justify-content: center;

  overflow: hidden;

<<<<<<< HEAD
  color: var(--color-text-secondary);
=======
  color: #777;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

/* 카드 스캔 라인 */
.scan-line {
  position: absolute;

  top: 0;

  left: 0;

  width: 100%;

  height: 3px;

<<<<<<< HEAD
  background: var(--color-border);
=======
  background: #4f46e5;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  animation: scan 1.2s infinite;
}

/* 위아래 움직임 */
@keyframes scan {
  0% {
    top: 0;
  }

  50% {
    top: 100%;
  }

  100% {
    top: 0;
  }
}

.scan-button,
.complete-button {
  width: 100%;

  height: 48px;

  margin-top: 24px;

  border: none;

  border-radius: 12px;

<<<<<<< HEAD
  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );

  color: var(--color-btn-primary-text);
=======
  background: #4f46e5;

  color: white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}

.info {
  margin-top: 20px;
}

.info p {
  display: flex;

  justify-content: space-between;

  padding: 12px 0;

<<<<<<< HEAD
  border-bottom: 1px solid var(--color-border);
}
</style>
=======
  border-bottom: 1px solid #eee;
}
</style>
<!-- 07_25 연동 변경: 카드 스캔 결과를 카드등록 API 입력으로 전달하도록 보완했다. -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
