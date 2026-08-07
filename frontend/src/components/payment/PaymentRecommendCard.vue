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

}


img{

width:72px;
height:auto;
border-radius:var(--radius-sm);

}


</style>