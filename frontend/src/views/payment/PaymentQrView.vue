<script setup>
import { ref, computed } from 'vue'
import QRCode from 'qrcode'
import { createPaymentQr } from '@/api/paymentQrApi'
import PageHeader from '@/components/common/PageHeader.vue'
import BottomNavigation from '@/components/layout/BottomNavigation.vue'

const userCardId = ref(1)
const loading = ref(false)
const errorMessage = ref('')
const qrToken = ref('')
const qrImage = ref('')
const expiresAt = ref('')
const hasQr = computed(() => Boolean(qrToken.value && qrImage.value))

const createQr = async () => {
  loading.value = true
  errorMessage.value = ''
  qrToken.value = ''
  qrImage.value = ''
  try {
    const data = await createPaymentQr(Number(userCardId.value))
    if (!data?.qrToken) {
      throw new Error('QR 토큰을 발급받지 못했습니다.')
    }
    qrToken.value = data.qrToken || ''
    expiresAt.value = data.expiresAt || ''
    const backendImage = data.qrImageBase64 || ''
    qrImage.value = backendImage
      ? (backendImage.startsWith('data:image') ? backendImage : `data:image/png;base64,${backendImage}`)
      : await QRCode.toDataURL(qrToken.value, { width: 240, margin: 2 })
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || 'QR 생성에 실패했습니다.'
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
        <p class="description">결제에 사용할 보유 카드 ID를 입력해주세요.</p>
        <input v-model="userCardId" type="number" min="1" class="card-input" aria-label="보유 카드 ID">
        <button class="create-button" :disabled="loading" @click="createQr">
          {{ loading ? 'QR 생성 중...' : 'QR 생성하기' }}
        </button>
        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        <div v-if="hasQr" class="qr-box">
          <img :src="qrImage" alt="결제 QR 코드" class="qr-image">
          <p class="token-text">QR Token</p>
          <p class="token-value">{{ qrToken }}</p>
          <p v-if="expiresAt" class="expire-text">만료 시간: {{ expiresAt }}</p>
        </div>
      </section>
    </main>
    <BottomNavigation />
  </div>
</template>

<style scoped>
.qr-page{min-height:100vh;padding:20px;padding-bottom:80px}.content{margin-top:24px}.qr-section{background:white;border-radius:18px;padding:22px}h1{font-size:24px;font-weight:700;margin-bottom:8px}.description{font-size:14px;color:#666;margin-bottom:16px}.card-input{width:100%;height:46px;border:1px solid #ddd;border-radius:12px;padding:0 14px;margin-bottom:12px}.create-button{width:100%;height:52px;border:none;border-radius:14px;background:#2454e6;color:white;font-size:16px;font-weight:600;cursor:pointer}.create-button:disabled{opacity:.6;cursor:not-allowed}.error-text{margin-top:16px;color:#dc2626;font-size:14px}.qr-box{margin-top:28px;display:flex;flex-direction:column;align-items:center}.qr-image{width:240px;height:240px;object-fit:contain}.token-text{margin-top:18px;font-size:13px;color:#777}.token-value{max-width:100%;word-break:break-all;text-align:center;font-size:13px;color:#333}.expire-text{margin-top:10px;font-size:13px;color:#666}
</style>
<!-- 07_25 연동 추가: QR 생성부터 결제 상태 확인까지 백엔드 API로 시연하는 화면이다. -->
