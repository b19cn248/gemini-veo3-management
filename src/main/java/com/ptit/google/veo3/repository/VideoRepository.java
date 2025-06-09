package com.ptit.google.veo3.repository;


import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface để truy cập dữ liệu Video
 * Mở rộng JpaRepository để có sẵn các method CRUD cơ bản
 */
@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    /**
     * Tìm video theo tên khách hàng (không phân biệt hoa thường)
     */
    List<Video> findByCustomerNameContainingIgnoreCase(String customerName);

    /**
     * Tìm video theo trạng thái
     */
    List<Video> findByStatus(VideoStatus status);

    /**
     * Tìm video được tạo trong khoảng thời gian
     */
    @Query("SELECT v FROM Video v WHERE v.createdAt BETWEEN :startDate AND :endDate")
    Page<Video> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    /**
     * Tìm video theo nhân viên được giao
     */
    List<Video> findByAssignedStaffContainingIgnoreCase(String assignedStaff);

    /**
     * Đếm số video theo trạng thái
     */
    long countByStatus(VideoStatus status);
}