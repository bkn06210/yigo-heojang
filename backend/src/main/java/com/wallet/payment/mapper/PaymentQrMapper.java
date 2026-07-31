package com.wallet.payment.mapper;

import com.wallet.payment.dto.PaymentQrInsertParam;
import com.wallet.payment.dto.PaymentQrPayParam;
import com.wallet.payment.dto.PaymentQrRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentQrMapper {

    int countActiveUserCard(
            @Param("memberId") Long memberId,
            @Param("userCardId") Long userCardId
    );

    void insertPaymentQr(PaymentQrInsertParam param);

    PaymentQrRecord selectPaymentQrByToken(
            @Param("qrToken") String qrToken
    );

    PaymentQrRecord selectPaymentQrForUpdate(
            @Param("qrToken") String qrToken
    );

    int markQrExpired(
            @Param("qrToken") String qrToken
    );

    void insertExpenseByQr(PaymentQrPayParam param);

    void insertPaymentByQr(PaymentQrPayParam param);

    int markQrUsed(
            @Param("qrToken") String qrToken,
            @Param("paymentId") Long paymentId
    );
    int markQrFailed(
            @Param("qrToken") String qrToken,
            @Param("failReason") String failReason
    );
}