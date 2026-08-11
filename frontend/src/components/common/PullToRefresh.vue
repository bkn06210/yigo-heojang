<template>
  <div
    class="refresh-container"
    @touchstart="handleTouchStart"
    @touchmove="handleTouchMove"
    @touchend="handleTouchEnd"
  >

    <!-- 새로고침 안내 영역 -->
    <div
      class="refresh-indicator"
      :style="{ height: `${pullDistance}px` }"
    >

      <span v-if="isRefreshing">
        새로고침 중...
      </span>

      <span v-else-if="pullDistance > threshold">
        놓으면 새로고침
      </span>

      <span v-else-if="pullDistance > 0">
        아래로 당겨 새로고침
      </span>

    </div>


    <!-- 실제 페이지 내용 -->
    <div
      class="content"
      :style="{ transform: `translateY(${pullDistance}px)` }"
    >

      <slot />

    </div>

  </div>
</template>


<script setup>
import { ref } from 'vue'


// 새로고침 이벤트 전달
const emit = defineEmits([
  'refresh'
])


// 현재 당긴 거리
const pullDistance = ref(0)


// 새로고침 진행 여부
const isRefreshing = ref(false)


// 시작 위치
const startY = ref(0)


// 새로고침 기준 거리
const threshold = 80



// 손가락을 처음 댄 위치
const handleTouchStart = (event) => {

  startY.value = event.touches[0].clientY

}



// 움직이는 동안 거리 계산
const handleTouchMove = (event) => {

  const currentY = event.touches[0].clientY

  const distance = currentY - startY.value


  // 아래로 당길 때만 동작
  if(distance > 0){

    pullDistance.value = Math.min(distance, 120)

  }

}



// 손을 뗐을 때
const handleTouchEnd = async () => {

  if(pullDistance.value > threshold){

    isRefreshing.value = true

    try {

      await emit('refresh')

    } finally {

      isRefreshing.value = false

    }

  }


  pullDistance.value = 0

}

</script>


<style scoped>

.refresh-container {

  overflow: visible;

  width: 100%;

  height: 100%;

  display: flex;

  flex-direction: column;

}



.refresh-indicator {

  display: flex;

  justify-content: center;

  align-items: center;


  color: var(--color-text-secondary);

  font-size: var(--font-sm);

}



.content {

  transition: transform var(--transition-fast);

  flex: 1;

  display: flex;

  flex-direction: column;

}

</style>