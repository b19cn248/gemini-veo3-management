package com.ptit.google.veo3.entity;

public enum PaymentStatus {
    DA_THANH_TOAN("Đã thanh toán"),
    CHUA_THANH_TOAN("Chưa thanh toán"),
    BUNG("Bùng");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
