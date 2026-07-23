package com.wallet.engine.model;

/**
 * 혜택이 겨냥하는 대상의 종류 (benefit.target_type).
 * CATEGORY는 대분류를 넣으면 하위 중분류 결제까지 매칭된다 (계층 깊이 2단계 고정).
 */
public enum TargetType {
    CATEGORY,
    MERCHANT,
    ALL
}
