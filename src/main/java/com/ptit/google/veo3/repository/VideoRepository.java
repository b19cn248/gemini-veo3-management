package com.ptit.google.veo3.repository;

import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.User;
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

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    // Tìm theo tên khách hàng
    List<Video> findByCustomerNameContainingIgnoreCase(String customerName);

    // Tìm theo trạng thái
    Page<Video> findByStatus(VideoStatus status, Pageable pageable);

    // Tìm theo trạng thái thanh toán
    Page<Video> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    // Tìm theo user được giao
    List<Video> findByAssignedUser(User assignedUser);

    // Tìm theo user được giao với phân trang
    Page<Video> findByAssignedUser(User assignedUser, Pageable pageable);

    // Tìm các video cần giao trong khoảng thời gian
    @Query("SELECT v FROM Video v WHERE v.deliveryTime BETWEEN :startDate AND :endDate")
    List<Video> findByDeliveryTimeBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Thống kê số lượng video theo trạng thái
    @Query("SELECT v.status, COUNT(v) FROM Video v GROUP BY v.status")
    List<Object[]> countVideosByStatus();

    // Thống kê số lượng video theo user
    @Query("SELECT u.fullName, COUNT(v) FROM Video v JOIN v.assignedUser u GROUP BY u.id, u.fullName ORDER BY COUNT(v) DESC")
    List<Object[]> countVideosByUser();

    // Tìm video chưa có ai nhận
    List<Video> findByAssignedUserIsNull();
}
