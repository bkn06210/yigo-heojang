<script setup>
import { computed, ref } from 'vue';
import Icon from '@/components/common/Icon.vue';

const emit = defineEmits([
  'close',
  'apply',
]);

// 현재 보고 있는 달
const currentDate = ref(new Date());

// 선택 날짜
const startDate = ref(null);
const endDate = ref(null);

// 월 선택 표시
const showMonthPicker = ref(false);


// 현재 년/월
const currentYear = computed(() =>
  currentDate.value.getFullYear(),
);

const currentMonth = computed(() =>
  currentDate.value.getMonth() + 1,
);


// 날짜 포맷
const formatDate = (date) => {
  if (!date) return '';

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}.${month}.${day}`;
};


// 월 이동
const changeMonth = (amount) => {
  currentDate.value = new Date(
    currentYear.value,
    currentMonth.value - 1 + amount,
    1,
  );
};


// 월 선택
const selectMonth = (month) => {
  currentDate.value = new Date(
    currentYear.value,
    month - 1,
    1,
  );

  showMonthPicker.value = false;
};


// 달력 생성
const calendarDays = computed(() => {
  const year = currentYear.value;
  const month = currentMonth.value - 1;

  const firstDay = new Date(
    year,
    month,
    1,
  ).getDay();

  const lastDate = new Date(
    year,
    month + 1,
    0,
  ).getDate();


  const days = [];


  // 앞 빈칸
  for (let i = 0; i < firstDay; i++) {
    days.push(null);
  }


  // 날짜
  for (let i = 1; i <= lastDate; i++) {
    days.push(
      new Date(year, month, i),
    );
  }


  return days;
});


// 날짜 선택
const selectDate = (date) => {
  if (!date) return;


  // 시작일 선택
  if (!startDate.value || endDate.value) {

    startDate.value = date;
    endDate.value = null;

    return;
  }


  // 종료일 선택
  if (date < startDate.value) {

    endDate.value = startDate.value;
    startDate.value = date;

  } else {

    endDate.value = date;

  }
};


// 선택 날짜 표시
const isSelected = (date) => {
  if (!date) return false;


  return (
    startDate.value &&
    date.getTime() === startDate.value.getTime()
  )
  ||
  (
    endDate.value &&
    date.getTime() === endDate.value.getTime()
  );
};


// 범위 표시
const isBetween = (date) => {
  if (!startDate.value || !endDate.value || !date) {
    return false;
  }


  return (
    date > startDate.value &&
    date < endDate.value
  );
};


// 초기화
const reset = () => {
  console.log('초기화 클릭');

  startDate.value = null;
  endDate.value = null;
};

// 확인
const apply = () => {

  emit('apply', {
    startDate: formatDate(startDate.value),
    endDate: formatDate(endDate.value),
  });

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


<div class="title">

  <h2>
    기간 선택
  </h2>

  <button
    @click="close"
  >
    <Icon name="close" size="sm" />
    ✕
  </button>

</div>



<!-- 월 이동 -->
<div class="month-header">

<button
  @click="changeMonth(-1)"
>
  &lt;
</button>


<button
  class="month"
  @click="showMonthPicker = !showMonthPicker"
>
  {{ currentYear }}.{{ String(currentMonth).padStart(2,'0') }}
</button>


<button
  @click="changeMonth(1)"
>
  &gt;
</button>


</div>



<!-- 월 선택 -->
<div
  v-if="showMonthPicker"
  class="month-picker"
>

<button
  v-for="month in 12"
  :key="month"
  @click="selectMonth(month)"
>
  {{ month }}월
</button>

</div>




<!-- 요일 -->
<div class="week">

<span
  v-for="day in [
    '일',
    '월',
    '화',
    '수',
    '목',
    '금',
    '토'
  ]"
  :key="day"
>
  {{ day }}
</span>

</div>




<!-- 달력 -->
<div class="calendar">


<button
  v-for="(date,index) in calendarDays"
  :key="index"

  :class="{
    selected:isSelected(date),
    between:isBetween(date)
  }"

  @click="selectDate(date)"
>

{{ date?.getDate() }}

</button>


</div>




<!-- 선택 범위 -->
<div class="selected-date">

<span>
{{ formatDate(startDate) || '시작일 선택' }}
</span>


<span>
~
</span>


<span>
{{ formatDate(endDate) || '종료일 선택' }}
</span>


</div>




<!-- 하단 버튼 -->
<div class="footer">


<button
  class="reset"
  @click="reset"
>
초기화
</button>


<button
  class="confirm"
  @click="apply"
>
확인
</button>


</div>


</section>

</div>

</template>


<style scoped>

.overlay {
  position: fixed;
  inset: 0;

  background: rgba(0,0,0,.35);

  display:flex;
  align-items:flex-end;

  z-index:1000;
}


.sheet {

  width:100%;

  background:var(--color-surface);
  background:white;

  border-radius:24px 24px 0 0;

  padding:20px;

}



.handle {

  width:42px;
  height:5px;

  background:var(--color-border);
  background:#ddd;

  border-radius:99px;

  margin:0 auto;

}



.title {

  display:flex;

  justify-content:space-between;

  align-items:center;

  margin:20px 0;

  color:var(--color-text-primary);

}


.title button {

  border:none;

  background:none;

  font-size:20px;

  color:var(--color-text-primary);

}



.month-header {

  display:flex;

  justify-content:center;

  align-items:center;

  gap:30px;

  margin-bottom:20px;

}


.month-header button {

  border:none;

  background:none;

  font-size:20px;

  color:var(--color-text-primary);

  cursor:pointer;

}


.month {

  font-weight:bold;

  color:var(--color-text-primary);

}



.month-picker {

  display:grid;

  grid-template-columns:repeat(4,1fr);

  gap:10px;

  margin-bottom:20px;

}

.month-picker button {

  color:var(--color-text-primary);

  border:1px solid var(--color-border);

  background:var(--color-surface);

  padding:8px;

  border-radius:8px;

  cursor:pointer;

}



.week,
.calendar {

  display:grid;

  grid-template-columns:repeat(7,1fr);

  text-align:center;

}


.week span {

  font-size:13px;

  color:var(--color-text-secondary);
  color:#777;

}



.calendar button {

  height:40px;

  border:none;

  background:var(--color-surface);

  color:var(--color-text-primary);

  cursor:pointer;
  background:white;

}



.calendar .selected {

  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );

  color:var(--color-btn-primary-text);
  background:#4F46E5;

  color:white;

  border-radius:50%;

}


.calendar .between {

  background:var(--color-filter-active-bg);
  background:#EEF2FF;

}



.selected-date {

  display:flex;

  justify-content:center;

  gap:12px;

  margin:20px 0;

  color:var(--color-text-primary);

}



.footer {

  display:flex;

  gap:12px;

}



.footer button {

  flex:1;

  height:48px;

  border:none;

  border-radius:12px;

}



.reset {

  background:var(--color-border);

  color:var(--color-text-secondary);

  font-weight:var(--font-semibold);
  background:#eee;

}



.confirm {

  background:
    linear-gradient(
      90deg,
      var(--color-btn-primary-start),
      var(--color-btn-primary-end)
    );

  color:var(--color-btn-primary-text);

  font-weight:var(--font-semibold);
  background:#4F46E5;

  color:white;

}

</style>