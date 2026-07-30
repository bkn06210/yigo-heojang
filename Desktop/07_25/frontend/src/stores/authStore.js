//로그인 상태 관리 전용 저장소

import { defineStore } from 'pinia'
import { ref } from 'vue'


// 로그인 사용자 상태 관리 Store
export const useAuthStore = defineStore(
  'auth',
  () => {


    // 로그인 토큰 저장
    // 로그인 성공 후 백엔드에서 받은 accessToken 저장 예정
    const token = ref(null)


    // 로그인 사용자 정보 저장
    const user = ref(null)



    // 로그인 처리
    // 추후 LoginView에서 호출
    const setLogin = (loginToken, userInfo) => {

      token.value = loginToken
      user.value = userInfo


      // 새로고침해도 유지되도록 저장
      localStorage.setItem(
        'token',
        loginToken
      )

    }



    // 로그아웃 처리
    const logout = () => {

      token.value = null
      user.value = null


      localStorage.removeItem(
        'token'
      )

    }



    return {
      token,
      user,
      setLogin,
      logout
    }

  }
)