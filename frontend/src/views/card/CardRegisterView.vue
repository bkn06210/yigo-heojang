<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useCardStore } from '@/stores/cardStore';

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
const registerCard = () => {

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
/>

</div>

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
