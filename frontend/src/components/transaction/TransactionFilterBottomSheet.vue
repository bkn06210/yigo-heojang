<!-- src/components/transaction/TransactionFilterBottomSheet.vue -->

<script setup>
import { ref, watch } from 'vue';


// 부모 전달값
const props = defineProps({

  cards: {

    type: Array,

    default: () => [],

  },

  


  selectedDate: {

    type: Object,

    default: () => ({

      startDate: '',

      endDate: '',

    }),

  },

});


// 이벤트
const emit = defineEmits([
  'close',
  'apply',
  'open-date-picker',
]);



// 카드 목록 표시
const showCardList = ref(false);



// 조회 조건
const filter = ref({

  approval: '승인',

  cardType: '전체',

  card: '전체',

  region: '전체',

  transactionType: '전체',

  period: '이번달',

  startDate: '',

  endDate: '',

});




// 부모에서 날짜 변경되면 반영
watch(

  () => props.selectedDate,

  (value) => {

    filter.value.startDate = value.startDate;

    filter.value.endDate = value.endDate;

  },

  {
    deep:true
  }

);





const cardTypes = [

  '전체',

  '신용카드',

  '체크카드',

  '선불카드',

  '기프트카드',

];





const transactionTypes = [

  '전체',

  '일시불',

  '할부',

  '단기카드대출',

  '취소',

];





const periods = [

  '이번달',

  '1개월',

  '3개월',

  '월별 선택',

  '직접 선택',

];







// 카드 선택

const selectCard = (card) => {


  filter.value.card = card.name;


  filter.value.cardType = card.type;


  showCardList.value = false;


};







// 기간 선택

const selectPeriod = (item) => {


  filter.value.period = item;



  if(item === '직접 선택') {


    emit(
      'open-date-picker'
    );


  }


};








// 적용

const apply = () => {


  emit(

    'apply',

    filter.value

  );


};








// 닫기

const close = () => {


  emit('close');


};








// 초기화

const reset = () => {


  filter.value = {


    approval:'승인',

    cardType:'전체',

    card:'전체',

    region:'전체',

    transactionType:'전체',

    period:'이번달',

    startDate:'',

    endDate:'',


  };


};

</script>





<template>

<div
class="overlay"
@click.self="close"
>


<section class="filter-sheet">



<div class="handle"></div>




<div class="title-area">


<h2>
조회조건 선택
</h2>



<button
@click="close"
>
✕
</button>


</div>









<!-- 승인구분 -->


<div class="filter-item">


<h3>
승인구분
</h3>



<div class="chips">


<button

:class="{
active:filter.approval==='승인'
}"

@click="filter.approval='승인'"

>
승인
</button>



<button

:class="{
active:filter.approval==='결제확정'
}"

@click="filter.approval='결제확정'"

>
결제확정
</button>



</div>


</div>









<!-- 카드구분 -->


<div class="filter-item">


<h3>
카드구분
</h3>



<div class="chips wrap">


<button

v-for="type in cardTypes"

:key="type"

:class="{
active:filter.cardType===type
}"

@click="filter.cardType=type"

>

{{type}}

</button>


</div>


</div>









<!-- 카드 선택 -->


<div class="filter-item">


<h3>
카드 선택
</h3>



<button

class="select-button"

@click="showCardList=!showCardList"

>

<span>
{{filter.card}}
</span>


<span>
{{showCardList?'⌃':'›'}}
</span>


</button>





<div

v-if="showCardList"

class="card-list"

>


<button

v-for="card in props.cards"

:key="card.id"

class="card-item"

@click="selectCard(card)"

>


<img

:src="card.imageUrl"

:alt="card.name"

/>


<div>

<strong>
{{card.name}}
</strong>


<p>
{{card.type}}
</p>


</div>



</button>



</div>



</div>









<!-- 지역 -->


<div class="filter-item">


<h3>
지역
</h3>



<div class="chips">


<button

v-for="item in ['전체','국내','해외']"

:key="item"

:class="{
active:filter.region===item
}"

@click="filter.region=item"

>

{{item}}

</button>


</div>


</div>









<!-- 거래구분 -->


<div class="filter-item">


<h3>
거래구분
</h3>



<div class="chips wrap">


<button

v-for="item in transactionTypes"

:key="item"

:class="{
active:filter.transactionType===item
}"

@click="filter.transactionType=item"

>

{{item}}

</button>


</div>


</div>









<!-- 조회기간 -->


<div class="filter-item">


<h3>
조회기간
</h3>



<div class="chips wrap">


<button

v-for="item in periods"

:key="item"

:class="{
active:filter.period===item
}"

@click="selectPeriod(item)"

>

{{item}}

</button>


</div>





<div class="date-box">


{{filter.startDate || '시작일'}}

~

{{filter.endDate || '종료일'}}


</div>



</div>









<div class="footer-buttons">


<button

class="reset"

@click="reset"

>
초기화
</button>



<button

class="apply"

@click="apply"

>
적용
</button>


</div>



</section>


</div>


</template>






<style scoped>

.overlay {

position:fixed;

inset:0;

background:rgba(0,0,0,.35);

display:flex;

align-items:flex-end;

z-index:1100;

}



.filter-sheet {

width:100%;

max-height:85vh;

overflow-y:auto;

background:white;

border-radius:24px 24px 0 0;

padding:20px;

}



.handle {

width:40px;

height:5px;

background:#ddd;

border-radius:10px;

margin:0 auto 20px;

}



.title-area {

display:flex;

justify-content:space-between;

align-items:center;

}



.title-area button {

border:none;

background:none;

font-size:20px;

}



.filter-item {

margin-top:24px;

}



.filter-item h3 {

font-size:15px;

margin-bottom:12px;

}



.chips {

display:flex;

gap:8px;

}



.wrap {

flex-wrap:wrap;

}



.chips button {

padding:10px 14px;

border-radius:12px;

border:1px solid #ddd;

background:white;

}



.chips button.active {

border:2px solid #4F46E5;

color:#4F46E5;

}



.select-button {

width:100%;

height:48px;

padding:0 16px;

display:flex;

justify-content:space-between;

align-items:center;

border:1px solid #ddd;

border-radius:12px;

background:white;

}



.card-list {

margin-top:12px;

border:1px solid #ddd;

border-radius:16px;

overflow:hidden;

}



.card-item {

width:100%;

display:flex;

gap:14px;

align-items:center;

padding:14px;

background:white;

border:none;

border-bottom:1px solid #eee;

}



.card-item img {

width:60px;

height:38px;

object-fit:cover;

border-radius:8px;

}



.card-item p {

margin:4px 0 0;

font-size:13px;

color:#777;

}



.date-box {

margin-top:12px;

padding:14px;

background:#f7f7f7;

border-radius:12px;

text-align:center;

}



.footer-buttons {

display:flex;

gap:12px;

margin-top:30px;

}



.footer-buttons button {

height:50px;

border-radius:12px;

}



.reset {

flex:1;

border:1px solid #ddd;

background:white;

}



.apply {

flex:2;

border:none;

background:#4F46E5;

color:white;

}

</style>