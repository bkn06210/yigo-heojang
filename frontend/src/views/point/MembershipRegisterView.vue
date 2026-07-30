<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMembershipUsagePlaces } from '@/constants/membershipUsagePlaces'
import {
  getMembershipProviders,
  getMyMemberships,
  registerMembership,
} from '@/api/membershipApi'

const router = useRouter()

const keyword = ref('')
const showModal = ref(false)
const selectedMembership = ref(null)
const membershipList = ref([])

const getProviderName = (provider) => {
  return (
      provider.providerName ||
      provider.pointProviderName ||
      provider.name ||
      provider.membershipName ||
      ''
  )
}

const getMainUses = (name) => {
  return getMembershipUsagePlaces(name)
}

const fetchMembershipProviders = async () => {
  try {
    const providerResponse = await getMembershipProviders()
    const providers = providerResponse.data?.data?.providers || []

    const myResponse = await getMyMemberships()
    const myMemberships = myResponse.data?.data?.memberships || []

    const registeredProviderIds = myMemberships.map((membership) => {
      return membership.pointProviderId
    })

    membershipList.value = providers
        .filter((provider) => {
          return !registeredProviderIds.includes(provider.pointProviderId)
        })
        .map((provider) => {
          const providerName = getProviderName(provider)
          const mainUses = getMainUses(providerName)

          return {
            id: provider.pointProviderId,
            pointProviderId: provider.pointProviderId,
            name: providerName,
            providerName,
            logoImageUrl: provider.logoImageUrl,
            logoImage: provider.logoImageUrl,
            mainUses,
          }
        })
  } catch (error) {
    console.error('멤버십 제공사 목록 조회 실패', error)
    alert('멤버십 목록을 불러오지 못했습니다.')
  }
}

const searchResult = computed(() => {
  if (!keyword.value.trim()) {
    return []
  }

  return membershipList.value.filter((membership) =>
      membership.name
          .toLowerCase()
          .includes(keyword.value.toLowerCase())
  )
})

const escapeRegExp = (text) => {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

const highlightKeyword = (name, searchText) => {
  if (!searchText) {
    return name
  }

  const regex = new RegExp(`(${escapeRegExp(searchText)})`, 'gi')

  return name.replace(
      regex,
      '<span class="highlight">$1</span>'
  )
}

const popularMemberships = computed(() => {
  return membershipList.value
})

const selectedMembershipUsagePlaces = computed(() => {
  if (!selectedMembership.value) {
    return []
  }

  const name =
      selectedMembership.value.providerName ||
      selectedMembership.value.name ||
      ''

  const mappedPlaces = getMembershipUsagePlaces(name)

  if (mappedPlaces.length > 0) {
    return mappedPlaces
  }

  return selectedMembership.value.mainUses || []
})

const addMembership = async (membership) => {
  try {
    await registerMembership(membership.pointProviderId)

    const name =
        membership.providerName ||
        membership.name ||
        ''

    selectedMembership.value = {
      ...membership,
      mainUses: getMembershipUsagePlaces(name),
    }

    showModal.value = true
  } catch (error) {
    console.error('멤버십 추가 실패', error)
    alert('멤버십 추가에 실패했습니다.')
  }
}

const closeModal = () => {
  showModal.value = false
  selectedMembership.value = null
  keyword.value = ''

  fetchMembershipProviders()
}

const goMembershipList = () => {
  showModal.value = false
  keyword.value = ''

  router.push('/points')
}

const selectSuggestion = (membership) => {
  keyword.value = membership.name
}

onMounted(() => {
  fetchMembershipProviders()
})
</script>

<template>
  <div class="membership-register">
    <h1>
      멤버십 추가
    </h1>

    <p class="description">
      자주 사용하는 멤버십을 추가해보세요
    </p>

    <div class="search-box">
      <input
          :value="keyword"
          @input="keyword = $event.target.value"
          placeholder="멤버십 이름을 입력해주세요"
      />
    </div>

    <div
        v-if="searchResult.length"
        class="suggestion-box"
    >
      <div
          v-for="membership in searchResult"
          :key="membership.id"
          class="suggestion-item"
          @click="selectSuggestion(membership)"
      >
        <div class="suggestion-info">
          <span
              class="suggestion-name"
              v-html="highlightKeyword(membership.name, keyword)"
          ></span>

          <div class="usage-list small">
            <span
                v-for="use in membership.mainUses"
                :key="use"
                class="usage-chip"
            >
              {{ use }}
            </span>
          </div>
        </div>

        <button
            @click.stop="addMembership(membership)"
        >
          추가
        </button>
      </div>
    </div>

    <section class="section">
      <h2>
        많이 사용하는 멤버십
      </h2>

      <div
          v-for="membership in popularMemberships"
          :key="membership.id"
          class="membership-item"
      >
        <div class="membership-info">
          <span class="membership-name">
            {{ membership.name }}
          </span>

          <div class="usage-list">
            <span
                v-for="use in membership.mainUses"
                :key="use"
                class="usage-chip"
            >
              {{ use }}
            </span>
          </div>
        </div>

        <button
            @click="addMembership(membership)"
        >
          추가
        </button>
      </div>
    </section>

    <div
        v-if="showModal && selectedMembership"
        class="modal-background"
    >
      <div class="modal">
        <h2>
          {{ selectedMembership.name }} 추가 완료!
        </h2>

        <p class="modal-title">
          주요 사용처
        </p>

        <ul class="modal-usage-list">
          <li
              v-for="use in selectedMembershipUsagePlaces"
              :key="use"
          >
            {{ use }}
          </li>
        </ul>

        <button
            class="confirm-button"
            @click="goMembershipList"
        >
          멤버십 보러 가기
        </button>

        <button
            class="more-button"
            @click="closeModal"
        >
          다른 멤버십 추가
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.membership-register {
  padding: 20px;
}

h1 {
  font-size: 24px;
}

.description {
  margin: 16px 0;
  color: #555;
}

.search-box input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid #ddd;
  box-sizing: border-box;
}

.section {
  margin-top: 28px;
}

h2 {
  font-size: 17px;
  margin-bottom: 12px;
}

.membership-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #eee;
}

.membership-info {
  flex: 1;
}

.membership-name {
  display: block;
  font-weight: 700;
  margin-bottom: 8px;
}

.usage-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.usage-list.small {
  margin-top: 8px;
}

.usage-chip {
  padding: 5px 8px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 12px;
}

button {
  border: none;
  background: #1d4ed8;
  color: white;
  border-radius: 8px;
  padding: 8px 14px;
  flex-shrink: 0;
}

.modal-background {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal {
  background: white;
  width: 80%;
  max-height: 80vh;
  overflow-y: auto;
  border-radius: 16px;
  padding: 24px;
}

.modal-title {
  margin-top: 20px;
  font-weight: 600;
}

.modal-usage-list {
  padding-left: 20px;
  margin-bottom: 20px;
}

.modal-usage-list li {
  margin: 6px 0;
}

.confirm-button {
  width: 100%;
  height: 44px;
  background: #1d4ed8;
  color: white;
}

.more-button {
  width: 100%;
  height: 44px;
  margin-top: 12px;
  background: #f3f4f6;
  color: #333;
}

.suggestion-box {
  margin-top: 4px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 12px;
  overflow: hidden;
}

.suggestion-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-item:hover {
  background: #f8f8f8;
}

.suggestion-info {
  flex: 1;
}

.suggestion-name {
  font-weight: 600;
}

:deep(.highlight) {
  color: #1d4ed8;
  font-weight: 700;
}
</style>