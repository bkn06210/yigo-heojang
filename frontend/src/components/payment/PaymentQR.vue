<<<<<<< HEAD
﻿<template>
=======
<script setup>
defineProps({
  image: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})
</script>

<template>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

<div class="qr-box">

<div class="qr">

<<<<<<< HEAD
QR
=======
<img v-if="image" :src="image" alt="결제 QR 코드" />
<span v-else>{{ loading ? 'QR 생성 중...' : 'QR' }}</span>
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

</div>


</div>

</template>


<style scoped>

.qr-box{

display:flex;
justify-content:center;
<<<<<<< HEAD
margin: var(--space-2xl) 0;
=======
margin:30px;
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e

}


.qr{

width:220px;
height:220px;

display:flex;
align-items:center;
justify-content:center;

<<<<<<< HEAD
border-radius: var(--radius-xl);

background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.1) 0%, rgba(var(--color-primary-dark-rgb), 0.03) 100%);
border: 1px solid rgba(var(--color-primary-dark-rgb), 0.2);
box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.4);
backdrop-filter: blur(10px);
-webkit-backdrop-filter: blur(10px);

font-size: var(--typo-display-large-size);
font-weight: var(--typo-display-large-weight);
color: var(--color-text-primary);

}

[data-theme="dark"] .qr{

background: linear-gradient(135deg, rgba(var(--color-primary-dark-rgb), 0.2) 0%, rgba(var(--color-primary-dark-rgb), 0.07) 100%);
border: 1px solid rgba(var(--color-primary-dark-rgb), 0.28);
box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.06);

}

</style>
=======
background:#eee;

font-size:40px;

}

.qr img{
width:100%;
height:100%;
object-fit:contain;
}

</style>
<!-- 07_25 연동 변경: QR 결제 API 응답과 만료 상태를 화면에 반영한다. -->
>>>>>>> 29557b87f11aa5ce9f2606e90780fd1b5f44382e
