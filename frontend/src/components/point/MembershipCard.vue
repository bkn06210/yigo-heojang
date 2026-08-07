<script setup>
import { useRouter } from 'vue-router'
import BaseCard from '@/components/common/BaseCard.vue'
import AppButton from '@/components/common/AppButton.vue'

const router = useRouter()

const props = defineProps({
  membership: {
    type: Object,
    required: true,
  },
})

const goDetail = (id) => {
  router.push(`/memberships/${id}`)
}
</script>

<template>
  <BaseCard class="membership-card">
    <div class="membership-item">
      <span class="membership-name">
        {{ props.membership.name }}
      </span>

      <AppButton
        text="상세 보기"
        type="outline"
        size="small"
        @click="goDetail(props.membership.id)"
      />
    </div>
  </BaseCard>
  <div class="membership-card">


    <div class="membership-item">


      <span class="membership-main">
        <img
          v-if="membership.logo"
          :src="membership.logo"
          :alt="`${membership.name} 로고`"
          class="membership-logo"
        />
        <span class="membership-name">{{ membership.name }}</span>
      </span>


      <button
        class="move-button"
        @click="goDetail(membership.id)"
      >
        상세 보기
      </button>


    </div>


  </div>

</template>

<style scoped>
.membership-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.membership-name {
  font-size: var(--font-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
}
</style>

.membership-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.membership-logo {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  border-radius: 12px;
  object-fit: contain;
  background: #fff;
}


.move-button {

  border: none;

  background: #f3f4f6;

  border-radius: 8px;

  padding: 8px 12px;

  font-size: 13px;

  cursor: pointer;

}

</style>
<!-- 07_25 연동 변경: 멤버십 API 데이터와 제휴사 로고를 카드에 표시한다. -->
