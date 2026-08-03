package com.wallet.auth.domain;

public class VerificationStatus {
    public static final String PENDING = "PENDING";
    public static final String VERIFIED = "VERIFIED";
    public static final String USED = "USED";
    public static final String EXPIRED = "EXPIRED";

    private VerificationStatus() {
    }
}
