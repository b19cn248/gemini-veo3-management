package com.ptit.google.veo3.entity;

public enum VideoStatus {
    CHUA_AI_NHAN("Chưa ai nhận"),
    DANG_LAM("Đang làm"),
    DA_XONG("Đã xong"),
    DANG_SUA("Đang sửa"),
    DA_SUA_XONG("Đã sửa xong");

    private final String displayName;

    VideoStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}