
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMembershipUsagePlaces } from '@/constants/membershipUsagePlaces'
import PageHeader from '@/components/common/PageHeader.vue'
import BottomNav from '@/components/common/BottomNav.vue'

import {
  getMembershipDetail,
  cancelMembership,
} from '@/api/membershipApi'

const route = useRoute()
const router = useRouter()

const membership = ref(null)
const loading = ref(false)
const error = ref('')

const fetchMembershipDetail = async () => {
  loading.value = true
  error.value = ''

  try {
    const membershipRegisterId = route.params.id

    const response = await getMembershipDetail(membershipRegisterId)

    membership.value = response.data?.data || null

    if (!membership.value) {
      error.value = '멤버십 정보를 찾을 수 없습니다.'
    }
  } catch (err) {
    console.error('멤버십 상세 조회 실패', err)
    error.value = '멤버십 상세 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const membershipName = computed(() => {
  return (
      membership.value?.providerName ||
      membership.value?.pointProviderName ||
      membership.value?.name ||
      membership.value?.membershipName ||
      ''
  )
})
const usagePlaces = computed(() => {
  const mappedPlaces = getMembershipUsagePlaces(membershipName.value)

  if (mappedPlaces.length > 0) {
    return mappedPlaces
  }

  const places =
      membership.value?.usagePlaces ||
      membership.value?.mainUsagePlaces ||
      membership.value?.places ||
      []

  if (Array.isArray(places) && places.length > 0) {
    return places.map((place) => {
      if (typeof place === 'string') {
        return place
      }

      return (
          place.placeName ||
          place.usagePlaceName ||
          place.categoryName ||
          place.description ||
          '사용처 정보'
      )
    })
  }

  return []
})



const getOfficialSiteUrlByName = (name) => {
  const urlMap = {
    'CJ ONE': 'https://www.cjone.com/cjmweb/join.do',
    '해피포인트': 'https://www.happypointcard.com/page/presentation/membership.spc',
    'KT 멤버십': 'https://membership.kt.com/main/MainInfo.do',
    'L.POINT': 'https://m.lpoint.com/app/membership/LWMS100100.do',
    'OK캐쉬백': 'https://www.okcashbag.com/',
    '신세계포인트': 'https://www.shinsegae.com/service/membership/shinsegae-point.do',
    'GS ALL 멤버십': 'https://www.gsall.com/',
    'U+ 멤버십': 'https://m.lguplus.com/membership/intro',
  }

  return urlMap[name] || ''
}

const officialSiteUrl = computed(() => {
  return (
      membership.value?.officialSiteUrl ||
      membership.value?.siteUrl ||
      membership.value?.homepageUrl ||
      membership.value?.link ||
      getOfficialSiteUrlByName(membershipName.value) ||
      ''
  )
})

const goOfficialSite = () => {
  if (!officialSiteUrl.value) {
    alert('등록된 공식 사이트 정보가 없습니다.')
    return
  }

  window.open(officialSiteUrl.value, '_blank')
}

const deleteMembership = async () => {
  const membershipRegisterId = route.params.id

  if (!confirm('멤버십을 삭제하시겠습니까?')) {
    return
  }

  try {
    await cancelMembership(membershipRegisterId)

    alert('멤버십이 삭제되었습니다.')
    router.push('/points')
  } catch (err) {
    console.error('멤버십 삭제 실패', err)
    alert('멤버십 삭제에 실패했습니다.')
  }
}

onMounted(() => {
  fetchMembershipDetail()
})
</script>

<template>
  <div class="membership-detail">
    <!-- 페이지 제목 -->
    <PageHeader title="멤버십 상세" />

    <p v-if="loading" class="status-text">
      멤버십 정보를 불러오는 중입니다.
    </p>

    <p v-else-if="error" class="status-text error">
      {{ error }}
    </p>

    <template v-else>
      <!-- 멤버십 이름 -->
      <section class="title-section">
        <h1>
          {{ membershipName }}
        </h1>
      </section>

      <!-- 등록 정보 -->
      <section class="info-section">
        <h2>
          등록 정보
        </h2>

        <p>
          {{ membershipName }}
        </p>
      </section>

      <!-- 주요 사용처 -->
      <section class="usage-section">
        <h2>
          주요 사용처
        </h2>

        <ul v-if="usagePlaces.length > 0">
          <li
              v-for="place in usagePlaces"
              :key="place"
          >
            {{ place }}
          </li>
        </ul>

        <p v-else class="empty-text">
          등록된 사용처 정보가 없습니다.
        </p>
      </section>

      <!-- 버튼 영역 -->
      <section class="button-section">
        <button
            class="official-button"
            @click="goOfficialSite"
        >
          공식 사이트 이동
        </button>

        <button
            class="delete-button"
            @click="deleteMembership"
        >
          멤버십 삭제
        </button>
      </section>
    </template>

    <BottomNav />
  </div>
</template>

<style scoped>
.membership-detail {
  min-height: 100vh;
  padding: 20px;
  padding-bottom: 80px;
}

.title-section {
  margin-top: 32px;
  margin-bottom: 28px;
}

.title-section h1 {
  font-size: 26px;
  font-weight: 700;
}

.info-section,
.usage-section {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-top: 20px;
}

h2 {
  font-size: 18px;
  margin-bottom: 16px;
}

.info-section p {
  font-size: 15px;
  color: #333;
}

ul {
  padding-left: 22px;
  margin: 0;
}

li {
  font-size: 15px;
  margin: 8px 0;
}

.button-section {
  margin-top: 32px;
}

button {
  width: 100%;
  height: 50px;
  border-radius: 12px;
  font-size: 15px;
  cursor: pointer;
}

.official-button {
  background: #2454e6;
  color: white;
  border: none;
  margin-bottom: 14px;
}

.delete-button {
  background: white;
  color: #ef4444;
  border: 1px solid #ef4444;
}

.status-text {
  font-size: 14px;
  color: #666;
  padding: 20px 0;
}

.error {
  color: #dc2626;
}

.empty-text {
  font-size: 14px;
  color: #777;
}
</style>