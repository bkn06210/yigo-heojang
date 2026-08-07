<!-- src/components/transaction/TransactionFilterBottomSheet.vue -->

<script setup>
<<<<<<< HEAD
import { ref, watch } from 'vue';
import Icon from '@/components/common/Icon.vue';
=======
import { onMounted, ref, watch } from 'vue';
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


// 부모 전달값
const props = defineProps({

  cards: {

    type: Array,

    default: () => [],

  },

<<<<<<< HEAD
=======
  categories: {
    type: Array,
    default: () => [],
  },

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  


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

<<<<<<< HEAD
  approval: '승인',
=======
  approval: '전체',
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  cardType: '전체',

  card: '전체',

<<<<<<< HEAD
=======
  userCardId: null,

  categoryId: null,

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  region: '전체',

  transactionType: '전체',

<<<<<<< HEAD
  period: '이번달',
=======
  period: '전체',
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  startDate: '',

  endDate: '',

});




// 부모에서 날짜 변경되면 반영
watch(

  () => props.selectedDate,

  (value) => {

    filter.value.startDate = value.startDate;

    filter.value.endDate = value.endDate;

<<<<<<< HEAD
  },

  {
    deep:true
=======
    if (value.startDate || value.endDate) {
      filter.value.period = '직접 선택';
    }

  },

  {
    deep:true,
    immediate:true,
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

<<<<<<< HEAD
=======
  '전체',

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  '이번달',

  '1개월',

  '3개월',

<<<<<<< HEAD
=======
  '월별 선택',

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
  '직접 선택',

];







// 카드 선택

const selectCard = (card) => {


  filter.value.card = card.name;

<<<<<<< HEAD
=======
  filter.value.userCardId = card.id;

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

  filter.value.cardType = card.type;


  showCardList.value = false;


};







<<<<<<< HEAD
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
=======
// 기간 선택

const selectPeriod = (item) => {


  filter.value.period = item;


  const today = new Date();
  const formatDate = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  if (item === '전체') {
    filter.value.startDate = '';
    filter.value.endDate = '';
  } else if (item === '이번달') {
    filter.value.startDate = formatDate(new Date(today.getFullYear(), today.getMonth(), 1));
    filter.value.endDate = formatDate(today);
  } else if (item === '1개월' || item === '3개월') {
    const months = item === '1개월' ? 1 : 3;
    const start = new Date(today);
    start.setMonth(start.getMonth() - months);
    filter.value.startDate = formatDate(start);
    filter.value.endDate = formatDate(today);
  }



  if(item === '직접 선택' || item === '월별 선택') {


    emit(
      'open-date-picker'
    );


  }


>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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


<<<<<<< HEAD
    approval:'승인',
=======
    approval:'전체',
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

    cardType:'전체',

    card:'전체',

<<<<<<< HEAD
=======
    userCardId:null,

    categoryId:null,

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
    region:'전체',

    transactionType:'전체',

<<<<<<< HEAD
    period:'이번달',
=======
    period:'전체',
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

    startDate:'',

    endDate:'',


  };

<<<<<<< HEAD

};

=======
  selectPeriod('전체');


};

onMounted(() => {
  if (!props.selectedDate.startDate && !props.selectedDate.endDate) {
    selectPeriod('전체');
  }
});

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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
<<<<<<< HEAD
<Icon name="close" size="sm" />
=======
✕
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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
<<<<<<< HEAD
=======
active:filter.approval==='전체'
}"

@click="filter.approval='전체'"

>
전체
</button>


<button

:class="{
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

<<<<<<< HEAD
=======
<div class="filter-item">
  <h3>소비 카테고리</h3>
  <div class="chips wrap">
    <button
      :class="{ active: filter.categoryId === null }"
      @click="filter.categoryId = null"
    >전체</button>
    <button
      v-for="category in props.categories"
      :key="category.categoryId"
      :class="{ active: filter.categoryId === category.categoryId }"
      @click="filter.categoryId = category.categoryId"
    >{{ category.categoryName }}</button>
  </div>
</div>

<!-- 지역 -->

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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

<<<<<<< HEAD
background:var(--color-surface);
=======
background:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

border-radius:24px 24px 0 0;

padding:20px;

}



.handle {

width:40px;

height:5px;

<<<<<<< HEAD
background:var(--color-border);
=======
background:#ddd;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

border-radius:10px;

margin:0 auto 20px;

}



.title-area {

display:flex;

justify-content:space-between;

align-items:center;

}

<<<<<<< HEAD
.title-area h2 {

color:var(--color-text-primary);

margin:0;

}
=======

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

.title-area button {

border:none;

background:none;

font-size:20px;

<<<<<<< HEAD
color:var(--color-text-primary);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}



.filter-item {

margin-top:24px;

}



.filter-item h3 {

<<<<<<< HEAD
font-size: var(--font-md);

font-weight: var(--font-semibold);

margin-bottom:12px;

color:var(--color-text-primary);

=======
font-size:15px;

margin-bottom:12px;

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

<<<<<<< HEAD
border:1px solid var(--color-border);

background:var(--color-surface);

color:var(--color-text-primary);
=======
border:1px solid #ddd;

background:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.chips button.active {

<<<<<<< HEAD
border:2px solid var(--color-primary);

background:var(--color-primary);

color:var(--color-btn-primary-text);
=======
border:2px solid #4F46E5;

color:#4F46E5;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.select-button {

width:100%;

height:48px;

padding:0 16px;

display:flex;

justify-content:space-between;

align-items:center;

<<<<<<< HEAD
border:1px solid var(--color-border);

border-radius:12px;

background:var(--color-surface);

color:var(--color-text-primary);
=======
border:1px solid #ddd;

border-radius:12px;

background:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.card-list {

margin-top:12px;

<<<<<<< HEAD
border:1px solid var(--color-border);
=======
border:1px solid #ddd;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

border-radius:16px;

overflow:hidden;

}



.card-item {

width:100%;

display:flex;

gap:14px;

align-items:center;

padding:14px;

<<<<<<< HEAD
background:var(--color-surface);

border:none;

border-bottom:1px solid var(--color-border);

color:var(--color-text-primary);
=======
background:white;

border:none;

border-bottom:1px solid #eee;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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

<<<<<<< HEAD
color:var(--color-text-secondary);
=======
color:#777;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.date-box {

margin-top:12px;

padding:14px;

<<<<<<< HEAD
background:var(--color-bg);
=======
background:#f7f7f7;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

border-radius:12px;

text-align:center;

<<<<<<< HEAD
color:var(--color-text-primary);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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

<<<<<<< HEAD
border:1px solid var(--color-border);

background:var(--color-surface);

color:var(--color-text-primary);
=======
border:1px solid #ddd;

background:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.apply {

flex:2;

border:none;

<<<<<<< HEAD
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
=======
background:#4F46E5;

color:white;

}

</style>
<!-- 07_25 연동 변경: 조회조건을 소비내역 API 쿼리 파라미터로 전달한다. -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
