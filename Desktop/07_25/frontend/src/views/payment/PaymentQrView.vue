<script setup>
import { ref, computed } from 'vue'
import QRCode from 'qrcode'

import { createPaymentQr } from '@/api/paymentQrApi'

import PageHeader from '@/components/common/PageHeader.vue'
import BottomNav from '@/components/common/BottomNav.vue'

const loading = ref(false)
const error = ref('')

const qrToken = ref('')
const qrImageBase64 = ref('')
const expiresAt = ref('')

const qrImageSrc = computed(() => {
  return qrImageBase64.value || ''
})

const createQr = async () => {
  loading.value = true
  error.value = ''

  qrToken.value = ''
  qrImageBase64.value = ''
  expiresAt.value = ''

  try {
    // 테스트용 카드 ID
    const userCardId = 1

    const response = await createPaymentQr(userCardId)
    const data = response.data?.data || response.data || {}

    console.log('QR 생성 응답:', data)

    qrToken.value = data.qrToken || data.token || ''
    expiresAt.value = data.expiresAt || ''

    const backendQrImage =
        data.qrImageBase64 ||
        data.qrImage ||
        data.imageBase64 ||
        ''

    if (backendQrImage) {
      qrImageBase64.value = backendQrImage.startsWith('data:image')
          ? backendQrImage
          : `data:image/png;base64,${backendQrImage}`
      return
    }

    if (!qrToken.value) {
      error.value = 'QR 토큰이 없습니다.'
      return
    }

    // 백엔드가 이미지 안 주면 프론트에서 QR 이미지 생성
    qrImageBase64.value = await QRCode.toDataURL(qrToken.value)
  } catch (err) {
    console.error('QR 생성 실패', err)

    const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'QR 생성에 실패했습니다.'

    error.value = message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="qr-page">
    <PageHeader title="QR 결제" />

    <main class="content">
      <section class="qr-section">
        <h1>결제 QR 생성</h1>

        <p class="description">
          결제에 사용할 QR 코드를 생성합니다.
        </p>

        <button
            class="create-button"
            :disabled="loading"
            @click="createQr"
        >
          {{ loading ? 'QR 생성 중...' : 'QR 생성하기' }}
        </button>

        <p
            v-if="error"
            class="error-text"
        >
          {{ error }}
        </p>

        <div
            v-if="qrImageSrc"
            class="qr-box"
        >
          <img
              :src="qrImageSrc"
              alt="결제 QR 코드"
              class="qr-image"
          />

          <p class="token-text">
            QR Token
          </p>

          <p class="token-value">
            {{ qrToken }}
          </p>

          <p
              v-if="expiresAt"
              class="expire-text"
          >
            만료 시간: {{ expiresAt }}
          </p>
        </div>
      </section>
    </main>

    <BottomNav />
  </div>
</template>

<style scoped>
.qr-page {
  min-height: 100vh;
  padding: 20px;
  padding-bottom: 80px;
}

.content {
  margin-top: 24px;
}

.qr-section {
  background: white;
  border-radius: 18px;
  padding: 22px;
}

h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.description {
  font-size: 14px;
  color: #666;
  margin-bottom: 24px;
}

.create-button {
  width: 100%;
  height: 52px;
  border: none;
  border-radius: 14px;
  background: #2454e6;
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}

.create-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-text {
  margin-top: 16px;
  color: #dc2626;
  font-size: 14px;
}

.qr-box {
  margin-top: 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-image {
  width: 240px;
  height: 240px;
  object-fit: contain;
}

.token-text {
  margin-top: 18px;
  font-size: 13px;
  color: #777;
}

.token-value {
  max-width: 100%;
  word-break: break-all;
  text-align: center;
  font-size: 13px;
  color: #333;
}

.expire-text {
  margin-top: 10px;
  font-size: 13px;
  color: #666;
}
</style>