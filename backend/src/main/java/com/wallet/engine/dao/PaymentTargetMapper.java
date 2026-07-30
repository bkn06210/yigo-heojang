package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.PaymentTargetRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 결제 지점 해석 매퍼 — 요청의 merchantId·categoryId를 매칭에 필요한 형태로 펼친다.
 *
 * 가맹점·카테고리 마스터 조회지만 목적이 뚜렷해 별도 매퍼로 둔다: 혜택 매칭에 쓸
 * id·코드·상위 카테고리를 한 번에 확보하는 것이다.
 *
 * 없는 id면 null을 반환한다 — 서비스가 404로 변환한다(무시하고 계산하면 요청과 다른 결과가 나간다).
 */
@Mapper
public interface PaymentTargetMapper {

    /** 가맹점 id로 해석한다. 소속 카테고리와 그 상위 카테고리까지 채운다. */
    PaymentTargetRow findByMerchantId(@Param("merchantId") long merchantId);

    /** 카테고리 id로 해석한다(가맹점 미지정). 가맹점 필드는 null이다. */
    PaymentTargetRow findByCategoryId(@Param("categoryId") long categoryId);
}
