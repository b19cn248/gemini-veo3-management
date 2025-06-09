package com.ptit.google.veo3.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên nhân viên không được để trống")
    @Size(max = 100, message = "Tên nhân viên không được vượt quá 100 ký tự")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải từ 10-11 chữ số")
    @Column(name = "phone", length = 15)
    private String phone;

    @Size(max = 100, message = "Phòng ban không được vượt quá 100 ký tự")
    @Column(name = "department", length = 100)
    private String department;

    @Size(max = 100, message = "Chức vụ không được vượt quá 100 ký tự")
    @Column(name = "position", length = 100)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ One-to-Many với Video
    @OneToMany(mappedBy = "assignedUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Tránh circular reference khi serialize JSON
    private List<Video> assignedVideos;

    // Helper method để lấy số lượng video được giao
    public int getAssignedVideoCount() {
        return assignedVideos != null ? assignedVideos.size() : 0;
    }
}
