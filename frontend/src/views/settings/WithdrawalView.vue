<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import PageHeader from '@/components/common/PageHeader.vue';
import AppButton from '@/components/common/AppButton.vue';
import ConfirmModal from '@/components/common/ConfirmModal.vue';
import PasswordVerifyModal from '@/components/auth/PasswordVerifyModal.vue';
import { useToast } from '@/composables/useToast';
import { useAuthStore } from '@/stores/authStore';
import { getWithdrawalTerms, verifyMemberPassword, withdrawMember } from '@/api/memberApi';

const router = useRouter();
const authStore = useAuthStore();
const { showToast } = useToast();

// 백엔드 WithdrawalReasonType enum과 값이 정확히 같아야 한다.
// 라벨만 바꾸는 건 자유지만 value는 서버 enum 이름 그대로여야 한다.
const REASON_OPTIONS = [
  { value: 'LOW_USAGE', label: '서비스를 자주 사용하지 않아요' },
  { value: 'INCONVENIENT_UX', label: '사용이 불편해요' },
  { value: 'PRIVACY_CONCERN', label: '개인정보 제공이 부담스러워요' },
  { value: 'FOUND_ALTERNATIVE', label: '다른 서비스를 사용하게 됐어요' },
  { value: 'OTHER', label: '기타' }
];

// 탈퇴 고지 약관 — 서버에서 받아온다.
const withdrawalTerm = ref(null);
const termsLoading = ref(true);
const termsError = ref('');

// 사용자 입력
const selectedReason = ref('');
const reasonDetail = ref('');

// 모달 상태
const isConfirmModalOpen = ref(false);
const isPasswordModalOpen = ref(false);
const isSubmitting = ref(false);
const passwordError = ref('');
const verifiedPassword = ref('');

// 안내문을 조회했고 탈퇴 사유를 선택하면 진행할 수 있다.
const canSubmit = computed(() =>
  Boolean(withdrawalTerm.value) &&
  Boolean(selectedReason.value)
);

// 서버 응답에서 사람이 읽을 메시지를 뽑는다.
// axios는 4xx/5xx에서 reject하므로 error.response.data에 ApiResponse가 들어 있다.
const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback;

const resolveErrorCode = (error) => error?.response?.data?.code || error?.code;

const loadWithdrawalTerms = async () => {
  termsLoading.value = true;
  termsError.value = '';

  try {
    const response = await getWithdrawalTerms();

    // 탈퇴 스코프에는 현재 약관이 하나뿐이지만, 서버는 목록으로 내려준다.
    // 필수 약관만 골라 쓰고, 없으면 탈퇴를 진행시키지 않는다.
    const terms = response?.terms ?? [];
    withdrawalTerm.value = terms.find((term) => term.required) ?? terms[0] ?? null;

    if (!withdrawalTerm.value) {
      termsError.value = '탈퇴 안내 약관을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.';
    }
  } catch (error) {
    termsError.value = resolveErrorMessage(
      error,
      '탈퇴 안내 약관을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'
    );
  } finally {
    termsLoading.value = false;
  }
};

onMounted(loadWithdrawalTerms);

const goBack = () => {
  router.go(-1);
};

const openConfirmModal = () => {
  if (!canSubmit.value) {
    return;
  }

  passwordError.value = '';
  verifiedPassword.value = '';
  isPasswordModalOpen.value = true;
};

// 비밀번호가 맞는지 먼저 확인한 뒤 최종 탈퇴 확인 모달을 연다.
const verifyPasswordBeforeWithdrawal = async (password) => {
  if (isSubmitting.value) {
    return;
  }

  isSubmitting.value = true;
  passwordError.value = '';

  try {
    await verifyMemberPassword(password);
    verifiedPassword.value = password;
    isPasswordModalOpen.value = false;
    isConfirmModalOpen.value = true;
  } catch (error) {
    passwordError.value = resolveErrorMessage(error, '비밀번호가 일치하지 않습니다.');
  } finally {
    isSubmitting.value = false;
  }
};

const closeFinalConfirmModal = () => {
  isConfirmModalOpen.value = false;
  verifiedPassword.value = '';
};

const confirmWithdrawal = () => {
  isConfirmModalOpen.value = false;
  handleWithdrawal(verifiedPassword.value);
};

const handleWithdrawal = async (password) => {
  if (isSubmitting.value) {
    return;
  }

  isSubmitting.value = true;
  passwordError.value = '';

  try {
    await withdrawMember({
      password,
      reasonType: selectedReason.value,
      // 기타가 아니면 상세 사유는 보내지 않는다. 빈 문자열 대신 null이어야
      // DB의 reason_detail이 NULL로 남는다.
      reasonDetail:
        selectedReason.value === 'OTHER' && reasonDetail.value.trim()
          ? reasonDetail.value.trim()
          : null,
      termVersionId: withdrawalTerm.value.termsVersionId
    });

    isPasswordModalOpen.value = false;

    // 서버가 이미 refresh token을 전부 폐기했다. 남은 로컬 토큰도 지워
    // 다음 요청이 죽은 토큰으로 나가지 않게 한다.
    authStore.logout();
    verifiedPassword.value = '';

    showToast('success', '회원 탈퇴가 정상적으로 처리되었습니다.');
    router.replace('/auth/login');
  } catch (error) {
    const code = resolveErrorCode(error);

    // 비밀번호만 틀린 경우는 모달을 닫지 않고 그 자리에서 다시 입력받는다.
    if (code === 'WITHDRAWAL_PASSWORD_MISMATCH') {
      passwordError.value = resolveErrorMessage(error, '비밀번호가 일치하지 않습니다.');
      return;
    }

    // 조회 시점과 탈퇴 시점 사이에 약관이 개정되면 서버가 INPUT_INVALID를 준다.
    // 이때는 최신 약관을 다시 받아와 동의부터 다시 받아야 한다.
    if (code === 'INPUT_INVALID') {
      isPasswordModalOpen.value = false;
      await loadWithdrawalTerms();
      showToast('error', '탈퇴 안내 내용이 변경되었습니다. 최신 내용을 다시 확인해주세요.');
      return;
    }

    isPasswordModalOpen.value = false;
    showToast('error', resolveErrorMessage(error, '회원 탈퇴에 실패했습니다.'));
  } finally {
    isSubmitting.value = false;
  }
};
</script>


<template>
  <div class="withdrawal-view">
    <!-- 상단 헤더: 타이틀과 뒤로가기 버튼 -->
    <PageHeader title="회원 탈퇴" @back="goBack" />

    <div class="content-container">
      <!-- 안내 타이틀 및 문구 -->
      <div class="notice-section">
        <p class="notice-subtitle">탈퇴하기 전에 아래 내용을 꼭 확인해주세요.</p>
      </div>

      <!-- 탈퇴 고지 약관 (서버에서 받아온 전문 + 필수 동의) -->
      <div class="card-box">
        <div v-if="termsLoading" class="state-text">
          탈퇴 안내를 불러오는 중입니다...
        </div>

        <div v-else-if="termsError" class="state-block">
          <p class="state-text error">{{ termsError }}</p>
          <button type="button" class="retry-button" @click="loadWithdrawalTerms">
            다시 시도
          </button>
        </div>

        <template v-else-if="withdrawalTerm">
          <div class="section-label">
            {{ withdrawalTerm.termsName.replace(' 및 동의', '') }}
          </div>

          <p class="term-content">{{ withdrawalTerm.content }}</p>
        </template>
      </div>

      <!-- 탈퇴 사유 선택 (서버에서 필수값) -->
      <div class="card-box reason-section">
        <div class="section-label">탈퇴 사유</div>

        <div class="reason-list">
          <label
            v-for="option in REASON_OPTIONS"
            :key="option.value"
            class="reason-option"
          >
            <input
              v-model="selectedReason"
              type="radio"
              name="withdrawal-reason"
              :value="option.value"
            />
            <span>{{ option.label }}</span>
          </label>

          <!-- 기타 선택 시 입력 박스 -->
          <div v-if="selectedReason === 'OTHER'" class="etc-input-section">
            <input
              v-model="reasonDetail"
              type="text"
              placeholder="탈퇴 사유를 입력해주세요"
              class="etc-input"
              maxlength="500"
            />
            <span class="input-counter">{{ reasonDetail.length }}/500</span>
          </div>
        </div>
      </div>

      <!-- 하단 탈퇴하기 버튼 -->
      <div class="footer-button-area">
        <AppButton :disabled="!canSubmit" @click="openConfirmModal">탈퇴하기</AppButton>
      </div>
    </div>

    <!-- 2단계: 비밀번호 확인 후 마지막으로 탈퇴 여부를 확인한다. -->
    <ConfirmModal
      v-if="isConfirmModalOpen"
      title="정말 탈퇴하시겠습니까?"
      message="탈퇴하면 보유 카드와 소비·결제 내역, 포인트 정보가 모두 삭제되며 다시 복구할 수 없습니다."
      confirm-text="탈퇴"
      cancel-text="취소"
      reverse-actions
      cancel-primary
      cancel-danger
      @confirm="confirmWithdrawal"
      @close="closeFinalConfirmModal"
    />

    <!-- 1단계: 탈퇴 전 로그인 비밀번호를 먼저 확인한다. -->
    <PasswordVerifyModal
      v-if="isPasswordModalOpen"
      title="비밀번호 확인"
      description="회원 탈퇴를 진행하려면 현재 비밀번호를 입력해주세요."
      confirm-text="확인"
      :loading="isSubmitting"
      :error-message="passwordError"
      @confirm="verifyPasswordBeforeWithdrawal"
      @close="isPasswordModalOpen = false; verifiedPassword = ''"
    />
  </div>
</template>

<style scoped>
.withdrawal-view {
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--color-bg);
  box-sizing: border-box;
}

.content-container {
  flex: 1;
  padding: var(--space-lg);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
}

.notice-section {
  margin-bottom: var(--space-xl);
  padding-left: var(--space-xxs);
}

/* 안내 타이틀 (Display Typography) */
.notice-title {
  font-size: var(--typo-display-medium-size);
  font-weight: var(--typo-display-medium-weight);
  line-height: var(--typo-display-medium-line-height);
  letter-spacing: var(--typo-display-medium-letter-spacing);
  color: var(--color-text-primary);
  margin-bottom: var(--space-xs);
}

.notice-subtitle {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

/* 카드 영역 (Soft Glassmorphism, 중립 톤) */
.card-box {
  border-radius: var(--radius-lg);
  padding: var(--space-md);
  margin-bottom: var(--space-sm);

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.5) 0%, rgba(255, 255, 255, 0.2) 100%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05), inset 0 1px 0 rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

[data-theme="dark"] .card-box {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.section-label {
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
}

/* 약관 전문은 줄바꿈이 들어간 원문 그대로 보여준다 */
.term-content {
  margin: 0;
  white-space: pre-line;
  font-size: var(--font-sm);
  line-height: 1.7;
  color: var(--color-text-secondary);
}

/* 약관 로딩·실패 상태 */
.state-block {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-sm);
}

.state-text {
  margin: 0;
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

.state-text.error {
  color: var(--color-input-error);
}

.retry-button {
  padding: var(--space-xs) var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--font-xs);
  font-family: inherit;
  cursor: pointer;
}

.reason-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

/* 사유는 하나만 고를 수 있다 — 서버가 단일 enum 값을 받기 때문 */
.reason-option {
  display: flex;
  gap: var(--space-xs);
  align-items: center;
  color: var(--color-text-primary);
  font-size: var(--font-sm);
  cursor: pointer;
}

.reason-option input {
  width: 18px;
  height: 18px;
  accent-color: var(--color-primary);
  cursor: pointer;
}

.etc-input-section {
  margin-top: var(--space-xs);
  padding-top: 0;
  border-top: none;
}

.etc-input {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-size: var(--font-sm);
  font-family: inherit;
  transition: all var(--transition-fast);
  box-sizing: border-box;
}

.etc-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(var(--color-primary-dark-rgb), 0.1);
}

[data-theme="dark"] .etc-input:focus {
  box-shadow: 0 0 0 3px rgba(var(--color-primary-dark-rgb), 0.15);
}

.etc-input::placeholder {
  color: var(--color-text-tertiary);
}

.input-counter {
  display: block;
  margin-top: var(--space-xs);
  font-size: var(--font-xs);
  color: var(--color-text-tertiary);
  text-align: right;
}

.footer-button-area {
  width: 160px;
  margin: var(--space-xl) auto 0;
}

.footer-button-area :deep(button) {
  min-height: 42px;
  padding: var(--space-xs) var(--space-lg);
  border: none;
  background: var(--color-coral);
  color: #fff;
}
</style>
