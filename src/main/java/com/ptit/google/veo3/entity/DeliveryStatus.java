package com.ptit.google.veo3.entity;

public enum DeliveryStatus {
    DA_GUI("Đã gửi"),
    CHUA_GUI("Chưa gửi");

    private final String displayName;

    DeliveryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
