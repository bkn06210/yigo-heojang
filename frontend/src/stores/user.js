import { defineStore } from 'pinia';
import { ref } from 'vue';


export const useUserStore = defineStore('user', () => {


  // 저장된 사용자 정보 불러오기
  const savedUser = localStorage.getItem('user');


  const user = ref(
    savedUser
      ? JSON.parse(savedUser)
      : null
  );


  // 사용자 정보 수정
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
  const clearUser = () => {

  user.value = null;

  localStorage.removeItem('user');

};


 return {
  user,
  updateUser,
  clearUser,
};
});