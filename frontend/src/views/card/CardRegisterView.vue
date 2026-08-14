<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useCardStore } from '@/stores/cardStore';
import { registerUserCard } from '@/api/walletApi';
import { useToast } from '@/composables/useToast';

import PageHeader from '@/components/common/PageHeader.vue';

import CardScanModal from '@/components/card/CardScanModal.vue';
import CardUploadModal from '@/components/card/CardUploadModal.vue';
import CardRegisterCompleteModal from '@/components/card/CardRegisterCompleteModal.vue';
import Icon from '@/components/common/Icon.vue';

const cardStore = useCardStore();
const { showToast } = useToast();

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
// 카드명·카드사·카드 종류는 사용자가 고르지 않는다. 카드번호를 보내면 서버가
// 등록 가능한 번호인지 확인하고 어떤 카드 상품인지 결정해서 응답으로 알려준다.
// 아래 기본값은 시연용 번호(93_seed_mock_card.sql)라 그대로 등록해볼 수 있다.
// 시드 계정이 이미 보유한 카드를 넣으면 바로 "이미 등록된 카드"가 되므로,
// 92_seed_user_data.sql이 등록하지 않은 카드(ALL 카드)의 번호를 골랐다.
const cardNumber = ref('2228-7900-0000-0024');
const expiryDate = ref('12/25');
const cvc = ref('123');
const password = ref('12');

// 등록 요청 진행 상태 — 중복 제출을 막는다.
const registering = ref(false);

// 서버가 매칭한 카드 상품. 등록 완료 모달에 보여준다.
const registeredCard = ref(null);

// 카드번호는 하이픈 포함 19자(숫자 16자)여야 보낼 수 있다.
const canRegister = computed(
  () => cardNumber.value.replace(/-/g, '').length === 16 && !registering.value
);


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


// 인식 결과 반영
// 카드명은 서버가 카드번호로 결정하므로 인식 결과에서 받아도 쓰지 않는다.
const completeScan = (data) => {

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


// 서버 응답에서 사람이 읽을 메시지를 뽑는다.
const resolveErrorMessage = (error, fallback) =>
  error?.response?.data?.message || error?.message || fallback;


const registerCard = async () => {

  if (!canRegister.value) {

    showToast('warning', '카드번호 16자리를 모두 입력해주세요.');

    return;

  }

  registering.value = true;

  try {

    // 카드번호만 보낸다. 카드 상품은 서버가 정한다.
    const response = await registerUserCard(
      cardNumber.value.replace(/-/g, '')
    );

    registeredCard.value = response;

    await cardStore.loadCards();

    showComplete.value = true;

  } catch (error) {

    const code = error?.response?.data?.code;

    // 등록을 막은 이유를 사용자에게 알려준다.
    // 예전에는 콘솔에만 찍혀서 화면상 아무 반응이 없었다.
    if (code === 'CARD_NOT_SUPPORTED') {

      showToast('error', '등록을 지원하지 않는 카드번호입니다. 카드번호를 다시 확인해주세요.');

    } else if (code === 'USER_CARD_ALREADY_EXISTS') {

      showToast('warning', '이미 등록된 카드입니다.');

    } else {

      showToast('error', resolveErrorMessage(error, '카드 등록에 실패했습니다.'));

    }

  } finally {

    registering.value = false;

  }

};


// 카드 목록 이동
const goCardList = () => {

  showComplete.value = false;

  router.push('/cards');

};

</script>

<template>

<div class="page">


<PageHeader title="카드 등록" @back="router.back()"/>

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

<Icon name="camera" size="lg" />

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

<Icon name="image" size="lg" />

<span>
사진 첨부
</span>

<p>
저장된 카드 이미지로 자동 인식합니다
</p>

</button>

</div>

</section>

<section class="form">

<h3>
카드 정보 입력
</h3>

<!-- 카드명·카드사는 입력받지 않는다. 서버가 카드번호로 판별해 알려준다. -->
<p class="form-hint">
카드번호를 입력하면 카드사와 카드 종류가 자동으로 확인됩니다.
</p>


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

</section>
<!-- 카드 등록 버튼 -->
<button
  class="register-button"
  :disabled="!canRegister"
  @click="registerCard"
>
  {{ registering ? '등록 중...' : '카드 등록' }}
</button>


<!-- 카드 촬영 모달 -->
<CardScanModal
  v-if="showScan"
  @close="showScan = false"
  @complete="completeScan"
/>

<CardRegisterCompleteModal
  v-if="showComplete"
  :card="registeredCard"
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

padding: var(--space-md);

margin: 0 auto;

max-width: 480px;

box-sizing: border-box;

overflow: hidden visible;

}


.register-type,
.form{

margin-top: var(--space-xl);

}


h3{

font-size: var(--font-md);

margin-bottom: var(--space-sm);
color: var(--color-text-primary);
font-weight: var(--font-semibold);

}



.type-list{

display:flex;

gap: var(--space-sm);

}



.type-card{

flex:1;

padding: var(--space-md) var(--space-xs);

border-radius: var(--radius-md);

border: 1px solid var(--color-input-border);

background: var(--color-surface);
color: var(--color-text-primary);
cursor: pointer;

}



.type-card.active{

border: 2px solid var(--color-primary);

}



.type-card span{

display:block;

font-weight: var(--font-bold);

margin-top: var(--space-xs);

}



.type-card p{

font-size: var(--font-xs);

color: var(--color-text-tertiary);

}



.input-box{

margin-top: var(--space-md);

display:flex;

flex-direction:column;

gap: var(--space-xs);

}



.input-box label{

font-size: var(--font-sm);

color: var(--color-text-secondary);
font-weight: var(--font-medium);

}



.input-box input{

height:48px;

border-radius: var(--radius-sm);

border: 1px solid var(--color-input-border);

padding: 0 var(--space-sm);

font-size: var(--font-sm);
color: var(--color-text-primary);

background: var(--color-surface);

}

 

.register-button{

width:100%;

height:52px;

margin-top: var(--space-2xl);

border:none;

border-radius: var(--radius-md);

background: var(--color-primary);

color: var(--color-btn-primary-text);

font-size: var(--font-md);
font-weight: var(--font-semibold);
cursor: pointer;

}


.register-button:disabled{

background: var(--color-btn-disabled-bg);

color: var(--color-btn-disabled-text);

cursor: not-allowed;

}


.form-hint{

margin: 0 0 var(--space-md);

font-size: var(--font-xs);

color: var(--color-text-secondary);

line-height: 1.5;

}

</style>
