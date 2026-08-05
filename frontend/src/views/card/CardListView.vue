<script setup>

import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';

import { useCardStore } from '@/stores/cardStore';
import { useAuthStore } from '@/stores/authStore';

import CardItem from '@/components/card/CardItem.vue';
import CardCompanyGroup from '@/components/card/CardCompanyGroup.vue';
import EmptyStateCard from '@/components/common/EmptyStateCard.vue';
import BottomNavigation from '@/components/layout/BottomNavigation.vue';



const router = useRouter();


// 카드 Store 연결
const cardStore = useCardStore();
const authStore = useAuthStore();

const { cards } = storeToRefs(cardStore);

// 테스트 로그 추가
console.log('카드 목록 진입:', cards.value);



// 로그인 상태
// TODO: 추후 authStore 연결
// PR #25 연동: 로그인 상태일 때만 인증이 필요한 카드 현황 API를 호출한다.
const isLogin = computed(() => Boolean(authStore.token || localStorage.getItem('token')));

// PR #32 연동: 기본 보유카드 목록을 조회한 뒤 PR #25 실적 정보를 카드 ID로 결합한다.
onMounted(async () => {
  if (!isLogin.value) return;
  try {
    await cardStore.loadCards();
  } catch (error) {
    console.error('카드 현황 조회 실패', error);
  }
});



// 로그인 이동
const goLogin = () => {

  router.push('/auth/login');

};


// 카드 등록 이동
const goRegister = () => {

  router.push('/cards/register');

};


// 고정 카드 목록
const pinnedCards = computed(() => {


  return [...cards.value]

    .filter(card => card.pinned)

    .sort((a, b) =>

      a.name.localeCompare(b.name, 'ko')

    );


});



// 카드사별 그룹
const cardCompanies = computed(() => {


  const groups = {};



  cards.value.forEach(card => {


    if (!groups[card.company]) {


      groups[card.company] = {

        name: card.company,

        cards: [],

      };


    }



    groups[card.company].cards.push(card);

  });


  return Object.values(groups)

    .sort((a, b) =>

      a.name.localeCompare(b.name, 'ko')

    );


});


// 카드 고정
const togglePin = (id) => {


  const target = cards.value.find(

    card => card.id === id

  );


  if (!target) return;


  const pinnedCount =

    cards.value.filter(

      card => card.pinned

    ).length;



  if (!target.pinned && pinnedCount >= 3) {


    alert(

      '고정 카드는 최대 3개까지 가능합니다.'

    );


    return;


  }


  target.pinned = !target.pinned;

};



</script>

<template>

<div class="card-list-page">



  <!-- 비로그인 -->

  <EmptyStateCard

    v-if="!isLogin"

    title="로그인 후 이용할 수 있어요"

    description="로그인하면 내 카드를 등록하고 관리할 수 있습니다."

    buttonText="로그인"

    @click="goLogin"

  />



  <!-- 로그인 + 카드 없음 -->

  <EmptyStateCard

    v-else-if="cards.length === 0"

    title="등록된 카드가 없어요"

    description="카드를 등록하면 혜택과 소비 관리를 시작할 수 있습니다."

    buttonText="카드 등록"

    @click="goRegister"

  />




  <!-- 로그인 + 카드 있음 -->

  <template v-else>



    <header class="header">


      <h1>
        내 카드
      </h1>




      <button

        class="register-button"

        @click="goRegister"

      >

        + 카드 등록

      </button>



    </header>




    <!-- 고정 카드 -->

    <section class="card-section">


      <h2>
        📌 고정 카드
      </h2>




      <CardItem

        v-for="card in pinnedCards"

        :key="card.id"

        :card="card"

        @toggle-pin="togglePin"

      />



    </section>




    <!-- 전체 카드 -->

    <section class="card-section">


      <h2>
        전체 카드
      </h2>




      <CardCompanyGroup

        v-for="company in cardCompanies"

        :key="company.name"

        :company="company"

        @toggle-pin="togglePin"

      />



    </section>



  </template>
  
<BottomNavigation />

</div>



</template>



<style scoped>


.card-list-page {

  padding:20px;

  min-height:100vh;

  background:#fafafa;

}





.header {

  display:flex;

  justify-content:space-between;

  align-items:center;

  margin-bottom:24px;

}





.header h1 {

  margin:0;

  font-size:24px;

  font-weight:700;

}





.register-button {


  border:none;

  background:#4F46E5;

  color:white;


  padding:11px 16px;


  border-radius:14px;


  font-size:14px;


  font-weight:600;


  cursor:pointer;


}





.card-section {


  margin-bottom:32px;


}





.card-section h2 {


  font-size:18px;


  margin-bottom:16px;


  font-weight:700;


}






:deep(.empty-card) {


  margin-top:40px;


  background:white;


  border-radius:20px;


  padding:36px 20px;


  box-shadow:

    0 4px 12px rgba(0,0,0,0.06);


}




:deep(.empty-card button:first-child) {


  background:#4F46E5;


  color:white;


}




</style>
<!-- 07_25 연동 변경: 로그인 회원의 실제 보유카드 목록 API를 화면 진입 시 호출한다. -->
