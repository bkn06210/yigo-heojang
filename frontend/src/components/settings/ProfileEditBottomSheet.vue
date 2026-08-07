<script setup>
import { ref, watch } from 'vue';

import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';
import Icon from '@/components/common/Icon.vue';


const props = defineProps({
  user: {
    type: Object,
    required: true,
  },
});


const emit = defineEmits([
  'close',
  'save',
]);

const fileInput = ref(null);

const closeSheet = () => emit('close');

const openFilePicker = () => fileInput.value?.click();

const changeProfileImage = (event) => {
  const file = event.target.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = () => {
    form.value.profileImageUrl = String(reader.result || '');
  };
  reader.readAsDataURL(file);
};


// 수정용 임시 데이터
const form = ref({
  nickname: '',
  profileImageUrl: '',
});


// 부모 데이터 변경 감지
watch(
  () => props.user,
  (newUser) => {
    if (newUser) {
      form.value = {
        nickname: newUser.nickname ?? '',
        profileImageUrl: newUser.profileImageUrl ?? '',
      };
    }
  },
  {
    immediate: true,
    deep: true,
  }
);


const saveProfile = () => {
  emit('save', {
    nickname: form.value.nickname,
    profileImageUrl: form.value.profileImageUrl,
  });
};


// 바텀시트 닫기
const closeSheet = () => {
  emit('close');
};


// 숨겨진 파일 input 참조
const fileInput = ref(null);


// 카메라 버튼 클릭 시 파일 선택창 열기
const openFilePicker = () => {
  fileInput.value?.click();
};


// 선택한 이미지를 미리보기로 반영
const changeProfileImage = (event) => {
  const file = event.target.files?.[0];

  if (!file) {
    return;
  }

  const reader = new FileReader();

  reader.onload = () => {
    form.value.profileImageUrl = reader.result;
  };

  reader.readAsDataURL(file);
};
</script>

<template>
  <div class="overlay" @click.self="closeSheet">
    <section class="bottom-sheet">
      <div class="handle"></div>

      <h2>프로필 수정</h2>

      <!-- 프로필 이미지 -->
      <div class="profile-image-area">
        <div class="profile-image-wrapper">
          <img
            v-if="form.profileImageUrl"
            :src="form.profileImageUrl"
            class="profile-image"
          />

          <div v-else class="profile-image default-image"></div>

          <button class="camera-button" type="button" @click="openFilePicker">
            <Icon name="camera" size="sm" />
            <span class="material-icons"> photo_camera </span>
          </button>
        </div>
      </div>

      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        hidden
        @change="changeProfileImage"
      />

      <!-- 닉네임 -->
      <AppInput v-model="form.nickname" placeholder="닉네임" />

      <div class="button-area">
        <AppButton text="취소" type="secondary" @click="closeSheet" />

        <AppButton text="저장" type="primary" @click="saveProfile" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;

  inset: 0;

  background: rgba(0, 0, 0, 0.45);

  display: flex;

  align-items: flex-end;

  z-index: 1000;
}

.bottom-sheet {
  width: 100%;

  border-radius: var(--radius-xl) var(--radius-xl) 0 0;

  padding: var(--space-xl);

  background: var(--color-surface);
  border-top: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

[data-theme="dark"] .bottom-sheet {
  background: var(--color-surface);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.35);
  background: white;

  border-radius: 24px 24px 0 0;

  padding: 24px;
}

.handle {
  width: 40px;

  height: 4px;

  background: var(--color-border);

  border-radius: 10px;

  margin: 0 auto var(--space-lg);
  background: #ddd;

  border-radius: 10px;

  margin: 0 auto 20px;
}

h2 {
  text-align: center;
  color: var(--color-text-primary);
  font-size: var(--font-xl);
  font-weight: var(--font-bold);
  letter-spacing: -0.2px;
  margin: 0;
}

.profile-image-area {
  display: flex;

  justify-content: center;

  margin: 24px 0;
}

.profile-image-wrapper {
  position: relative;

  width: 100px;

  height: 100px;
}

.profile-image {
  width: 100%;

  height: 100%;

  border-radius: 50%;

  object-fit: cover;
}

.default-image {
  background: var(--color-border);
  background: #ddd;

  width: 100%;

  height: 100%;

  border-radius: 50%;
}

.camera-button {
  position: absolute;

  right: -5px;

  bottom: -5px;

  width: 36px;

  height: 36px;

  border-radius: 50%;

  display: flex;
  align-items: center;
  justify-content: center;

  background: var(--color-primary);

  border: none;

  color: white;

  cursor: pointer;
  transition: var(--transition-fast);
  padding: 0;
}

.camera-button:hover {
  opacity: 0.8;
}

[data-theme="dark"] .camera-button {
  background: white;
  color: #333;
  width: 34px;

  height: 34px;

  border-radius: 50%;

  background: white;

  border: 1px solid #ddd;
}

.button-area {
  display: flex;

  gap: 12px;

  margin-top: 24px;
}

.button-area :deep(button) {
  flex: 1;
}
</style>
<!-- 07_25 연동 변경: 회원 프로필 수정 내용을 실제 회원 API로 저장한다. -->
