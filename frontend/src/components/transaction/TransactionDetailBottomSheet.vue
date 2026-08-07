<!-- <script setup>
import { ref, computed } from 'vue';

const emit = defineEmits([
  'close',
  'apply',
]);


// 현재 보고 있는 년/월
const currentDate = ref(new Date(2026, 6, 1));


// 선택 날짜
const startDate = ref(null);
const endDate = ref(null);


// 요일
const weekDays = [
  '일',
  '월',
  '화',
  '수',
  '목',
  '금',
  '토',
];


// 달력 날짜 생성
const calendarDays = computed(() => {

  const year = currentDate.value.getFullYear();

  const month = currentDate.value.getMonth();


  const firstDay =
    new Date(year, month, 1).getDay();


  const lastDate =
    new Date(year, month + 1, 0).getDate();


  const days = [];


  // 앞 빈칸
  for(let i = 0; i < firstDay; i++) {

    days.push(null);

  }


  // 날짜
  for(let i = 1; i <= lastDate; i++) {

    days.push({
      day: i,
      date:
        `${year}.${String(month + 1).padStart(2,'0')}.${String(i).padStart(2,'0')}`,
    });

  }


  return days;

});


// 이전 달
const prevMonth = () => {

  currentDate.value =
    new Date(
      currentDate.value.getFullYear(),
      currentDate.value.getMonth() - 1,
      1
    );

};


// 다음 달
const nextMonth = () => {

  currentDate.value =
    new Date(
      currentDate.value.getFullYear(),
      currentDate.value.getMonth() + 1,
      1
    );

};


// 날짜 선택
const selectDate = (date) => {


  if(!startDate.value) {

    startDate.value = date;

    return;

  }


  if(!endDate.value) {

    endDate.value = date;

    return;

  }


  // 다시 선택하면 시작일 변경
  startDate.value = date;

  endDate.value = null;

};



// 선택 상태
const isSelected = (date) => {

  return (
    startDate.value === date ||
    endDate.value === date
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
      startDate: startDate.value,
      endDate: endDate.value,
    }
  );


};


// 닫기
const close = () => {

  emit('close');

};

<<<<<<< HEAD
</script> -->


<!-- <template>
=======
</script>


<template>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

<div
  class="overlay"
  @click.self="close"
>


<section class="sheet">


<div class="handle"></div>



<div class="header">

<h2>
기간 선택
</h2>


<button @click="close">
✕
</button>


</div>




<div class="selected-date">


<div>

<span>
시작일
</span>

<strong>
{{ startDate ?? '-' }}
</strong>

</div>



<div>

<span>
종료일
</span>

<strong>
{{ endDate ?? '-' }}
</strong>

</div>


</div>





<div class="month-header">


<button @click="prevMonth">
‹
</button>


<strong>
{{ currentDate.getFullYear() }}년
{{ currentDate.getMonth()+1 }}월
</strong>


<button @click="nextMonth">
›
</button>


</div>





<div class="calendar">


<div
  v-for="day in weekDays"
  :key="day"
  class="week"
>

{{ day }}

</div>



<button

v-for="(date,index) in calendarDays"

:key="index"

:disabled="!date"

:class="{
selected: date && isSelected(date.date)
}"

@click="date && selectDate(date.date)"

>

{{ date?.day }}

</button>


</div>





<button
class="apply-button"
@click="apply"
>

적용

</button>



</section>


</div>

<<<<<<< HEAD
</template> -->



<!-- <style scoped>
=======
</template>



<style scoped>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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



.header {

display:flex;

justify-content:space-between;

align-items:center;

}



.header button {

border:none;

background:none;

font-size:20px;

<<<<<<< HEAD
color:var(--color-text-primary);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}



.selected-date {

display:flex;

justify-content:space-around;

margin:24px 0;

}



.selected-date div {

text-align:center;

}



.selected-date span {

display:block;

font-size:13px;

<<<<<<< HEAD
color: var(--color-text-tertiary);
=======
color:#777;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.selected-date strong {

display:block;

margin-top:6px;

<<<<<<< HEAD
color:var(--color-text-primary);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}





.month-header {

display:flex;

justify-content:space-between;

align-items:center;

margin-bottom:15px;

}



.month-header button {

border:none;

background:none;

font-size:25px;

<<<<<<< HEAD
color:var(--color-text-primary);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}




.calendar {

display:grid;

grid-template-columns:repeat(7,1fr);

gap:8px;

}



.week {

text-align:center;

font-size:13px;

<<<<<<< HEAD
color: var(--color-text-tertiary);
=======
color:#777;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.calendar button {

height:40px;

border-radius:50%;

<<<<<<< HEAD
border:1px solid var(--color-border);

background:var(--color-surface);

color:var(--color-text-primary);
=======
border:1px solid #eee;

background:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.calendar button.selected {

<<<<<<< HEAD
background:
  linear-gradient(
    90deg,
    var(--color-btn-primary-start),
    var(--color-btn-primary-end)
  );

color:var(--color-btn-primary-text);
=======
background:#4F46E5;

color:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

border:none;

}




.apply-button {

width:100%;

height:50px;

margin-top:25px;

border:none;

border-radius:12px;

<<<<<<< HEAD
background:
  linear-gradient(
    90deg,
    var(--color-btn-primary-start),
    var(--color-btn-primary-end)
  );

color:var(--color-btn-primary-text);
=======
background:#4F46E5;

color:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

font-size:16px;

}

</style> -->

<script setup>
<<<<<<< HEAD
import Icon from '@/components/common/Icon.vue';
=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

const props = defineProps({

  transaction: {

    type: Object,

    required: true,

  },

});


const emit = defineEmits([
  'close',
]);


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

<h2>
<<<<<<< HEAD
상세 이용내역
=======
사용내역 상세
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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




<div class="content">


<<<<<<< HEAD
<div class="amount-box">

<span>결제 금액</span>

<strong class="amount">
{{ props.transaction.amount.toLocaleString() }}원
</strong>

</div>


<div class="row">

<span>
거래일
=======
<div class="row">

<span>
사용일자
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</span>

<strong>
{{ props.transaction.date }}
</strong>

</div>


<<<<<<< HEAD
<div class="row">

<span>
가맹점명
=======

<div class="row">

<span>
가맹점
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</span>

<strong>
{{ props.transaction.merchant }}
</strong>

</div>


<<<<<<< HEAD
<div class="row">

<span>
카테고리
</span>

<strong>
{{ props.transaction.category }}
=======


<div class="row">

<span>
사용 카드
</span>

<strong>
{{ props.transaction.cardName }}
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</strong>

</div>


<<<<<<< HEAD
<div class="row">

<span>
이용카드
</span>

<div class="card-info">
<strong>{{ props.transaction.cardName }}</strong>
<span class="card-number">본인 {{ props.transaction.cardLastDigits.slice(0, 3) }}*</span>
</div>

</div>


<div class="row">

<span>
거래구분
</span>

<strong>
{{ props.transaction.installment }}
</strong>

</div>


<div class="row">

<span>
승인번호
</span>

<strong>
{{ props.transaction.approvalNumber }}
</strong>

</div>


<div class="row">

<span>
거래상태
</span>

<strong>
{{ props.transaction.status }}
=======


<div class="row amount">

<span>
결제 금액
</span>

<strong>
-{{ props.transaction.amount.toLocaleString() }}원
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
</strong>

</div>


</div>




<button
class="close-button"
@click="close"
>
확인
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

z-index:1300;

}



.sheet {

<<<<<<< HEAD
width: 100%;

height: 80vh;

display: flex;

flex-direction: column;

background: var(--color-surface);

border-radius: 24px 24px 0 0;

padding: 20px;
=======
width:100%;

background:white;

border-radius:24px 24px 0 0;

padding:20px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

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



.header {

display:flex;

justify-content:space-between;

align-items:center;

}



.header button {

border:none;

background:none;

font-size:20px;

<<<<<<< HEAD
color:var(--color-text-primary);

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
}



.content {

<<<<<<< HEAD
margin-top: 24px;

flex: 1;

overflow-y: auto;

padding-right: 8px;
=======
margin-top:24px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


<<<<<<< HEAD
.amount-box {

background:var(--color-bg);

border-radius:12px;

padding:20px;

text-align:center;

margin-bottom:24px;

}


.amount-box span {

display:block;

font-size:14px;

color:var(--color-text-secondary);

margin-bottom:8px;

}


.amount-box strong {

display:block;

font-size:24px;

color:var(--color-coral);

font-weight:bold;

}

=======
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

.row {

display:flex;

justify-content:space-between;

padding:16px 0;

<<<<<<< HEAD
border-bottom:1px solid var(--color-border);
=======
border-bottom:1px solid #eee;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}



.row span {

<<<<<<< HEAD
color:var(--color-text-secondary);

}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: right;
}

.card-info strong {
  font-weight: bold;
  color: var(--color-text-primary);
}

.card-info .card-number {
  font-size: var(--font-sm);
  color: var(--color-text-secondary);
}

=======
color:#777;

}

>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e


.amount strong {

<<<<<<< HEAD
color:var(--color-coral);
=======
color:#e53935;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

font-size:18px;

}



.close-button {

width:100%;

height:50px;

margin-top:24px;

border:none;

border-radius:12px;

<<<<<<< HEAD
background:
  linear-gradient(
    90deg,
    var(--color-btn-primary-start),
    var(--color-btn-primary-end)
  );

color:var(--color-btn-primary-text);
=======
background:#4F46E5;

color:white;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


<<<<<<< HEAD
</style> -->
=======
</style>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
