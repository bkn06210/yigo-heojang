<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useAuthStore } from '@/stores/authStore';
import PageHeader from '@/components/common/PageHeader.vue';

const router = useRouter();
const authStore = useAuthStore();
const { user } = storeToRefs(authStore);

const profileImage = ref(user.value?.profileImage || null);
const fileInput = ref(null);

const handleBack = () => {
  router.back();
};

const handleImageUpload = (e) => {
  const file = e.target.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = (event) => {
    profileImage.value = event.target.result;
    authStore.updateUser({
      profileImage: event.target.result
    });
  };
  reader.readAsDataURL(file);
};

const triggerFileInput = () => {
  fileInput.value?.click();
};
</script>

<template>
  <div class="profile-view">
    <PageHeader title="프로필" @back="handleBack" />

    <main class="profile-content">
      <!-- 프로필 사진 -->
      <section class="profile-image-section">
        <div class="profile-image-container" @click="triggerFileInput">
          <img v-if="profileImage" :src="profileImage" alt="프로필" class="profile-image" />
          <div v-else class="profile-placeholder">
            {{ user?.nickname?.charAt(0) || user?.name?.charAt(0) || '👤' }}
          </div>
          <div class="edit-badge">
            <span>📷</span>
          </div>
        </div>
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          class="file-input"
          @change="handleImageUpload"
        />
        <p class="upload-hint">클릭하여 프로필 사진 변경</p>
      </section>

      <!-- 사용자 정보 -->
      <section class="user-info-section">
        <div class="info-item">
          <label>이름</label>
          <p>{{ user?.name || '-' }}</p>
        </div>
        <div class="info-item">
          <label>닉네임</label>
          <p>{{ user?.nickname || '-' }}</p>
        </div>
        <div class="info-item">
          <label>이메일</label>
          <p>{{ user?.email || '-' }}</p>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.profile-view {
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--color-bg);
  box-sizing: border-box;
  padding: var(--space-md);
  padding-bottom: calc(var(--space-xl) + var(--space-2xl) + var(--space-xl));
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

/* 프로필 사진 섹션 */
.profile-image-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
}

.profile-image-container {
  position: relative;
  width: 120px;
  height: 120px;
  cursor: pointer;
}

.profile-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  transition: var(--transition-normal);
}

.profile-image-container:hover .profile-image {
  opacity: 0.8;
}

.profile-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: var(--font-bold);
  color: white;
}

.profile-image-container:hover .profile-placeholder {
  opacity: 0.8;
}

.edit-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 36px;
  height: 36px;
  background: var(--color-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.file-input {
  display: none;
}

.upload-hint {
  margin: 0;
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

/* 사용자 정보 섹션 */
.user-info-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.info-item {
  padding: var(--space-md);
  background: var(--color-surface);
  border-radius: var(--radius-md);
}

.info-item label {
  display: block;
  font-size: var(--font-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-xs);
}

.info-item p {
  margin: 0;
  font-size: var(--font-md);
  color: var(--color-text-primary);
}
</style>
