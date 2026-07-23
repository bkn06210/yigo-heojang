package com.wallet.engine.model;

/**
 * 계산기가 해석하지 못해 무시한 제외 규칙 한 건.
 *
 * performance_exclusion은 값에 FK가 없어(코드 문자열) 오타·판정 불가값이 섞일 수 있다.
 * 알려진 유형인데 값을 해석할 수 없는 경우(OVERSEAS — expense에 대응 컬럼이 없음,
 * MIN_TXN_AMOUNT 숫자 파싱 실패 등)는 예외로 계산 전체를 죽이지 않고 이 목록에 담아 반환한다.
 *
 * 소비처가 둘이다 — 운영자용 로그(서비스 계층이 남긴다)와 챗봇용 사용자 답변("이 규칙이
 * 반영되지 않았다"). 계산기 자신은 로깅하지 않는다(순수성 유지).
 *
 * 모르는 유형(스키마 위반)은 이 목록이 아니라 예외로 처리한다 — 그건 버그이기 때문이다.
 *
 * @param rawType  원본 exclusion_type 문자열
 * @param rawValue 원본 exclusion_value 문자열
 * @param reason   무시한 이유 (사람이 읽는 설명)
 */
public record IgnoredExclusion(String rawType, String rawValue, String reason) {
}
