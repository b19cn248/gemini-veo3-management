package com.ptit.google.veo3.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity đại diện cho bảng users trong database
 * Kế thừa từ BaseEntity để có các trường audit fields
 * <p>
 * Chứa thông tin cơ bản của user trong hệ thống
 *
 * @author Generated
 * @since 1.0
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    /**
     * Primary key - ID tự tăng
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Họ và tên đầy đủ của user
     * Trường bắt buộc, tối đa 255 ký tự
     */
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /**
     * Tên (first name) của user
     * Tối đa 100 ký tự
     */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /**
     * Họ (last name) của user
     * Tối đa 100 ký tự
     */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /**
     * Username duy nhất trong hệ thống
     * Trường bắt buộc, unique, tối đa 50 ký tự
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Email của user
     * Không bắt buộc, tối đa 255 ký tự
     * Có thể null vì có thể user chỉ đăng nhập bằng username
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Trạng thái hoạt động của user
     * true = active, false = inactive
     * Mặc định là true khi tạo mới
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Ghi chú về user (optional)
     * Có thể chứa thông tin bổ sung như phòng ban, vị trí,...
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Business method: Deactivate user
     * Đặt isActive = false thay vì xóa
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Business method: Activate user
     * Đặt isActive = true
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Business method: Lấy display name
     * Ưu tiên fullName, fallback về firstName + lastName, cuối cùng là username
     *
     * @return Display name phù hợp
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }

        if (firstName != null && lastName != null &&
                !firstName.trim().isEmpty() && !lastName.trim().isEmpty()) {
            return firstName.trim() + " " + lastName.trim();
        }

        return username != null ? username : "Unknown User";
    }
}