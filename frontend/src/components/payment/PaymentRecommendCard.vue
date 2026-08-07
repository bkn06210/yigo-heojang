<script setup>

defineProps({

 card:Object,

 selected:Boolean

});


const emit = defineEmits([
  'select',
  'detail'
]);


</script>

<template>

<div
class="card"
:class="{selected}"
@click="emit('select',card)"
>


<img
v-if="card.image"
:src="card.image"
/>


<div>

<h3>
{{card.name}}
</h3>


<p>
{{card.benefit}}
</p>


<button
@click.stop="emit('detail', card)"
>
자세히 보기
</button>


</div>


</div>


</template>


<style scoped>

.card{

display:flex;
gap:16px;
padding:16px;
border:1px solid #ddd;
border-radius:16px;

}


.selected{

border:2px solid #333;

}


img{

width:70px;
height:100px;

}


</style>
<!-- 07_25 연동 변경: 추천 API가 반환한 카드·예상 혜택 정보를 표시한다. -->
