// 카드번호 마스킹 처리
// 예) 1234567890127034 → 703*
export const maskCardNumber = (number) => {
  if (!number) return '';

  const lastFour = number.slice(-4);

  return `${lastFour.slice(0, 3)}*`;
};