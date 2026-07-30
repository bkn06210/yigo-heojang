<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import PageHeader from '@/components/common/PageHeader.vue'
import BottomNav from '@/components/common/BottomNav.vue'
import PullToRefresh from '@/components/common/PullToRefresh.vue'

import FinancialPointCard from '@/components/point/FinancialPointCard.vue'
import MembershipCard from '@/components/point/MembershipCard.vue'

import { getPoints } from '@/api/pointApi'
import { getMyMemberships } from '@/api/membershipApi'

const router = useRouter()

const financialPointList = ref([])
const membershipList = ref([])

const loading = ref(false)
const error = ref('')

const fetchFinancialPoints = async () => {
  const response = await getPoints()
  const points = response.data?.data?.points || []

  console.log('포인트 API 응답:', points)

  financialPointList.value = points
      .filter((point) => point.providerType === 'POINT')
      .map((point) => ({
        id: point.pointWalletId,
        pointWalletId: point.pointWalletId,
        pointProviderId: point.pointProviderId,
        name: point.providerName,
        providerName: point.providerName,
        point: point.totalPoint || 0,
        totalPoint: point.totalPoint || 0,
        logoImage: point.logoImage,
      }))
}

const fetchMemberships = async () => {
  const response = await getMyMemberships()
  const memberships = response.data?.data?.memberships || []

  console.log('멤버십 API 응답:', memberships)

  membershipList.value = memberships.map((membership) => ({
    id: membership.membershipRegisterId,
    membershipRegisterId: membership.membershipRegisterId,
    pointProviderId: membership.pointProviderId,
    name: membership.providerName,
    providerName: membership.providerName,
    point: membership.totalPoint || 0,
    totalPoint: membership.totalPoint || 0,
    logoImage: membership.logoImageUrl,
    link: `/memberships/${membership.membershipRegisterId}`,
  }))
}

const refreshPoint = async () => {
  console.log('포인트/멤버십 데이터 갱신 시작')

  loading.value = true
  error.value = ''

  try {
    await fetchFinancialPoints()
    await fetchMemberships()

    console.log('포인트/멤버십 데이터 갱신 완료')
  } catch (err) {
    console.error('포인트/멤버십 데이터 갱신 실패', err)
    error.value = '포인트 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const goMembershipRegister = () => {
  router.push('/memberships/register')
}

onMounted(() => {
  refreshPoint()
})
</script>

<template>
  <PullToRefresh @refresh="refreshPoint">
    <div class="point-page">
      <!-- 페이지 제목 -->
      <PageHeader title="포인트" />

      <main class="content">
        <p v-if="loading" class="notice">
          포인트 정보를 불러오는 중입니다.
        </p>

        <p v-else-if="error" class="notice error">
          {{ error }}
        </p>

        <template v-else>
          <!-- 금융 포인트 영역 -->
          <section class="point-section">
            <h2>
              금융 포인트
            </h2>

            <FinancialPointCard :points="financialPointList" />
          </section>

          <!-- 멤버십 영역 -->
          <section class="membership-section">
            <div class="section-header">
              <h2>
                멤버십
              </h2>

              <button
                  class="add-button"
                  @click="goMembershipRegister"
              >
                + 추가
              </button>
            </div>

            <p v-if="membershipList.length === 0" class="notice">
              등록된 멤버십 포인트가 없습니다.
            </p>

            <template v-else>
              <MembershipCard
                  v-for="membership in membershipList"
                  :key="membership.id"
                  :membership="membership"
              />
            </template>

            <p class="notice">
              ※ 멤버십 상세 페이지에서 사용처 및 이용 정보를 확인할 수 있습니다.
            </p>
          </section>
        </template>
      </main>

      <BottomNav />
    </div>
  </PullToRefresh>
</template>

<style scoped>
.point-page {
  min-height: 100vh;
  padding-bottom: 80px;
}

.content {
  padding: 20px;
}

.point-section {
  margin-bottom: 32px;
}

.membership-section {
  margin-bottom: 32px;
}

h2 {
  font-size: 18px;
  margin-bottom: 14px;
}

button {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header h2 {
  margin-bottom: 0;
}

.add-button {
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 600;
}

.notice {
  margin-top: 14px;
  font-size: 12px;
  color: #777;
  line-height: 1.5;
}

.error {
  color: #dc2626;
}
</style>