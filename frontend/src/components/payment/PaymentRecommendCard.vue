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
<<<<<<< HEAD
=======
v-if="card.image"
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
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
<<<<<<< HEAD
gap:var(--space-md);
padding:var(--space-lg);
border:1px solid var(--color-border);
border-radius:var(--radius-lg);
background:var(--color-surface);
box-shadow:var(--shadow-card);
cursor:pointer;
transition:var(--transition-fast);

align-items:center;

overflow:hidden;
word-break:break-word;

box-sizing:border-box;

}

.card:hover{

transform:translateY(-2px);
box-shadow:0 4px 16px rgba(0,0,0,0.08);

}

.selected{

border:2px solid var(--color-primary);
=======
gap:16px;
padding:16px;
border:1px solid #ddd;
border-radius:16px;

}


.selected{

border:2px solid #333;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


img{

<<<<<<< HEAD
width:72px;
height:auto;
border-radius:var(--radius-sm);
=======
width:70px;
height:100px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


<<<<<<< HEAD
</style>
=======
</style>
<!-- 07_25 연동 변경: 추천 API가 반환한 카드·예상 혜택 정보를 표시한다. -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
