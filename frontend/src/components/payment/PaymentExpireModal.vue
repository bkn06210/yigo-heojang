<script setup>

/*
| QR 만료 모달
| QR 유효시간(60초)이 끝났을 때 표시되는 모달
|
| emit
| - refresh : QR 다시 생성
| - cancel : 결제 취소
|
*/

const emit = defineEmits([
  'refresh',
  'cancel'
]);

</script>

<template>

  <div class="overlay">

    <div class="modal">

      <!-- 안내 아이콘 -->
      <div class="icon">
        ⏰
      </div>

      <h2>
        QR 코드가 만료되었습니다.
      </h2>

      <p class="description">
        보안을 위해 QR 코드는 1분 후 자동으로 만료됩니다.
        <br>
        QR을 다시 생성하여 결제를 진행해주세요.
      </p>

      <div class="button-group">

        <!-- QR 재생성 -->
        <button
          class="refresh-button"
          @click="emit('refresh')"
        >
          QR 다시 생성
        </button>

        <!-- 결제 취소 -->
        <button
          class="cancel-button"
          @click="emit('cancel')"
        >
          결제 취소
        </button>

      </div>

    </div>

  </div>

</template>

<style scoped>

.overlay{

  position:fixed;
  inset:0;
  width:100%;
  max-width:480px;
  left:50%;
  transform:translateX(-50%);
  margin:0 auto;

  background:rgba(0,0,0,.45);

  display:flex;
  justify-content:center;
  align-items:center;

  z-index:1000;

}

.modal{

  width:90%;
  max-width:480px;

  border-radius: var(--radius-xl);

  padding: var(--space-xl);

  text-align:center;

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);

  box-sizing: border-box;

}

[data-theme="dark"] .modal{

  background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, var(--color-surface) 60%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.05);

}

.modal h2{

  font-size: var(--font-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;

}

.icon{

  font-size:42px;

  margin-bottom: var(--space-md);

}

.description{

  margin: var(--space-lg) 0;

  line-height:1.6;

  font-size: var(--font-sm);

  color: var(--color-text-secondary);

}

.button-group{

  display:flex;

  flex-direction:column;

  gap: var(--space-sm);

}

.refresh-button{

  height:48px;

  border:none;

  border-radius: var(--radius-md);

  background: linear-gradient(90deg, var(--color-btn-primary-start), var(--color-btn-primary-end));

  color: var(--color-btn-primary-text);

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);

  cursor:pointer;

  transition: var(--transition-fast);

}

.refresh-button:hover{

  opacity: 0.9;

}

.cancel-button{

  height:48px;

  border: 1px solid var(--color-border);

  border-radius: var(--radius-md);

  background: none;

  color: var(--color-text-secondary);

  font-size: var(--font-sm);
  font-weight: var(--font-semibold);

  cursor:pointer;

  transition: var(--transition-fast);

}

.cancel-button:hover{

  background: var(--color-surface);
  color: var(--color-text-primary);

}

</style>