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

console.log('MembershipCard props:', props.membership)

const goDetail = (id) => {
  router.push(`/memberships/${id}`)
}
</script>

<template>
  <BaseCard class="membership-card">
    <div class="membership-item">
      <div class="membership-info">
        <img
          v-if="props.membership.logoImageUrl"
          :src="props.membership.logoImageUrl"
          :alt="props.membership.providerName || props.membership.name"
          class="membership-logo"
        />
        <span class="membership-name">
          {{ props.membership.providerName || props.membership.name }}
        </span>
      </div>

      <AppButton
        text="상세 보기"
        type="outline"
        size="small"
        @click="goDetail(props.membership.id)"
      />
    </div>
  </BaseCard>
</template>

<style scoped>
.membership-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
}

.membership-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex: 1;
  min-width: 0;
}

.membership-logo {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  object-fit: cover;
  flex-shrink: 0;
}

.membership-name {
  font-size: var(--font-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
