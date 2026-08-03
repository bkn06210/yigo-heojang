<!-- src/components/transaction/TransactionDatePickerBottomSheet.vue -->

<script setup>
import { ref, computed } from 'vue';


const emit = defineEmits([
  'close',
  'apply',
]);



// 현재 보고 있는 달
const currentDate = ref(new Date());



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







// 적용

const apply = () => {


  if(!startDate.value || !endDate.value) {


    return;


  }




  emit(

    'apply',

    {

      startDate:startDate.value,

      endDate:endDate.value,

    }

  );


};






// 닫기

const close = () => {


  emit('close');


};


</script>





<template>


<div

class="overlay"

@click.self="close"

>


<section class="sheet">



<div class="handle"></div>





<div class="header">


<button
@click="changeMonth(-1)"
>
‹
</button>



<h2>

{{currentDate.getFullYear()}}

년

{{currentDate.getMonth()+1}}

월

</h2>



<button
@click="changeMonth(1)"
>
›
</button>



<button
@click="close"
>
✕
</button>



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








<div class="calendar">



<button

v-for="(day,index) in calendarDays"

:key="index"

:disabled="!day"

:class="{
active:isSelected(day)
}"

@click="selectDate(day)"

>


{{day?.split('.')[2]}}


</button>



</div>







<button

class="apply"

@click="apply"

>

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



.header {

display:flex;

align-items:center;

justify-content:space-between;

}



.header button {

border:none;

background:none;

font-size:22px;

}



.selected {

display:flex;

justify-content:space-around;

margin:24px 0;

}



.selected p {

font-size:13px;

color:#777;

}



.selected strong {

font-size:16px;

}



.calendar {

display:grid;

grid-template-columns:repeat(7,1fr);

gap:8px;

}



.calendar button {

height:42px;

border-radius:10px;

border:1px solid #ddd;

background:white;

}



.calendar button.active {

border:2px solid #4F46E5;

color:#4F46E5;

}



.apply {

width:100%;

height:50px;

margin-top:24px;

border:none;

border-radius:12px;

background:#4F46E5;

color:white;

}


</style>