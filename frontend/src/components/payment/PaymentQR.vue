<template>

<div class="qr-box">

<div class="qr">

QR

</div>


</div>

</template>


<style scoped>

.qr-box{

display:flex;
justify-content:center;
margin: var(--space-2xl) 0;

}


.qr{

width:220px;
height:220px;

display:flex;
align-items:center;
justify-content:center;

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