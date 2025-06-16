package com.ptit.google.veo3.dto;

/**
 * Enum định nghĩa các loại notification trong hệ thống
 */
public enum NotificationType {
    /**
     * Thông báo khi video cần sửa gấp - gửi cho assignedStaff
     */
    VIDEO_NEEDS_URGENT_FIX("Video cần sửa gấp"),
    
    /**
     * Thông báo khi video đã sửa xong - gửi cho createdBy (sale/admin)
     */
    VIDEO_FIXED_COMPLETED("Video đã được sửa xong");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}