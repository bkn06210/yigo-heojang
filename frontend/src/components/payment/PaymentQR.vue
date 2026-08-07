<script setup>
defineProps({
  image: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})
</script>

<template>

<div class="qr-box">

<div class="qr">

<img v-if="image" :src="image" alt="결제 QR 코드" />
<span v-else>{{ loading ? 'QR 생성 중...' : 'QR' }}</span>

</div>


</div>

</template>


<style scoped>

.qr-box{

display:flex;
justify-content:center;
margin:30px;

}


.qr{

width:220px;
height:220px;

display:flex;
align-items:center;
justify-content:center;

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
