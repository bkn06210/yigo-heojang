package com.wallet.auth.domain;

// 약관(term)이 어느 화면에서 노출·동의되는지를 나타내는 값이다.
// DB의 term.term_scope 컬럼 값과 이름이 완전히 같아야 한다 (SIGNUP, WITHDRAWAL).
public enum TermScope {
    SIGNUP,
    WITHDRAWAL
}
