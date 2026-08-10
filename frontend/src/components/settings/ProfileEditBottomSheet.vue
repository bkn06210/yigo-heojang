<script setup>
import { ref, watch } from 'vue';

import AppButton from '@/components/common/AppButton.vue';
import AppInput from '@/components/common/AppInput.vue';


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

  background: linear-gradient(180deg, rgba(255, 255, 255, 0.6) 0%, var(--color-surface) 30%);
  border-top: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

[data-theme="dark"] .bottom-sheet {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.06) 0%, var(--color-surface) 30%);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 -16px 40px rgba(0, 0, 0, 0.35);
}

.handle {
  width: 40px;

  height: 4px;

  background: var(--color-border);

  border-radius: 10px;

  margin: 0 auto var(--space-lg);
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

  width: 100%;

  height: 100%;

  border-radius: 50%;
}

.camera-button {
  position: absolute;

  right: -5px;

  bottom: -5px;

  width: 34px;

  height: 34px;

  border-radius: 50%;

  display: flex;
  align-items: center;
  justify-content: center;

  background: linear-gradient(135deg, var(--color-btn-primary-start), var(--color-btn-primary-end));

  border: 2px solid var(--color-surface);

  color: var(--color-btn-primary-text);

  cursor: pointer;
}

.camera-button .material-icons {
  font-size: 16px;
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
