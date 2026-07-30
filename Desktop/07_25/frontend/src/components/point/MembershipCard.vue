<script setup>

import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const getLogoImage = () => {
  return props.membership.logoImageUrl || props.membership.logoImage || ''
}

const goDetail = (id) => {
  router.push(`/memberships/${id}`)
}

// 부모(PointListView)에서 받은 멤버십 데이터
const props = defineProps({
  membership: {
    type: Object,
    required: true
  }
})



// 외부 멤버십 페이지 이동
const goMembershipPage = (link) => {

  window.open(link, '_blank')

}

const emit = defineEmits([
  'add-membership'
])

</script>


<template>
  <div class="logo-box">
    <img
        v-if="getLogoImage()"
        :src="getLogoImage()"
        :alt="membership.providerName || membership.name"
        class="logo-image"
    />

    <span v-else class="logo-fallback">
    {{ (membership.providerName || membership.name || 'M').charAt(0) }}
  </span>
  </div>
  <div class="membership-item">

  <span class="membership-name">
    {{ membership.name }}
  </span>


  <button
    class="move-button"
    @click="goDetail(membership.id)"
  >
    상세 보기
  </button>

</div>




</template>


<style scoped>

.membership-card {

  background: white;

  border-radius: 16px;

  padding: 20px;

  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.08);

}


.membership-item {

  display: flex;

  justify-content: space-between;

  align-items: center;

  padding: 14px 0;

}


.membership-item:not(:last-child) {

  border-bottom: 1px solid #eeeeee;

}


.membership-name {

  font-size: 15px;

  font-weight: 500;

}


.move-button {

  border: none;

  background: #f3f4f6;

  border-radius: 8px;

  padding: 8px 12px;

  font-size: 13px;

  cursor: pointer;

}

.logo-box {
  width: 48px;
  height: 48px;
  background: transparent;
  border-radius: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
  flex-shrink: 0;
}

.logo-image {
  max-width: 48px;
  max-height: 48px;
  width: auto;
  height: auto;
  object-fit: contain;
  display: block;
  padding: 0;
}

.logo-fallback {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #555;
}

.logo-fallback {
  font-size: 18px;
  font-weight: 700;
  color: #555;
}
</style>