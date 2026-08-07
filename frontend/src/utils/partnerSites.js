const normalize = (value = '') =>
  String(value).toLowerCase().replace(/[^a-z0-9가-힣]/g, '')

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
  [['뷰티포인트', 'beautypoint'], 'https://one-ap.amorepacific.com/auth/login?channelCd=030&cid=1836821967.1785287602&client_id=HkJyP7_EGWntx06NtszdlhWO8vAa&commonAuthCallerPath=%2Foauth2%2Fauthorize&forceAuth=false&passiveAuth=false&redirect_uri=https%3A%2F%2Fwww.beautypoint.co.kr%2Fapi%2Foauth2client&response_type=code&scope=openid&sid=s1785744992%24o2%24g1%24t1785744997%24j55%24l0%24h0&state=%7BredirectUri%3Dhttps%3A%2F%2Fwww.beautypoint.co.kr%2Fapi%2Flogin%3FreturnUrl%3Dhttps%3A%2F%2Fwww.beautypoint.co.kr%2Ffo-api%2Flogin%2FeyJyZWRpcmVjdFVybCI6Imh0dHBzOi8vd3d3LmJlYXV0eXBvaW50LmNvLmtyL21lbWJlcnNoaXAiLCJzZXNzaW9uSWQiOiIifQ%3D%3D%2C+gid%3Dnull%2C+channelCd%3D030%2C+ot%3Dnull%2C+sid%3Ds1785744992%24o2%24g1%24t1785744997%24j55%24l0%24h0%2C+dt%3Dnull%2C+popup%3Dnull%2C+cancelUri%3D%2C+kakaoEmbedded%3Dnull%2C+join%3Dnull%2C+prompt%3Dnull%2C+vt%3Dnull%2C+cid%3D1836821967.1785287602%7D&sessionDataKey=89f75dfa-b077-4172-9e90-31f475ae9393&relyingParty=HkJyP7_EGWntx06NtszdlhWO8vAa&type=oidc&sp=030&spId=cfbe4ea9-4aad-4e9e-b6be-188aa27ee1d4&isSaaSApp=false&authenticators=NAVER%3ANA%3BKAKAO%3AKA%3BAppleID%3AAP%3BMobileVerifier%3ALOCAL%3BBasicAuthenticator%3ALOCAL%3BISKVerifier%3ALOCAL'],
  [['t멤버십', 't멤버스', 'tmembership'], 'https://shop.tworld.co.kr/exhibition/view?exhibitionId=P00000494&utm_source=tworld&utm_medium=pc_banner&utm_campaign=foldable8'],
  [['kt멤버십', 'ktmembership'], 'https://accounts.kt.com/wamui/AthWeb.do?urlcd=https%3A%2F%2Fmembership.kt.com%2Fmain%2FMainInfo.do'],
  [['uplus', 'u플러스', 'u멤버십', '유플러스'], 'https://account.lguplus.com/login?client_id=G8RoYUvnwILirwwwK3xG4WR8q9D83to7&login_type=STANDARD_WEB&prompt=select_account&i18nextLng=ko'],
  [['네이버플러스멤버십', '네이버플러스', 'naverplus'], 'https://nid.naver.com/membership/join'],
  [['payco포인트', 'payco', '페이코'], 'https://www.payco.com/'],
  [['삼성패션멤버십', '삼성패션', 'ssfshop'], 'https://m.ssfshop.com/public/member/addMemberStep1'],
  [['lfmembers', 'lf멤버스', 'lf멤버십'], 'https://www.lfmembers.co.kr:4441/web/index.do'],
  [['한섬the클럽', '한섬더클럽', '한섬', 'handsomeclub'], 'https://m.thehandsome.com/ko/MK/event/24743'],
  [['블루멤버스', 'bluemembers'], 'https://idpconnect-kr.hyundai.com/auth/api/v2/user/oauth2/authorize?client_id=2b6b3d2d-5ccc-497f-8e8b-e9b2d7582097&redirect_uri=https%3A%2F%2Fwww.hyundai.com%2Fkr%2Fko%2Fccspcallback.html%3Fhmgid%3D2&response_type=code&scope=&state=login&connector_client_id=hmgid1.0-2b6b3d2d-5ccc-497f-8e8b-e9b2d7582097&ui_locales=&connector_scope=&connector_session_key=3dba3d13-92ad-4c72-acc3-462184770a7f'],
  [['기아멤버스', '기아멤버십', 'kiamembers'], 'https://idpconnect-kr.kia.com/auth/api/v2/user/oauth2/authorize?client_id=8a98de01-a5ca-442a-a4aa-9127647f9c7b&redirect_uri=https%3A%2F%2Fmembers.kia.com%2Fkr%2Fview%2Fhmgidredirect.do&response_type=code&scope=&state=1&connector_client_id=hmgid1.0-8a98de01-a5ca-442a-a4aa-9127647f9c7b&ui_locales=&connector_scope=&connector_session_key=f1026214-d10b-4dce-a750-9a941f79e63d'],
]

export const getOfficialSiteUrl = (providerName, apiUrl = '') => {
  const normalizedName = normalize(providerName)
  const matched = officialSiteEntries.find(([aliases]) =>
    aliases.some((alias) => normalizedName.includes(normalize(alias)))
  )

  return matched?.[1] || apiUrl || ''
}
// 07_25 연동 추가: 멤버십별 공식 사이트 이동 주소를 관리한다.
