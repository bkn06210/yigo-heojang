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

  background: white;

  border-radius: 24px 24px 0 0;

  padding: 24px;
}

.handle {
  width: 40px;

  height: 4px;

  background: #ddd;

  border-radius: 10px;

  margin: 0 auto 20px;
}

h2 {
  text-align: center;
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
  background: #ddd;

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
