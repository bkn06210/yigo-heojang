<script setup>
import { ref } from 'vue';
import Icon from '@/components/common/Icon.vue';

const emit = defineEmits([
  'close',
  'complete',
]);


// 선택한 이미지
const selectedImage = ref(null);


// 분석 상태
const isScanning = ref(false);


// 결과 표시
const showResult = ref(false);


// 분석 단계
const scanStep = ref('');


// 임시 OCR 결과
// 실제 서비스에서는 OCR API 결과
// 카드번호는 93_seed_mock_card.sql에 실제로 등록 가능하게 심어둔 번호여야
// "등록 정보 사용" → "카드 등록"까지 눌렀을 때 CARD_NOT_SUPPORTED 없이 시연이 끝까지 성공한다.
// CardRegisterView 수동입력 기본값(...0024), CardScanModal(...0016)과 겹치지 않게
// 같은 국민카드 그룹의 다른 번호(...0032)를 사용한다 — cardName 표시와 카드사가 어긋나지 않도록.
const scanResult = ref({
  cardName: 'KB My WE:SH 카드',
  cardNumber: '2228790000000032',
  expiryDate: '12/28',
});


// 카드번호 표시용
const formatCardNumber = (number) => {

  return number
    .replace(/[^0-9]/g, '')
    .replace(/(\d{4})(?=\d)/g, '$1-');

};


// 닫기
const close = () => {

  emit('close');

};



// 이미지 선택
const selectImage = (event) => {

  const file = event.target.files[0];


  if(!file) return;


  selectedImage.value =
    URL.createObjectURL(file);


  startScan();

};



// 분석 시작
const startScan = () => {

  isScanning.value = true;


  scanStep.value =
    '카드 영역 분석 중...';



  setTimeout(() => {

    scanStep.value =
      '카드 정보 확인 중...';


  },800);



  setTimeout(() => {

    scanStep.value =
      '카드사 확인 완료';


  },1600);



  setTimeout(() => {

    isScanning.value = false;

    showResult.value = true;


  },2400);

};



// 등록 정보 전달
const complete = () => {

  emit('complete', {

    cardName:
      scanResult.value.cardName,


    cardNumber:
      scanResult.value.cardNumber,


    expiryDate:
      scanResult.value.expiryDate,

  });

};

</script>


<template>

<div class="overlay">


<section class="modal">


<div class="header">

<h2>
카드 사진 인식
</h2>


<button @click="close">
<Icon name="close" size="sm" />
</button>

</div>


<!-- 이미지 선택 -->

<div
v-if="!isScanning && !showResult"
class="upload-area"
>


<label class="upload-button">


<Icon name="image" size="sm" /> 카드 사진 선택


<input
type="file"
accept="image/*"
@change="selectImage"
/>


</label>


</div>

<!-- 분석 화면 -->

<div
v-if="isScanning"
class="scan-area"
>


<img
v-if="selectedImage"
:src="selectedImage"
class="preview"
/>



<div class="scan-line"></div>



<h3>
{{ scanStep }}
</h3>



<div class="loading">
● ● ●
</div>


</div>





<!-- 결과 -->

<div
v-if="showResult"
class="result"
>


<h3>
<Icon name="check" size="sm" /> 카드 정보 인식 완료
</h3>



<div class="info">


<p>
카드명

<strong>
{{ scanResult.cardName }}
</strong>

</p>



<p>
카드번호

<strong>
{{ formatCardNumber(scanResult.cardNumber) }}
</strong>

</p>



<p>
만료일

<strong>
{{ scanResult.expiryDate }}
</strong>

</p>


</div>



<button
class="complete-button"
@click="complete"
>

등록 정보 사용

</button>


</div>


</section>


</div>

</template>


<style scoped>

.overlay{

position:fixed;
inset:0;
width:100%;
max-width:480px;
left:50%;
transform:translateX(-50%);
margin:0 auto;

background:rgba(0,0,0,.4);

display:flex;
align-items:center;
justify-content:center;

z-index:2000;

}



.modal{

width:90%;
max-width:480px;

background:var(--color-surface);

border-radius:24px;

padding:24px;

box-sizing: border-box;

}



.header{

display:flex;

justify-content:space-between;

}



.header button{

border:none;

background:none;

font-size:20px;

color:var(--color-text-primary);

}



.upload-area{

margin-top:30px;

}



.upload-button{

height:160px;

border:2px dashed var(--color-border);

border-radius:20px;

display:flex;

align-items:center;

justify-content:center;

cursor:pointer;

}



.upload-button input{

display:none;

}



.scan-area{

margin-top:30px;

text-align:center;

position:relative;

}



.preview{

width:100%;

height:180px;

object-fit:cover;

border-radius:16px;

}



.scan-line{

position:absolute;

top:40px;

left:10%;

width:80%;

height:3px;

background:var(--color-border);

animation:scan 1.5s infinite;

}



@keyframes scan{

0%{
transform:translateY(0);
}

50%{
transform:translateY(120px);
}

100%{
transform:translateY(0);
}

}



.loading{

margin-top:20px;

letter-spacing:8px;

}



.info p{

display:flex;

justify-content:space-between;

padding:14px 0;

border-bottom:1px solid var(--color-border);

}



.complete-button{

width:100%;

height:48px;

margin-top:24px;

border:none;

border-radius:12px;

background:
  linear-gradient(
    90deg,
    var(--color-btn-primary-start),
    var(--color-btn-primary-end)
  );

color:var(--color-btn-primary-text);

}

</style>