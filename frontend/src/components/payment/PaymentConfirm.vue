<script setup>


defineProps({

card:Object,

amount:Number

});


</script>


<template>


<section class="confirm">


<h2>
결제 정보
</h2>


<div class="card">


<img
:src="card.image"
/>


<div>

<h3>
{{card.name}}
</h3>


<p>
결제 예정 금액
</p>


<strong>
{{amount.toLocaleString()}}원
</strong>


</div>


</div>


</section>


</template>


<style scoped>


.card{

display:flex;

gap:16px;

padding:20px;

border-radius:16px;

border:1px solid #ddd;

}


img{

width:80px;

height:120px;

}


</style>