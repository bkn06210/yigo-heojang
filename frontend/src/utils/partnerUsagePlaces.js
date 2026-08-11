const normalize = (value = '') =>
  String(value).toLowerCase().replace(/[^a-z0-9가-힣]/g, '')

const entries = [
  [['nh멤버스', 'nh멤버십', 'nhmembers'], ['농협하나로마트', '농협몰', 'NH농협은행', 'NH농협카드', 'NH투자증권', 'NH농협생명', 'NH농협손해보험', '농협주유소', 'NH포인트샵', '한삼인', '목우촌', '농협축산물프라자']],
  [['뷰티포인트', 'beautypoint'], ['아모레몰', '아리따움', '이니스프리', '에뛰드', '설화수', '헤라', '아이오페', '라네즈', '마몽드', '한율', '프리메라', '에스트라']],
  [['t멤버십', 't멤버스', 'tmembership'], ['파리바게뜨', '파리크라상', '뚜레쥬르', 'VIPS', '던킨', '배스킨라빈스', '세븐일레븐', 'CU', 'CGV', '롯데시네마', '메가박스', '11번가']],
  [['kt멤버십', 'ktmembership'], ['GS25', '파리바게뜨', '배스킨라빈스', '던킨', '메가박스', 'CGV', '롯데시네마', '도미노피자', 'K쇼핑', '이마트24', '스타벅스', '롯데월드']],
  [['uplus', 'u플러스', 'u멤버십', '유플러스'], ['파리바게뜨', '배스킨라빈스', 'GS25', 'CGV', '메가박스', '롯데시네마', '도미노피자', 'VIPS', '아웃백', '일리커피', '프린트베이커리', '야놀자']],
  [['네이버플러스멤버십', '네이버플러스', 'naverplus'], ['네이버플러스 스토어', '네이버쇼핑', '네이버 브랜드스토어', '네이버예약', '네이버여행', '네이버장보기', '네이버웹툰', '네이버시리즈', '티빙', '요기요', '네이버페이', '네이버 MY플레이스']],
  [['payco포인트', 'payco', '페이코'], ['PAYCO 온라인결제', 'PAYCO 오프라인결제', 'PAYCO 포인트카드', '컬처랜드', '티머니', '이즐', 'GS25', 'CU', '세븐일레븐', '이마트24', '배달의민족', '요기요']],
  [['삼성패션멤버십', '삼성패션', 'ssfshop'], ['SSF SHOP', '갤럭시', '로가디스', '빈폴', '에잇세컨즈', '구호', '구호플러스', '르베이지', '비이커', '준지', '띠어리', '메종키츠네']],
  [['lfmembers', 'lf멤버스', 'lf멤버십'], ['LF몰', '헤지스', '닥스', '질스튜어트뉴욕', '마에스트로', '알레그리', 'TNGT', '라푸마', '리복', '챔피온', '바버', '아떼']],
  [['한섬the클럽', '한섬더클럽', '한섬', 'handsomeclub'], ['더한섬닷컴', 'TIME', 'MINE', 'SYSTEM', 'SJSJ', 'TIME HOMME', 'SYSTEM HOMME', 'the CASHMERE', 'LÄTT', "O'2nd", 'DECKE', 'TOM GREYHOUND']],
  [['블루멤버스', 'bluemembers'], ['블루핸즈', '현대자동차', '현대 Shop', '현대 디지털키', '카앤라이프몰', '현대모비스', '현대오일뱅크', '해비치호텔앤드리조트', '오토앤', '현대셀렉션', 'H Genuine Accessories', '마이현대']],
  [['기아멤버스', '기아멤버십', 'kiamembers'], ['오토큐', '기아자동차', '기아멤버스몰', '기아커넥트', '기아 디지털키', '기아 EV 충전', '현대모비스', '오토앤', '카앤라이프몰', 'GS칼텍스', '해비치호텔앤드리조트', '마이기아']],
  [['cjone', 'cj원'], ['CGV', '올리브영', 'CJ더마켓', '뚜레쥬르', 'VIPS', '더플레이스', '제일제면소', 'N서울타워', 'CJ온스타일', 'TVING', '메가MGC커피', 'CU']],
  [['해피포인트', 'happypoint'], ['파리바게뜨', '배스킨라빈스', '던킨', '파스쿠찌', '쉐이크쉑', '파리크라상', '빚은', '라그릴리아', '패션5', '리나스', '커피앳웍스', '리안헤어']],
  [['lpoint', 'l포인트', '엘포인트', '롯데멤버스'], ['롯데백화점', '롯데마트', '롯데슈퍼', '롯데ON', '롯데시네마', '세븐일레븐', '롯데리아', '엔제리너스', '크리스피크림도넛', '롯데월드', '롯데호텔', 'S-OIL']],
  [['신세계포인트', 'ssgpoint', 'ssg포인트'], ['신세계백화점', '이마트', 'SSG.COM', '이마트24', '스타벅스', '신세계면세점', '조선호텔앤리조트', '스타필드', '신세계사이먼 프리미엄아울렛', '노브랜드', '일렉트로마트', '트레이더스 홀세일 클럽']],
  [['hpoint', 'h포인트', '에이치포인트'], ['현대백화점', '더현대닷컴', '현대홈쇼핑', '현대Hmall', '현대백화점면세점', '현대식품관 투홈', '한섬', '더한섬닷컴', '현대리바트', '리바트몰', '현대렌탈케어', '현대드림투어']],
  [['ok캐쉬백', 'ok캐시백', 'okcashbag'], ['11번가', 'SK에너지', '세븐일레븐', 'CU', '이마트24', '홈플러스', '롯데리아', '도미노피자', '파리바게뜨', '배스킨라빈스', '메가박스', 'YES24']],
  [['gsall멤버십', 'gsall멤버스', 'gsall', 'gs올'], ['GS25', 'GS SHOP', 'GS더프레시', '우리동네GS', 'GS Postbox', '와인25플러스', '쿠캣', 'GS Pay', 'GS리테일 행사상품', 'GS25 택배', 'GS THE FRESH 온라인몰', 'GS SHOP 모바일앱']],
  [['epoint', 'e포인트', '이포인트'], ['이랜드몰', 'NC백화점', '뉴코아아울렛', '동아백화점', '킴스클럽', '스파오', '미쏘', '후아유', '로엠', '슈펜', '폴더', '애슐리']],
]

// 부분일치라 짧은 별칭이 긴 이름을 가로챈다 — 'kt멤버십'은 't멤버십'을 포함해서
// 먼저 선언된 T 멤버십 항목에 걸린다. 가장 길게 맞는 별칭을 고르면 KT가 KT를 찾는다.
const matchEntry = (normalizedName) => {
  let best = null
  let bestLength = 0
  for (const entry of entries) {
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

export const getPartnerUsagePlaces = (providerName, apiPlaces = []) => {
  const normalizedName = normalize(providerName)
  const matched = matchEntry(normalizedName)

  if (!matched) return apiPlaces || []

  return matched[1].map((placeName, index) => ({
    usagePlaceId: `${normalizedName}-${index + 1}`,
    placeName,
    description: '',
  }))
}
// 07_25 연동 추가: 멤버십 상세 화면의 주요 사용처 목록을 관리한다.
