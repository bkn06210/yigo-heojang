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
      // 부모 컴포넌트에서 Promise 반환을 기다림
      await emit('refresh')
    } finally {
      isRefreshing.value = false
    }

  }
  
  //초기화
  pullDistance.value = 0

}

</script>


<style scoped>

.refresh-container {
  overflow: hidden;
}


.refresh-indicator {
  display: flex;
  justify-content: center;
  align-items: center;

  font-size: 14px;
  color: #666;
}


.content {
  transition: transform 0.2s ease;
}

</style>