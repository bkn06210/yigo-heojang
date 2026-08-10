import { ref } from 'vue';

// 앱 전역에서 공유하는 단일 토스트 상태 — App.vue에 렌더러 하나만 두고
// 어느 화면에서든 showToast()만 호출하면 됨 (라우터 이동에도 유지됨)
const toast = ref(null);

export function useToast() {
  const showToast = (type, message, duration = 4000) => {
    toast.value = { type, message, duration, key: Date.now() };
  };

  const closeToast = () => {
    toast.value = null;
  };

  return { toast, showToast, closeToast };
}
