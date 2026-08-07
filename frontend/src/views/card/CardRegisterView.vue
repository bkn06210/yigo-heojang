<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useCardStore } from '@/stores/cardStore';
<<<<<<< HEAD
=======
import { getUserCardCandidates, registerUserCard } from '@/api/walletApi';
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

import PageHeader from '@/components/common/PageHeader.vue';

import CardScanModal from '@/components/card/CardScanModal.vue';
import CardUploadModal from '@/components/card/CardUploadModal.vue';
import CardRegisterCompleteModal from '@/components/card/CardRegisterCompleteModal.vue';
<<<<<<< HEAD
import Icon from '@/components/common/Icon.vue';
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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
<<<<<<< HEAD
=======
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
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


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


<<<<<<< HEAD
=======
// 직접 입력 선택
const selectManual = () => {

  registerType.value = 'manual';

};


>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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


<<<<<<< HEAD
// 카드 등록
const registerCard = () => {
=======
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
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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

<<<<<<< HEAD
  // cardName에서 회사명 추출 (예: "삼성 ID one" → "삼성카드")
  const extractCompany = (name) => {
    if (name.includes('삼성')) return '삼성카드';
    if (name.includes('롯데')) return '롯데카드';
    if (name.includes('현대')) return '현대카드';
    if (name.includes('신한')) return '신한카드';
    if (name.includes('국민') || name.includes('KB')) return 'KB국민카드';
    if (name.includes('우리')) return '우리카드';
    if (name.includes('하나')) return '하나카드';
    if (name.includes('NH')) return 'NH농협카드';
    return 'KB국민카드'; // 기본값
  };

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  const newCard = {

    id: Date.now(),

    name: cardName.value,

<<<<<<< HEAD
    company: extractCompany(cardName.value),
=======
    company: 'KB국민카드',
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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
<<<<<<< HEAD
=======
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

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
const goCardList = () => {

  showComplete.value = false;

  router.push('/cards');

};

</script>

<template>

<div class="page">


<<<<<<< HEAD
<PageHeader title="카드 등록" @back="router.back()"/>
=======
<PageHeader title="카드 등록"/>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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

<<<<<<< HEAD
<Icon name="camera" size="lg" />
=======
📷
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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

<<<<<<< HEAD
<Icon name="image" size="lg" />
=======
🖼️
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

<span>
사진 첨부
</span>

<p>
저장된 카드 이미지로 자동 인식합니다
</p>

</button>

<<<<<<< HEAD
=======

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

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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
<<<<<<< HEAD
=======
@blur="loadCardCandidates"
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
/>

</div>

<<<<<<< HEAD
=======
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

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

<<<<<<< HEAD
=======
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

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</section>
<!-- 카드 등록 버튼 -->
<button
  class="register-button"
<<<<<<< HEAD
=======
  :disabled="identifying || registering"
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

<<<<<<< HEAD
padding: var(--space-md);

margin: 0 auto;

max-width: 480px;

box-sizing: border-box;

overflow: hidden visible;
=======
padding:20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


.register-type,
.form{

<<<<<<< HEAD
margin-top: var(--space-xl);
=======
margin-top:24px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


h3{

<<<<<<< HEAD
font-size: var(--font-md);

margin-bottom: var(--space-sm);
color: var(--color-text-primary);
font-weight: var(--font-semibold);
=======
font-size:16px;

margin-bottom:14px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.type-list{

display:flex;

<<<<<<< HEAD
gap: var(--space-sm);
=======
gap:12px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.type-card{

flex:1;

<<<<<<< HEAD
padding: var(--space-md) var(--space-xs);

border-radius: var(--radius-md);

border: 1px solid var(--color-input-border);

background: var(--color-surface);
color: var(--color-text-primary);
cursor: pointer;
=======
padding:20px 10px;

border-radius:16px;

border:1px solid #ddd;

background:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.type-card.active{

<<<<<<< HEAD
border: 2px solid var(--color-primary);
=======
border:2px solid #4F46E5;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.type-card span{

display:block;

<<<<<<< HEAD
font-weight: var(--font-bold);

margin-top: var(--space-xs);
=======
font-weight:700;

margin-top:8px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.type-card p{

<<<<<<< HEAD
font-size: var(--font-xs);

color: var(--color-text-tertiary);
=======
font-size:12px;

color:#888;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.input-box{

<<<<<<< HEAD
margin-top: var(--space-md);
=======
margin-top:16px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

display:flex;

flex-direction:column;

<<<<<<< HEAD
gap: var(--space-xs);
=======
gap:8px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.input-box label{

<<<<<<< HEAD
font-size: var(--font-sm);

color: var(--color-text-secondary);
font-weight: var(--font-medium);
=======
font-size:14px;

color:#555;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.input-box input{

height:48px;

<<<<<<< HEAD
border-radius: var(--radius-sm);

border: 1px solid var(--color-input-border);

padding: 0 var(--space-sm);

font-size: var(--font-sm);
color: var(--color-text-primary);

background: var(--color-surface);
=======
border-radius:12px;

border:1px solid #ddd;

padding:0 14px;

font-size:15px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

 

.register-button{

width:100%;

height:52px;

<<<<<<< HEAD
margin-top: var(--space-2xl);

border:none;

border-radius: var(--radius-md);

background: var(--color-primary);

color: var(--color-btn-primary-text);

font-size: var(--font-md);
font-weight: var(--font-semibold);
cursor: pointer;
=======
margin-top:30px;

border:none;

border-radius:14px;

background:#4F46E5;

color:white;

font-size:16px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}

</style>
<!-- 07_25 연동 변경: 카드번호 확인과 보유카드 등록 API를 기존 등록 UI에 연결한다. -->
