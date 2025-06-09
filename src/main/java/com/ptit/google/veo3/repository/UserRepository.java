package com.ptit.google.veo3.repository;

import com.ptit.google.veo3.entity.User;
import com.ptit.google.veo3.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm user theo email
    Optional<User> findByEmail(String email);

    // Tìm users theo trạng thái
    List<User> findByStatus(UserStatus status);

    // Tìm users đang hoạt động (dùng để gán video)
    List<User> findByStatusOrderByFullNameAsc(UserStatus status);

    // Tìm theo tên (chứa)
    Page<User> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);

    // Tìm theo phòng ban
    List<User> findByDepartmentContainingIgnoreCase(String department);

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);

    // Thống kê số lượng user theo phòng ban
    @Query("SELECT u.department, COUNT(u) FROM User u WHERE u.status = 'ACTIVE' GROUP BY u.department")
    List<Object[]> countActiveUsersByDepartment();

    // Lấy users có nhiều video được giao nhất
    @Query("SELECT u FROM User u LEFT JOIN u.assignedVideos v WHERE u.status = 'ACTIVE' GROUP BY u ORDER BY COUNT(v) DESC")
    List<User> findUsersOrderByAssignedVideoCount();
}

