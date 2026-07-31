// src/stores/authStore.js

import { defineStore } from 'pinia'
import { ref } from 'vue'


export const useAuthStore = defineStore(
  'auth',
  () => {

    // 초기 상태 복원
    const token = ref(
      localStorage.getItem('token') || null
    )


    const getStoredUser = () => {

      const savedUser =
        localStorage.getItem('user')


      if (!savedUser) {
        return null
      }


      try {

        return JSON.parse(savedUser)

      } catch (error) {

        console.error(
          '저장된 사용자 정보 파싱 실패:',
          error
        )

        localStorage.removeItem('user')

        return null

      }

    }


    const user = ref(
      getStoredUser()
    )



   
    // 로그인
   const setLogin = (
  loginToken,
  userInfo
) => {

  console.log('로그인 userInfo 확인:', userInfo);


  token.value = loginToken;

  user.value = userInfo;


  localStorage.setItem(
    'token',
    loginToken
  );
const setAccessToken = (newAccessToken) => {
  token.value = newAccessToken

  localStorage.setItem(
    'token',
    newAccessToken
  )
}

  localStorage.setItem(
    'user',
    JSON.stringify(userInfo)
  );

};
const setAccessToken = (newAccessToken) => {

  token.value = newAccessToken;

  localStorage.setItem(
    'token',
    newAccessToken
  );

};
    const updateUser = (updatedUser) => {

  user.value = {
    ...(user.value ?? {}),
    ...updatedUser,
  };


  localStorage.setItem(
    'user',
    JSON.stringify(user.value)
  );

};



   
    // 로그아웃
    const logout = () => {


      token.value = null

      user.value = null



      localStorage.removeItem(
        'token'
      )


      localStorage.removeItem(
        'user'
      )


      console.log(
        '로그아웃 완료'
      )

    }


    // 로그인 여부 확인용
    const isLogin = () => {

      return !!token.value

    }



    return {

  token,

  user,

  setLogin,
	
  setAccessToken,

  updateUser,

  logout,

  isLogin


}
  }
)