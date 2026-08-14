package com.wallet.notification.batch.model;

/**
 * "이 회원에게 이 중복방지키로 이미 알림을 보냈는가"를 묻는 조회 입력 한 건.
 * <p>
 * 예전에는 {@code "회원ID:키"} 형태의 문자열 하나로 넘겼는데, 그러면 조회 SQL이
 * {@code WHERE CONCAT(member_id, ':', deduplication_key) IN (...)} 형태가 되어
 * 인덱스 컬럼을 함수로 감싸게 된다. MySQL은 이런 조건에서
 * {@code UNIQUE(member_id, deduplication_key)} 인덱스를 쓰지 못하고 전체 스캔을 한다.
 * <p>
 * 두 값을 분리해서 들고 있으면 SQL을 컬럼 대 컬럼 비교(JOIN)로 쓸 수 있어
 * 그 인덱스를 그대로 탄다. 알림이 쌓일수록 차이가 커지는 지점이다.
 *
 * @param memberId        회원 ID
 * @param deduplicationKey 중복 방지 키
 */
public record MemberDedupKeyLookup(
    Long memberId,
    String deduplicationKey
) {
}
