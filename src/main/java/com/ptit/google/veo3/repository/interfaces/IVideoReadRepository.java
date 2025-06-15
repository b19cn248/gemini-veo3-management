package com.ptit.google.veo3.repository.interfaces;

import com.ptit.google.veo3.dto.SalesSalaryProjection;
import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.entity.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Read-only repository interface for Video entity
 * Following Interface Segregation Principle - separating read operations
 */
public interface IVideoReadRepository {
    
    // Basic read operations
    Optional<Video> findById(Long id);
    List<Video> findAll();
    Page<Video> findAll(Pageable pageable);
    boolean existsById(Long id);
    boolean existsByCustomerName(String customerName);
    
    // Search operations
    List<Video> findByCustomerNameContainingIgnoreCase(String customerName);
    List<Video> findByStatus(VideoStatus status);
    List<Video> findByAssignedStaffIgnoreCase(String assignedStaff);
    Page<Video> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Complex queries
    List<Video> findByCustomerNameContainingIgnoreCaseAndStatusAndAssignedStaffIgnoreCaseAndCreatedAtBetween(
            String customerName, VideoStatus status, String assignedStaff, 
            LocalDateTime startDate, LocalDateTime endDate);
    
    List<Video> findByAssignedStaffIgnoreCaseAndDeliveryStatusAndPaymentStatus(
            String assignedStaff, DeliveryStatus deliveryStatus, PaymentStatus paymentStatus);
    
    // Count operations
    Long countByStatus(VideoStatus status);
    Long countByAssignedStaffIgnoreCaseAndStatusIn(String assignedStaff, List<VideoStatus> statuses);
    
    // Staff-related queries
    List<String> findDistinctAssignedStaff();
    List<String> findDistinctCreatedBy();
    
    // Salary calculations
    List<Video> findByAssignedStaffIgnoreCaseAndStatusInAndUpdatedAtBetween(
            String assignedStaff, List<VideoStatus> statuses, 
            LocalDateTime startOfDay, LocalDateTime endOfDay);
    
    List<SalesSalaryProjection> calculateSalesSalaryByDate(LocalDate targetDate);
    
    // Search with pagination
    Page<Video> findByCustomerNameContainingIgnoreCaseOrOrderCodeContainingIgnoreCaseAndStatus(
            String customerName, String orderCode, VideoStatus status, Pageable pageable);
}