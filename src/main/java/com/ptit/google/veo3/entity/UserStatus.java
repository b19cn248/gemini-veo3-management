package com.ptit.google.veo3.entity;

public enum UserStatus {
    ACTIVE("Đang hoạt động"),
    INACTIVE("Tạm nghỉ"),
    RESIGNED("Đã nghỉ việc");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
