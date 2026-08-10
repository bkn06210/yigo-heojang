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
