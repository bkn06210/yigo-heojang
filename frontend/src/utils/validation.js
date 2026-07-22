// 비밀번호 조건 검사
export const validatePassword = (password) => {

  const passwordRule =
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/

  return password
    ? passwordRule.test(password)
    : true

}