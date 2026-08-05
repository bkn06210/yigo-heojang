<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useCardStore } from '@/stores/cardStore';
import { getUserCardCandidates, registerUserCard } from '@/api/walletApi';

import PageHeader from '@/components/common/PageHeader.vue';

import CardScanModal from '@/components/card/CardScanModal.vue';
import CardUploadModal from '@/components/card/CardUploadModal.vue';
import CardRegisterCompleteModal from '@/components/card/CardRegisterCompleteModal.vue';

const cardStore = useCardStore();

// 카드 인식 방법 선택 상태
const registerType = ref(null);

const router = useRouter();


// 모달 상태 관리

// 카드 촬영 모달
const showScan = ref(false);

// 사진 첨부 모달
const showUpload = ref(false);

// 등록 완료 모달
const showComplete = ref(false);


// 카드 등록 정보
const cardName = ref('');
const cardNumber = ref('');
const expiryDate = ref('');
const cvc = ref('');
const password = ref('');
const residentNumber = ref('');

// PR #29 연동: BIN 조회 결과와 사용자가 선택한 실제 카드 상품 ID를 보관한다.
const cardCandidates = ref([]);
const selectedCardId = ref(null);
const registerError = ref('');
const identifying = ref(false);
const registering = ref(false);

// PR #29 연동: 서버에는 숫자만 전달해 BIN 조회와 Luhn 검증이 동일하게 적용되도록 한다.
const normalizedCardNumber = () => cardNumber.value.replace(/[^0-9]/g, '');

// PR #29 연동: POST /api/user-cards/candidates로 카드 상품 후보를 불러온다.
const loadCardCandidates = async () => {
  const number = normalizedCardNumber();
  if (number.length < 13) return false;

  identifying.value = true;
  registerError.value = '';
  try {
    const response = await getUserCardCandidates(number);
    cardCandidates.value = response?.cards || [];
    selectedCardId.value = cardCandidates.value.length === 1 ? cardCandidates.value[0].cardId : null;

    // PR #29 연동: 후보가 하나면 기존 카드명 입력칸도 서버 카드명으로 자동 채운다.
    if (selectedCardId.value) cardName.value = cardCandidates.value[0].cardName;
    return cardCandidates.value.length > 0;
  } catch (error) {
    cardCandidates.value = [];
    selectedCardId.value = null;
    registerError.value = error?.response?.data?.message || error?.message || '카드 상품을 확인하지 못했습니다.';
    return false;
  } finally {
    identifying.value = false;
  }
};

// PR #29 연동: 후보 선택 시 카드명을 서버 마스터 데이터와 일치시킨다.
const selectCandidate = () => {
  const selected = cardCandidates.value.find((candidate) => candidate.cardId === Number(selectedCardId.value));
  if (selected) cardName.value = selected.cardName;
};


// 카드 인식 방법 선택
// 카드 촬영 선택
const openScan = () => {

  registerType.value = 'scan';

  showScan.value = true;

};


// 사진 첨부 선택
const openUpload = () => {

  registerType.value = 'upload';

  showUpload.value = true;

};


// 직접 입력 선택
const selectManual = () => {

  registerType.value = 'manual';

};


// 인식 결과 반영
const completeScan = (data) => {

  cardName.value = data.cardName;

  cardNumber.value = formatCardNumber(
    data.cardNumber
  );

  expiryDate.value = data.expiryDate;


  // 인식 모달 닫기
  showScan.value = false;
  showUpload.value = false;

};


// 입력 포맷 처리

// 카드번호 포맷
// 1234567890123456
// ↓
// 1234-5678-9012-3456

const formatCardNumber = (number) => {

  return number
    .replace(/[^0-9]/g, '')
    .slice(0, 16)
    .replace(/(\d{4})(?=\d)/g, '$1-');

};


// 직접 입력 카드번호
const handleCardNumberInput = () => {

  cardNumber.value =
    cardNumber.value
      .replace(/[^0-9]/g, '')
      .slice(0, 16)
      .replace(/(\d{4})(?=\d)/g, '$1-');

};


// 만료일 MM/YY

const formatExpiryDate = () => {

  expiryDate.value =
    expiryDate.value
      .replace(/[^0-9]/g, '')
      .slice(0, 4)
      .replace(/(\d{2})(?=\d)/, '$1/');

};


// CVC

const formatCvc = () => {

  cvc.value =
    cvc.value
      .replace(/[^0-9]/g, '')
      .slice(0, 3);

};


// 카드 비밀번호

const formatPassword = () => {

  password.value =
    password.value
      .replace(/[^0-9]/g, '')
      .slice(0, 2);

};


// 주민번호 앞자리

const formatResidentNumber = () => {

  residentNumber.value =
    residentNumber.value
      .replace(/[^0-9]/g, '')
      .slice(0, 6);

};

// 카드 등록
// PR #29 연동 이전 임시 Store 등록 로직이며 실제 버튼에서는 더 이상 호출하지 않는다.
const registerCardLegacy = () => {

  console.log('카드 등록 클릭');


  console.log({

    showScan: showScan.value,

    showUpload: showUpload.value,

    showComplete: showComplete.value,
    

  });



  /*
    임시 저장
    TODO: 추후 카드 등록 API 연결 시 교체
  */

  const newCard = {

    id: Date.now(),

    name: cardName.value,

    company: 'KB국민카드',

    image: '',

    cardNumber: cardNumber.value,

    pinned: false,

  };


 cardStore.addCard(newCard);


// 테스트 로그 추가
console.log('등록 후 카드:', cardStore.cards);


  /*
    실제 서비스에서는:

    1. 입력값 검증
    2. 본인 인증 요청
    3. 카드 등록 API 요청
    4. 성공 응답

    이후 완료 팝업 표시
  */


  showComplete.value = true;

};


// 카드 목록 이동
// PR #29 연동: 선택한 카드 상품을 실제 보유카드 등록 API로 저장한다.
const registerCard = async () => {
  registerError.value = '';

  // PR #29 연동: 후보를 아직 조회하지 않았다면 등록 직전에 BIN 조회를 수행한다.
  if (!cardCandidates.value.length && !(await loadCardCandidates())) return;

  if (!selectedCardId.value) {
    registerError.value = '등록할 카드 상품을 선택해 주세요.';
    return;
  }

  registering.value = true;
  try {
    // PR #29 연동: 민감한 부가 입력값은 제외하고 cardId와 카드번호만 등록 API로 보낸다.
    const registered = await registerUserCard(Number(selectedCardId.value), normalizedCardNumber());

    // PR #29 연동: 등록 응답을 즉시 Store에 반영해 완료 후 목록에서도 확인할 수 있게 한다.
    cardStore.addCard({
      id: registered.userCardId,
      name: registered.cardName,
      company: registered.issuerName,
      image: registered.imageUrl || '',
      cardNumber: registered.maskedCardNumber,
      pinned: Boolean(registered.representative),
    });
    showComplete.value = true;
  } catch (error) {
    registerError.value = error?.response?.data?.message || error?.message || '카드 등록에 실패했습니다.';
  } finally {
    registering.value = false;
  }
};

const goCardList = () => {

  showComplete.value = false;

  router.push('/cards');

};

</script>

<template>

<div class="page">


<PageHeader title="카드 등록"/>

<section class="register-type">


<h3>
카드 인식 방법
</h3>


<div class="type-list">


<button
  class="type-card"
  :class="{ active: registerType === 'scan' }"
  @click="openScan"
>

📷

<span>
카드 촬영
</span>

<p>
카드 정보를 자동 인식합니다
</p>

</button>

<button
  class="type-card"
  :class="{ active: registerType === 'upload' }"
  @click="openUpload"
>

🖼️

<span>
사진 첨부
</span>

<p>
저장된 카드 이미지로 자동 인식합니다
</p>

</button>


<button
  class="type-card"
  :class="{ active: registerType === 'manual' }"
  @click="selectManual"
>

✍️

<span>
직접 입력
</span>

<p>
카드 정보를 직접 입력합니다
</p>

</button>

</div>

</section>

<section class="form">

<h3>
카드 정보 입력
</h3>

<!-- 카드명 -->

<div class="input-box">

<label>
카드명
</label>

<input
v-model="cardName"
placeholder="카드명을 입력해주세요"
/>

</div>


<!-- 카드 번호 -->

<div class="input-box">

<label>
카드 번호
</label>


<input
v-model="cardNumber"
maxlength="19"
inputmode="numeric"
placeholder="0000-0000-0000-0000"
@input="handleCardNumberInput"
@blur="loadCardCandidates"
/>

</div>

<!-- PR #29 연동: BIN 조회 결과가 여러 개면 실제 카드 상품을 사용자가 선택한다. -->
<div v-if="cardCandidates.length" class="input-box">
  <label>카드 상품 선택</label>
  <select v-model.number="selectedCardId" @change="selectCandidate">
    <option :value="null" disabled>카드 상품을 선택해 주세요</option>
    <option v-for="candidate in cardCandidates" :key="candidate.cardId" :value="candidate.cardId">
      {{ candidate.issuerName }} · {{ candidate.cardName }}
    </option>
  </select>
</div>

<!-- PR #29 연동: 후보 조회 및 등록 API 오류를 현재 폼 안에서 안내한다. -->
<p v-if="registerError" class="error">{{ registerError }}</p>

<!-- 만료일 -->

<div class="input-box">

<label>
만료일
</label>


<input
v-model="expiryDate"
maxlength="5"
inputmode="numeric"
placeholder="MM/YY"
@input="formatExpiryDate"
/>

</div>

<!-- CVC -->

<div class="input-box">


<label>
보안코드(CVC/CVV)
</label>

<input
type="password"
v-model="cvc"
maxlength="3"
inputmode="numeric"
placeholder="카드 뒷면의 3자리 숫자"
@input="formatCvc"
/>

</div>

<!-- 비밀번호 -->

<div class="input-box">

<label>
카드 비밀번호
</label>


<input
type="password"
v-model="password"
maxlength="2"
inputmode="numeric"
placeholder="앞 2자리"
@input="formatPassword"
/>

</div>

<!-- 주민번호 -->

<div class="input-box">

<label>
주민등록번호
</label>

<input
  v-model="residentNumber"
  maxlength="6"
  placeholder="생년월일 6자리"
  @input="formatResidentNumber"
/>

</div>

</section>
<!-- 카드 등록 버튼 -->
<button
  class="register-button"
  :disabled="identifying || registering"
  @click="registerCard"
>
  카드 등록
</button>


<!-- 카드 촬영 모달 -->
<CardScanModal
  v-if="showScan"
  @close="showScan = false"
  @complete="completeScan"
/>

<CardRegisterCompleteModal
  v-if="showComplete"
  @close="showComplete=false"
  @confirm="goCardList"
/>

<CardUploadModal
  v-if="showUpload"
  @close="showUpload=false"
  @complete="completeScan"
/>


</div>
</template>



<style scoped>

.page{

padding:20px;

}


.register-type,
.form{

margin-top:24px;

}


h3{

font-size:16px;

margin-bottom:14px;

}



.type-list{

display:flex;

gap:12px;

}



.type-card{

flex:1;

padding:20px 10px;

border-radius:16px;

border:1px solid #ddd;

background:white;

}



.type-card.active{

border:2px solid #4F46E5;

}



.type-card span{

display:block;

font-weight:700;

margin-top:8px;

}



.type-card p{

font-size:12px;

color:#888;

}



.input-box{

margin-top:16px;

display:flex;

flex-direction:column;

gap:8px;

}



.input-box label{

font-size:14px;

color:#555;

}



.input-box input{

height:48px;

border-radius:12px;

border:1px solid #ddd;

padding:0 14px;

font-size:15px;

}

 

.register-button{

width:100%;

height:52px;

margin-top:30px;

border:none;

border-radius:14px;

background:#4F46E5;

color:white;

font-size:16px;

}

</style>
<!-- 07_25 연동 변경: 카드번호 확인과 보유카드 등록 API를 기존 등록 UI에 연결한다. -->
