const normalize = (value = '') =>
  String(value).toLowerCase().replace(/[^a-z0-9가-힣]/g, '')

// 주소창에서 복사한 URL 을 그대로 넣지 마라. 로그인 도중의 주소에는
// sessionDataKey, state, sid, connector_session_key 같은 1회용 세션 값이 들어 있어
// 며칠 뒤면 만료되고 인증 요청이 거부된다.
// 항상 각 제공사의 랜딩 페이지나 로그인 시작 주소(세션 값 없는 고정 주소)를 쓴다.
const officialSiteEntries = [
  [['cjone', 'cj원'], 'https://www.cjone.com/cjmweb/login.do'],
  [['해피포인트', 'happypoint'], 'https://www.happypointcard.com/sso/login.jsp?returnUrl=/page/presentation/membership.spc'],
  [['lpoint', 'l포인트', '엘포인트', '롯데멤버스'], 'https://m.lpoint.com/app/login/LWLA100100.do'],
  [['신세계포인트', 'ssgpoint', 'ssg포인트'], 'https://www.shinsegae.com/service/membership/shinsegae-point.do'],
  [['hpoint', 'h포인트', '에이치포인트'], 'https://h-point.co.kr/cu/login.nhd'],
  [['ok캐쉬백', 'ok캐시백', 'okcashbag'], 'https://www.okcashbag.com/login'],
  [['gsall멤버십', 'gsall멤버스', 'gsall', 'gs올'], 'https://www.gsall.com/gsallpoint.html'],
  [['epoint', 'e포인트', '이포인트'], 'https://www.elandretail.com/m/epoint/Integrate_Account1.do'],
  [['nh멤버스', 'nh멤버십', 'nhmembers'], 'https://www.nhmembers.co.kr/nhweb/join/joinMbSelectCert.nh'],
  [['뷰티포인트', 'beautypoint'], 'https://www.beautypoint.co.kr/'],
  [['t멤버십', 't멤버스', 'tmembership'], 'https://shop.tworld.co.kr/exhibition/view?exhibitionId=P00000494&utm_source=tworld&utm_medium=pc_banner&utm_campaign=foldable8'],
  [['kt멤버십', 'ktmembership'], 'https://accounts.kt.com/wamui/AthWeb.do?urlcd=https%3A%2F%2Fmembership.kt.com%2Fmain%2FMainInfo.do'],
  [['uplus', 'u플러스', 'u멤버십', '유플러스'], 'https://account.lguplus.com/login?client_id=G8RoYUvnwILirwwwK3xG4WR8q9D83to7&login_type=STANDARD_WEB&prompt=select_account&i18nextLng=ko'],
  [['네이버플러스멤버십', '네이버플러스', 'naverplus'], 'https://nid.naver.com/membership/join'],
  [['payco포인트', 'payco', '페이코'], 'https://www.payco.com/'],
  [['삼성패션멤버십', '삼성패션', 'ssfshop'], 'https://m.ssfshop.com/public/member/addMemberStep1'],
  [['lfmembers', 'lf멤버스', 'lf멤버십'], 'https://www.lfmembers.co.kr:4441/web/index.do'],
  [['한섬the클럽', '한섬더클럽', '한섬', 'handsomeclub'], 'https://m.thehandsome.com/ko/MK/event/24743'],
  [['블루멤버스', 'bluemembers'], 'https://www.hyundai.com/kr/ko/service-membership/bluemembers/bluemembers-benefit'],
  [['기아멤버스', '기아멤버십', 'kiamembers'], 'https://members.kia.com/'],
]

// 부분일치라 짧은 별칭이 긴 이름을 가로챈다 — 'kt멤버십'은 't멤버십'을 포함해서
// 먼저 선언된 T 멤버십 항목에 걸린다. 가장 길게 맞는 별칭을 고르면 KT가 KT를 찾는다.
const matchEntry = (normalizedName) => {
  let best = null
  let bestLength = 0
  for (const entry of officialSiteEntries) {
    for (const alias of entry[0]) {
      const normalizedAlias = normalize(alias)
      if (normalizedName.includes(normalizedAlias) && normalizedAlias.length > bestLength) {
        best = entry
        bestLength = normalizedAlias.length
      }
    }
  }
  return best
}

export const getOfficialSiteUrl = (providerName, apiUrl = '') => {
  const normalizedName = normalize(providerName)
  const matched = matchEntry(normalizedName)

  return matched?.[1] || apiUrl || ''
}
// 07_25 연동 추가: 멤버십별 공식 사이트 이동 주소를 관리한다.
