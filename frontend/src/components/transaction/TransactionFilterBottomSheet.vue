<!-- src/components/transaction/TransactionFilterBottomSheet.vue -->

<script setup>
import { ref, watch } from 'vue';
import Icon from '@/components/common/Icon.vue';


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
// category(이름)는 화면 표시용, categoryId는 서버 전달용이다. 이름만 들고 있으면
// 서버가 카테고리명을 바꿨을 때 필터가 조용히 깨진다.
const filter = ref({

  approval: '승인',

  cardType: '전체',

  card: '전체',

  cardId: null,

  category: '전체',

  categoryId: null,

  region: '전체',

  transactionType: '전체',

  period: '이번달',

  startDate: '',

  endDate: '',

});




// 소비 카테고리 목록 — 개인화 설정(관심 카테고리)과 같은 9개 축으로 맞춘다.
// 서버 마스터(대분류 7 + 중분류 31)를 그대로 늘어놓으면 조회조건이 화면 한 장을 넘기고,
// 개인화 화면과 기준이 달라 사용자가 같은 축으로 읽지 못한다.
//
// categoryId는 표시용이 아니라 실제 조회 조건이라 라벨이 가리키는 범위와 맞아야 한다.
// 쇼핑·문화/여가·교통·의료는 중분류를 묶는 개념이라 대분류 ID를 보내고,
// 서버가 하위 중분류 결제까지 매칭한다(TransactionMapper.selectTransactionList).
//
// 개인화 스토어(stores/personalization.js)는 "현장 결제 대표 중분류" 기준이라
// 같은 라벨이라도 ID가 다르다. 라벨만 공유하고 ID는 각자의 용도를 따른다.
const categories = [

  { categoryId: 102, categoryName: '카페' },

  { categoryId: 201, categoryName: '편의점' },

  { categoryId: 101, categoryName: '음식점' },

  { categoryId: 202, categoryName: '마트' },

  { categoryId: 2, categoryName: '쇼핑' },

  { categoryId: 205, categoryName: '뷰티' },

  { categoryId: 5, categoryName: '문화/여가' },

  { categoryId: 3, categoryName: '교통' },

  { categoryId: 6, categoryName: '의료' },

];


// 카테고리 선택 — "전체"는 categoryId를 비워 서버 조건에서 아예 빠지게 한다.
const selectCategory = (category) => {

  if (!category) {
    filter.value.category = '전체';
    filter.value.categoryId = null;
    return;
  }

  filter.value.category = category.categoryName;

  filter.value.categoryId = category.categoryId;

};




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

  '직접 선택',

];







// 카드 선택
// 카드를 고르면 카드종류뿐 아니라 그 카드로도 걸러야 한다.
// cardId를 안 넘기면 "신한 체크카드"를 골라도 체크카드 전부가 나온다.

const selectCard = (card) => {


  if (!card) {

    filter.value.card = '전체';

    filter.value.cardId = null;

    filter.value.cardType = '전체';

    showCardList.value = false;

    return;

  }


  filter.value.card = card.name;


  filter.value.cardId = card.id;


  filter.value.cardType = card.type;


  showCardList.value = false;


};







// 날짜 포맷팅
const formatDate = (date) => {
  return (
    `${date.getFullYear()}.` +
    `${String(date.getMonth()+1).padStart(2,'0')}.` +
    `${String(date.getDate()).padStart(2,'0')}`
  );
};

// 기간 선택
const selectPeriod = (item) => {
  filter.value.period = item;

  const today = new Date();
  let startDate = new Date(today);
  let endDate = new Date(today);

  if (item === '이번달') {
    startDate = new Date(today.getFullYear(), today.getMonth(), 1);
    endDate = new Date(today.getFullYear(), today.getMonth() + 1, 0);
  } else if (item === '1개월') {
    startDate = new Date(today);
    startDate.setDate(startDate.getDate() - 30);
    endDate = new Date(today);
  } else if (item === '3개월') {
    startDate = new Date(today);
    startDate.setDate(startDate.getDate() - 90);
    endDate = new Date(today);
  } else if (item === '월별 선택') {
    // 월별 선택은 나중에 구현
    return;
  } else if (item === '직접 선택') {
    emit('open-date-picker');
    return;
  }

  filter.value.startDate = formatDate(startDate);
  filter.value.endDate = formatDate(endDate);
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

    cardId:null,

    category:'전체',

    categoryId:null,

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
<Icon name="close" size="sm" />
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

class="card-item"

@click="selectCard(null)"

>

<div>

<strong>
전체
</strong>

</div>

</button>



<button

v-for="card in props.cards"

:key="card.id"

class="card-item"

@click="selectCard(card)"

>


<img

v-if="card.imageUrl"

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









<!-- 카테고리 -->


<div
class="filter-item"
>


<h3>
카테고리
</h3>



<div class="chips wrap">


<button

:class="{
active:filter.categoryId===null
}"

@click="selectCategory(null)"

>
전체
</button>



<button

v-for="category in categories"

:key="category.categoryId"

:class="{
active:filter.categoryId===category.categoryId
}"

@click="selectCategory(category)"

>

{{category.categoryName}}

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
width:100%;
max-width:480px;
left:50%;
transform:translateX(-50%);
margin:0 auto;

background:rgba(0,0,0,.35);

display:flex;

justify-content:center;

align-items:flex-end;

z-index:1100;

}



.filter-sheet {

width:100%;

max-width:480px;

max-height:85vh;

overflow-y:auto;

background:var(--color-surface);

border-radius:24px 24px 0 0;

padding:20px;

box-sizing: border-box;

}



.handle {

width:40px;

height:5px;

background:var(--color-border);

border-radius:10px;

margin:0 auto 20px;

}



.title-area {

display:flex;

justify-content:space-between;

align-items:center;

}

.title-area h2 {

color:var(--color-text-primary);

margin:0;

}

.title-area button {

border:none;

background:none;

font-size:20px;

color:var(--color-text-primary);

}



.filter-item {

margin-top:24px;

}



.filter-item h3 {

font-size: var(--font-md);

font-weight: var(--font-semibold);

margin-bottom:12px;

color:var(--color-text-primary);

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

border:1px solid var(--color-border);

background:var(--color-surface);

color:var(--color-text-primary);

}



.chips button.active {

border:2px solid var(--color-primary);

background:var(--color-primary);

color:var(--color-btn-primary-text);

}



.select-button {

width:100%;

height:48px;

padding:0 16px;

display:flex;

justify-content:space-between;

align-items:center;

border:1px solid var(--color-border);

border-radius:12px;

background:var(--color-surface);

color:var(--color-text-primary);

}



.card-list {

margin-top:12px;

border:1px solid var(--color-border);

border-radius:16px;

overflow:hidden;

}



.card-item {

width:100%;

display:flex;

gap:14px;

align-items:center;

padding:14px;

background:var(--color-surface);

border:none;

border-bottom:1px solid var(--color-border);

color:var(--color-text-primary);

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

color:var(--color-text-secondary);

}



.date-box {

margin-top:12px;

padding:14px;

background:var(--color-bg);

border-radius:12px;

text-align:center;

color:var(--color-text-primary);

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

border:1px solid var(--color-border);

background:var(--color-surface);

color:var(--color-text-primary);

}



.apply {

flex:2;

border:none;

background:
  linear-gradient(
    90deg,
    var(--color-btn-primary-start),
    var(--color-btn-primary-end)
  );

color:var(--color-btn-primary-text);

font-weight:var(--font-semibold);

}

</style>