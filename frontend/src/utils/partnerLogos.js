const normalize = (value = '') =>
  String(value).toLowerCase().replace(/[^a-z0-9가-힣]/g, '')

const logoEntries = [
  [['뷰티포인트', 'beautypoint'], 'beauty-point.png'],
  [['블루멤버스', 'bluemembers'], 'blue-members.png'],
  [['cjone', 'cj원'], 'cj-one.png'],
  [['epoint', 'e포인트', '이포인트'], 'e-point.png'],
  [['gsall멤버십', 'gsall멤버스', 'gsall', 'gs올'], 'gs-all.png'],
  [['hpoint', 'h포인트', '에이치포인트'], 'h-point.png'],
  [['한섬', '한섬마일리지', 'handsomeclub'], 'handsome-club.png'],
  [['해피포인트', 'happypoint'], 'happypoint.png'],
  [['기아멤버스', '기아멤버십', 'kiamembers'], 'kia-members.png'],
  [['kt멤버십', 'ktmembership'], 'kt-membership.png'],
  [['lfmembers', 'lf멤버스', 'lf멤버십'], 'lf-members.png'],
  [['lpoint', 'l포인트', '엘포인트', '롯데멤버스'], 'lpoint.png'],
  [['네이버플러스', 'naverplus'], 'naver-plus.png'],
  [['nh멤버스', 'nh멤버십', 'nhmembers'], 'nh-members.png'],
  [['ok캐쉬백', 'ok캐시백', 'okcashbag'], 'okcashbag.png'],
  [['payco', '페이코'], 'payco-point.png'],
  [['신세계포인트', 'ssgpoint', 'ssg포인트'], 'ssg-point.png'],
  [['t멤버십', 't멤버스', 'tmembership'], 't-membership.png'],
  [['uplus', 'u플러스', 'u멤버십', '유플러스'], 'uplus-membership.png'],
]

export const getPartnerLogo = (providerName, apiLogo) => {
  const normalizedName = normalize(providerName)
  const localLogo = logoEntries.find(([aliases]) =>
    aliases.some((alias) => normalizedName.includes(normalize(alias)))
  )

  // DB에는 예전 /images/points/... 및 example.com 주소가 남아 있을 수 있다.
  // 프로젝트에 포함된 로고가 있으면 항상 검증된 로컬 파일을 우선 사용한다.
  if (localLogo) return `/images/${localLogo[1]}`

  return apiLogo || ''
}
// 07_25 연동 추가: 백엔드 제휴사명을 로컬 로고 이미지 경로로 매핑한다.
