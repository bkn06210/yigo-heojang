<script setup>

import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()


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

  <div class="membership-card">


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

</style>