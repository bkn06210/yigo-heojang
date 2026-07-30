<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
title: String,
message: String,
confirmText: { type: String, default: '확인' },
cancelText: { type: String, default: '취소' }
});

const emit = defineEmits(['confirm', 'close']);
</script>

<template>
<div class="modal-overlay" @click.self="$emit('close')">
<div class="modal-content">
<h3>{{ props.title }}</h3>
<p>{{ props.message }}</p>
<div class="modal-buttons">
<button class="cancel-btn" @click="$emit('close')">{{ props.cancelText }}</button>
<button class="confirm-btn" @click="$emit('confirm')">{{ props.confirmText }}</button>
</div>
</div>
</div>
</template>

<style scoped>
.modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; }
.modal-content { background: white; padding: 20px; border-radius: 8px; width: 300px; text-align: center; }
.modal-buttons { margin-top: 20px; display: flex; justify-content: space-around; }
</style>