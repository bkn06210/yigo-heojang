<!-- src/components/transaction/TransactionDatePickerBottomSheet.vue -->

<script setup>
import { ref, computed } from 'vue';
import Icon from '@/components/common/Icon.vue';


const emit = defineEmits([
  'close',
  'apply',
]);



// 현재 보고 있는 달
const currentDate = ref(new Date());

// 월 선택 모드
const showMonthPicker = ref(false);

// 선택 날짜
const startDate = ref('');

const endDate = ref('');




// 월 변경
const changeMonth = (value) => {


  currentDate.value = new Date(

    currentDate.value.getFullYear(),

    currentDate.value.getMonth() + value,

    1

  );


};



// 월 선택

const selectMonth = (month) => {

  const year = currentDate.value.getFullYear();

  currentDate.value = new Date(year, month, 1);

  showMonthPicker.value = false;

};



// 캘린더 날짜 생성

const calendarDays = computed(() => {


  const year = currentDate.value.getFullYear();

  const month = currentDate.value.getMonth();



  const firstDay = new Date(
    year,
    month,
    1
  ).getDay();



  const lastDate = new Date(
    year,
    month + 1,
    0
  ).getDate();



  const days = [];



  // 앞 빈칸

  for(let i = 0; i < firstDay; i++) {

    days.push(null);

  }



  // 날짜

  for(let i = 1; i <= lastDate; i++) {


    days.push(

      formatDate(
        new Date(
          year,
          month,
          i
        )

      )

    );


  }



  return days;


});




// 날짜 포맷

const formatDate = (date) => {


  return (

    `${date.getFullYear()}.` +

    `${String(date.getMonth()+1).padStart(2,'0')}.` +

    `${String(date.getDate()).padStart(2,'0')}`

  );


};




// 날짜 선택

const selectDate = (date) => {


  if(!date) return;



  // 처음 선택

  if(!startDate.value) {


    startDate.value = date;


    return;


  }




  // 종료일 선택

  if(!endDate.value) {


    if(new Date(date) < new Date(startDate.value)) {


      endDate.value = startDate.value;

      startDate.value = date;


    } else {


      endDate.value = date;


    }


    return;


  }




  // 다시 시작

  startDate.value = date;

  endDate.value = '';



};




// 선택 상태

const isSelected = (date) => {


  return (

    date === startDate.value ||

    date === endDate.value

  );


};



// 범위 내 상태 (시작일과 종료일 사이)

const isBetween = (date) => {

  if (!startDate.value || !endDate.value || !date) return false;

  const dateObj = new Date(date.replace(/\./g, '-'));

  const start = new Date(startDate.value.replace(/\./g, '-'));

  const end = new Date(endDate.value.replace(/\./g, '-'));

  return dateObj > start && dateObj < end;

};



// 범위의 시작 날짜 (범위가 있을 때만)

const isFirstInRange = (date) => {

  if (!startDate.value || !endDate.value || !date) return false;

  return date === startDate.value;

};



// 범위의 마지막 날짜 (범위가 있을 때만)

const isLastInRange = (date) => {

  if (!startDate.value || !endDate.value || !date) return false;

  return date === endDate.value;

};




// 적용

const apply = () => {

  let applyStartDate = startDate.value;

  let applyEndDate = endDate.value;


  // 날짜 선택이 없으면 현재 월 전체 사용

  if(!applyStartDate || !applyEndDate) {

    const year = currentDate.value.getFullYear();

    const month = currentDate.value.getMonth();

    const firstDay = new Date(year, month, 1);

    const lastDay = new Date(year, month + 1, 0);

    applyStartDate = formatDate(firstDay);

    applyEndDate = formatDate(lastDay);

  }


  emit(

    'apply',

    {

      startDate:applyStartDate,

      endDate:applyEndDate,

    }

  );


};




// 닫기

const close = () => {


  emit('close');


};


</script>

<template>

<div class="overlay" @click.self="close">

<section class="sheet">

<div class="handle"></div>

<div class="title-area">
<h2>날짜 선택</h2>
<button @click="close">
<Icon name="close" size="sm" />
</button>
</div>

<div class="month-nav">
<button @click="changeMonth(-1)">‹</button>
<button @click="showMonthPicker = true">{{currentDate.getFullYear()}}년 {{currentDate.getMonth()+1}}월</button>
<button @click="changeMonth(1)">›</button>
</div>

<div class="selected">

<div>

<p>
시작일
</p>


<strong>

{{startDate || '-'}}

</strong>


</div>




<div>

<p>
종료일
</p>


<strong>

{{endDate || '-'}}

</strong>


</div>



</div>




<div v-if="showMonthPicker" class="month-picker">

<div class="month-grid">

<button v-for="month in 12" :key="month" @click="selectMonth(month-1)" class="month-btn">

{{month}}월

</button>

</div>

<button class="month-close-btn" @click="showMonthPicker = false">

닫기

</button>

</div>

<div v-else>

<div class="weekdays">
<div>일</div>
<div>월</div>
<div>화</div>
<div>수</div>
<div>목</div>
<div>금</div>
<div>토</div>
</div>

<div class="calendar">

<button

v-for="(day,index) in calendarDays"

:key="index"

:disabled="!day"

:class="{
active:isSelected(day),
between:isBetween(day),
'range-start':isFirstInRange(day),
'range-end':isLastInRange(day)
}"

@click="selectDate(day)"

>

<span>{{day?.split('.')[2]}}</span>

</button>

</div>

</div>



<button class="apply" @click="apply">

적용

</button>



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
z-index:1200;
}

.sheet {
width:100%;
max-height:85vh;
overflow-y:auto;
background:var(--color-surface);
border-radius:24px 24px 0 0;
padding:20px;
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
margin-bottom:20px;
}

.title-area h2 {
margin:0;
font-size:var(--font-lg);
font-weight:var(--font-bold);
}

.title-area button {
border:none;
background:none;
cursor:pointer;
font-size:20px;
color:var(--color-text-primary);
}

.month-nav {
display:flex;
justify-content:space-between;
align-items:center;
gap:12px;
margin-bottom:20px;
}

.month-nav button {
width:40px;
height:40px;
border:1px solid var(--color-border);
border-radius:8px;
background:var(--color-surface);
cursor:pointer;
font-size:16px;
color:var(--color-text-primary);
}

.month-nav button:nth-child(2) {
flex:1;
border:none;
background:none;
font-weight:var(--font-semibold);
cursor:pointer;
color:var(--color-text-primary);
}

.selected {
display:grid;
grid-template-columns:1fr 1fr;
gap:0;
margin:24px 0;
}

.selected > div {
display:flex;
flex-direction:column;
align-items:center;
justify-content:center;
}

.selected p {
font-size:13px;
color:var(--color-text-secondary);
margin:0;
}

.selected strong {
font-size:16px;
margin:4px 0 0 0;
color:var(--color-text-primary);
}

.weekdays {
display:grid;
grid-template-columns:repeat(7,1fr);
gap:0;
margin-bottom:12px;
}

.weekdays div {
text-align:center;
font-size:12px;
font-weight:var(--font-semibold);
color:var(--color-text-secondary);
padding:8px 0;
}

.calendar {
display:grid;
grid-template-columns:repeat(7,1fr);
gap:0;
}

.calendar button {
height:42px;
width:100%;
border:none;
background:var(--color-surface);
cursor:pointer;
display:flex;
align-items:center;
justify-content:center;
border-radius:0;
padding:0;
box-sizing:border-box;
font-size:14px;
color:var(--color-text-primary);
}

.calendar button:disabled {
cursor:not-allowed;
color:var(--color-text-tertiary);
}

.calendar button.active {
background:rgba(255, 214, 0, 0.2);
color:var(--color-text-primary);
border-radius:0;
position:relative;
z-index:2;
}

.calendar button.active::before {
content:'';
position:absolute;
width:36px;
height:36px;
border-radius:50%;
background:var(--color-primary);
top:50%;
left:50%;
transform:translate(-50%, -50%);
z-index:0;
}

.calendar button span {
color:var(--color-text-primary);
}

.calendar button.active span {
position:relative;
z-index:1;
color:var(--color-btn-primary-text);
font-weight:var(--font-semibold);
}

.calendar button.between {
background:rgba(255, 214, 0, 0.2) !important;
border-radius:0 !important;
color:var(--color-text-primary);
height:36px !important;
padding:0 !important;
margin:3px 0 !important;
line-height:normal !important;
}

.calendar button.between.range-start {
border-radius:8px 0 0 8px;
}

.calendar button.between.range-end {
border-radius:0 8px 8px 0;
}

.month-picker {
padding:20px 0;
border-top:1px solid var(--color-border);
border-bottom:1px solid var(--color-border);
}

.month-grid {
display:grid;
grid-template-columns:repeat(4,1fr);
gap:8px;
margin:20px 0;
}

.month-btn {
padding:10px;
border:1px solid var(--color-border);
border-radius:8px;
cursor:pointer;
background:var(--color-surface);
color:var(--color-text-primary);
}

.month-close-btn {
width:100%;
padding:10px;
border:none;
background:var(--color-border);
color:var(--color-text-primary);
border-radius:8px;
cursor:pointer;
}

.apply {
width:100%;
height:50px;
margin-top:24px;
border:none;
border-radius:12px;
background:linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));
color:var(--color-btn-primary-text);
font-weight:var(--font-semibold);
cursor:pointer;
}

</style>
