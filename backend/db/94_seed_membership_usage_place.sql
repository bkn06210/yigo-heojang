-- 멤버십 주요 사용처 / 공식 사이트 시드.
-- 프론트 utils(partnerUsagePlaces.js, partnerSites.js)에 하드코딩돼 있던 값을 DB로 옮긴 것이다.
-- 이 파일은 스크립트로 생성했다. 값을 고칠 때는 여기를 직접 고친다.
--
-- 제공사를 id 가 아니라 <b>이름</b>으로 찾는다. id 는 DB 마다 다르다 —
-- 실제로 개발 DB(22개)와 새로 만든 DB(20개)의 id 가 2씩 어긋나 있었고,
-- id 로 적었을 때 URL 18건이 에러 없이 엉뚱한 제공사에 들어갔다.
-- 이름이 없으면 서브쿼리가 NULL 을 돌려주고 NOT NULL 제약에 걸려 즉시 실패한다. 조용히 틀리는 것보다 낫다.
--
-- 실행 순서: schema.sql → data.sql → 91 → 92 → 이 파일

-- 1) 공식 사이트 URL 컬럼.
--    schema.sql 에는 이 컬럼이 이미 들어 있다. 새로 세팅하는 사람은 여기서 아무 일도 일어나지 않는다.
--    이 블록은 schema.sql 이전 버전으로 만든 DB를 쓰는 사람을 위한 것이다.
--    MySQL 8 에는 ADD COLUMN IF NOT EXISTS 가 없어 존재 여부를 직접 확인한다.
--    그냥 ALTER 를 두면 신규 세팅에서 'Duplicate column name' 으로 스크립트가 중단된다.
SET @has_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'point_provider'
      AND COLUMN_NAME = 'official_site_url'
);
SET @ddl = IF(@has_col = 0,
    'ALTER TABLE point_provider ADD COLUMN official_site_url VARCHAR(1500) NULL COMMENT ''공식 사이트 URL'' AFTER logo_image_url',
    'DO 0');
PREPARE add_col FROM @ddl;
EXECUTE add_col;
DEALLOCATE PREPARE add_col;

-- 여기부터는 한 덩어리로 처리한다.
-- 사용처 교체가 DELETE 후 INSERT 라서, 중간에 실패하면(제공사 이름 불일치로
-- 서브쿼리가 NULL 을 돌려주는 경우가 대부분이다) 기존 사용처만 사라진 DB 가 남는다.
-- ALTER TABLE 은 암묵적 커밋을 일으켜 트랜잭션을 끊으므로 반드시 위 DDL 블록 다음에 시작한다.
--
-- 배치 모드(`mysql ... < setup.sql`)에서는 에러가 나면 클라이언트가 즉시 종료하고,
-- COMMIT 전에 연결이 끊기면 InnoDB 가 전부 롤백한다.
-- 대화형 클라이언트에서 SOURCE 로 실행할 때는 에러가 나도 그냥 다음 문장으로 넘어가므로
-- `mysql --abort-source-on-error ...` 로 실행해야 같은 보호를 받는다.
START TRANSACTION;

-- 2) 공식 사이트 URL
--    주소창에서 복사한 URL 을 그대로 넣지 마라. 로그인 도중의 주소에는
--    sessionDataKey, state, sid, connector_session_key 같은 1회용 세션 값이 들어 있어
--    며칠 뒤면 만료되고 인증 요청이 거부된다.
--    항상 세션 값이 없는 랜딩 페이지나 로그인 시작 주소를 쓴다.
UPDATE point_provider SET official_site_url = 'https://www.cjone.com/cjmweb/login.do'
  WHERE point_provider_name = 'CJ ONE';
UPDATE point_provider SET official_site_url = 'https://www.happypointcard.com/sso/login.jsp?returnUrl=/page/presentation/membership.spc'
  WHERE point_provider_name = '해피포인트';
UPDATE point_provider SET official_site_url = 'https://m.lpoint.com/app/login/LWLA100100.do'
  WHERE point_provider_name = 'L.POINT';
UPDATE point_provider SET official_site_url = 'https://www.shinsegae.com/service/membership/shinsegae-point.do'
  WHERE point_provider_name = '신세계포인트';
UPDATE point_provider SET official_site_url = 'https://h-point.co.kr/cu/login.nhd'
  WHERE point_provider_name = 'H.Point';
UPDATE point_provider SET official_site_url = 'https://www.okcashbag.com/login'
  WHERE point_provider_name = 'OK캐쉬백';
UPDATE point_provider SET official_site_url = 'https://www.gsall.com/gsallpoint.html'
  WHERE point_provider_name = 'GS ALL 멤버십';
UPDATE point_provider SET official_site_url = 'https://www.elandretail.com/m/epoint/Integrate_Account1.do'
  WHERE point_provider_name = 'E.POINT';
UPDATE point_provider SET official_site_url = 'https://www.nhmembers.co.kr/nhweb/join/joinMbSelectCert.nh'
  WHERE point_provider_name = 'NH멤버스';
UPDATE point_provider SET official_site_url = 'https://www.beautypoint.co.kr/'
  WHERE point_provider_name = '뷰티포인트';
UPDATE point_provider SET official_site_url = 'https://shop.tworld.co.kr/exhibition/view?exhibitionId=P00000494&utm_source=tworld&utm_medium=pc_banner&utm_campaign=foldable8'
  WHERE point_provider_name = 'T 멤버십';
UPDATE point_provider SET official_site_url = 'https://accounts.kt.com/wamui/AthWeb.do?urlcd=https%3A%2F%2Fmembership.kt.com%2Fmain%2FMainInfo.do'
  WHERE point_provider_name = 'KT 멤버십';
UPDATE point_provider SET official_site_url = 'https://account.lguplus.com/login?client_id=G8RoYUvnwILirwwwK3xG4WR8q9D83to7&login_type=STANDARD_WEB&prompt=select_account&i18nextLng=ko'
  WHERE point_provider_name = 'U+ 멤버십';
UPDATE point_provider SET official_site_url = 'https://nid.naver.com/membership/join'
  WHERE point_provider_name = '네이버플러스 멤버십';
UPDATE point_provider SET official_site_url = 'https://www.payco.com/'
  WHERE point_provider_name = 'PAYCO 포인트';
UPDATE point_provider SET official_site_url = 'https://m.ssfshop.com/public/member/addMemberStep1'
  WHERE point_provider_name = '삼성패션 멤버십';
UPDATE point_provider SET official_site_url = 'https://www.lfmembers.co.kr:4441/web/index.do'
  WHERE point_provider_name = 'LF Members';
UPDATE point_provider SET official_site_url = 'https://m.thehandsome.com/ko/MK/event/24743'
  WHERE point_provider_name = '한섬 THE 클럽';
UPDATE point_provider SET official_site_url = 'https://www.hyundai.com/kr/ko/service-membership/bluemembers/bluemembers-benefit'
  WHERE point_provider_name = '블루멤버스';
UPDATE point_provider SET official_site_url = 'https://members.kia.com/'
  WHERE point_provider_name = '기아멤버스';

-- 3) 주요 사용처. 재실행해도 중복이 쌓이지 않도록 멤버십 제공사의 기존 행을 먼저 지운다.
DELETE FROM point_usage_place
WHERE point_provider_id IN (
    SELECT point_provider_id FROM point_provider WHERE point_provider_type = 'MEMBERSHIP'
);

INSERT INTO point_usage_place (point_provider_id, place_name, use_yn) VALUES
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), 'CGV', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), '올리브영', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), 'CJ더마켓', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), '뚜레쥬르', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), 'VIPS', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), '더플레이스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), '제일제면소', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), 'N서울타워', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), 'CJ온스타일', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), 'TVING', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), '메가MGC커피', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'CJ ONE'), 'CU', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '파리바게뜨', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '배스킨라빈스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '던킨', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '파스쿠찌', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '쉐이크쉑', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '파리크라상', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '빚은', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '라그릴리아', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '패션5', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '리나스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '커피앳웍스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '해피포인트'), '리안헤어', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '롯데백화점', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '롯데마트', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '롯데슈퍼', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '롯데ON', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '롯데시네마', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '세븐일레븐', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '롯데리아', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '엔제리너스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '크리스피크림도넛', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '롯데월드', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), '롯데호텔', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'L.POINT'), 'S-OIL', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '신세계백화점', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '이마트', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), 'SSG.COM', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '이마트24', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '스타벅스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '신세계면세점', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '조선호텔앤리조트', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '스타필드', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '신세계사이먼 프리미엄아울렛', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '노브랜드', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '일렉트로마트', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '신세계포인트'), '트레이더스 홀세일 클럽', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '현대백화점', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '더현대닷컴', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '현대홈쇼핑', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '현대Hmall', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '현대백화점면세점', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '현대식품관 투홈', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '한섬', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '더한섬닷컴', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '현대리바트', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '리바트몰', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '현대렌탈케어', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'H.Point'), '현대드림투어', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), '11번가', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), 'SK에너지', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), '세븐일레븐', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), 'CU', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), '이마트24', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), '홈플러스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), '롯데리아', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), '도미노피자', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), '파리바게뜨', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), '배스킨라빈스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), '메가박스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'OK캐쉬백'), 'YES24', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), 'GS25', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), 'GS SHOP', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), 'GS더프레시', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), '우리동네GS', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), 'GS Postbox', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), '와인25플러스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), '쿠캣', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), 'GS Pay', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), 'GS리테일 행사상품', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), 'GS25 택배', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), 'GS THE FRESH 온라인몰', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'GS ALL 멤버십'), 'GS SHOP 모바일앱', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '이랜드몰', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), 'NC백화점', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '뉴코아아울렛', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '동아백화점', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '킴스클럽', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '스파오', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '미쏘', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '후아유', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '로엠', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '슈펜', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '폴더', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'E.POINT'), '애슐리', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), '농협하나로마트', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), '농협몰', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), 'NH농협은행', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), 'NH농협카드', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), 'NH투자증권', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), 'NH농협생명', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), 'NH농협손해보험', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), '농협주유소', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), 'NH포인트샵', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), '한삼인', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), '목우촌', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'NH멤버스'), '농협축산물프라자', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '아모레몰', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '아리따움', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '이니스프리', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '에뛰드', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '설화수', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '헤라', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '아이오페', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '라네즈', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '마몽드', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '한율', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '프리메라', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '뷰티포인트'), '에스트라', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), '파리바게뜨', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), '파리크라상', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), '뚜레쥬르', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), 'VIPS', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), '던킨', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), '배스킨라빈스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), '세븐일레븐', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), 'CU', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), 'CGV', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), '롯데시네마', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), '메가박스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'T 멤버십'), '11번가', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), 'GS25', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), '파리바게뜨', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), '배스킨라빈스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), '던킨', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), '메가박스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), 'CGV', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), '롯데시네마', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), '도미노피자', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), 'K쇼핑', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), '이마트24', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), '스타벅스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'KT 멤버십'), '롯데월드', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), '파리바게뜨', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), '배스킨라빈스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), 'GS25', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), 'CGV', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), '메가박스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), '롯데시네마', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), '도미노피자', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), 'VIPS', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), '아웃백', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), '일리커피', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), '프린트베이커리', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'U+ 멤버십'), '야놀자', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '네이버플러스 스토어', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '네이버쇼핑', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '네이버 브랜드스토어', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '네이버예약', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '네이버여행', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '네이버장보기', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '네이버웹툰', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '네이버시리즈', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '티빙', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '요기요', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '네이버페이', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '네이버플러스 멤버십'), '네이버 MY플레이스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), 'PAYCO 온라인결제', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), 'PAYCO 오프라인결제', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), 'PAYCO 포인트카드', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), '컬처랜드', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), '티머니', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), '이즐', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), 'GS25', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), 'CU', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), '세븐일레븐', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), '이마트24', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), '배달의민족', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'PAYCO 포인트'), '요기요', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), 'SSF SHOP', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '갤럭시', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '로가디스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '빈폴', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '에잇세컨즈', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '구호', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '구호플러스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '르베이지', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '비이커', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '준지', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '띠어리', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '삼성패션 멤버십'), '메종키츠네', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), 'LF몰', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), '헤지스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), '닥스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), '질스튜어트뉴욕', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), '마에스트로', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), '알레그리', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), 'TNGT', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), '라푸마', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), '리복', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), '챔피온', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), '바버', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = 'LF Members'), '아떼', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), '더한섬닷컴', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'TIME', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'MINE', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'SYSTEM', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'SJSJ', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'TIME HOMME', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'SYSTEM HOMME', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'the CASHMERE', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'LÄTT', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'O''2nd', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'DECKE', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '한섬 THE 클럽'), 'TOM GREYHOUND', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '블루핸즈', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '현대자동차', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '현대 Shop', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '현대 디지털키', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '카앤라이프몰', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '현대모비스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '현대오일뱅크', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '해비치호텔앤드리조트', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '오토앤', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '현대셀렉션', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), 'H Genuine Accessories', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '블루멤버스'), '마이현대', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '오토큐', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '기아자동차', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '기아멤버스몰', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '기아커넥트', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '기아 디지털키', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '기아 EV 충전', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '현대모비스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '오토앤', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '카앤라이프몰', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), 'GS칼텍스', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '해비치호텔앤드리조트', 'Y'),
    ((SELECT point_provider_id FROM point_provider WHERE point_provider_name = '기아멤버스'), '마이기아', 'Y');

-- 여기까지 전부 성공했을 때만 반영된다.
COMMIT;
