package com.ptit.google.veo3.service.interfaces;

import com.ptit.google.veo3.dto.StaffSalaryDto;
import com.ptit.google.veo3.dto.VideoRequestDto;
import com.ptit.google.veo3.dto.VideoResponseDto;
import com.ptit.google.veo3.dto.SalesSalaryDto;
import com.ptit.google.veo3.entity.VideoStatus;
import com.ptit.google.veo3.entity.DeliveryStatus;
import com.ptit.google.veo3.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interface for Video Service operations
 * Following Interface Segregation Principle (ISP) and Dependency Inversion Principle (DIP)
 */
public interface IVideoService {
    
    // Basic CRUD operations
    VideoResponseDto createVideo(VideoRequestDto requestDto);
    VideoResponseDto updateVideo(Long id, VideoRequestDto requestDto);
    VideoResponseDto getVideoById(Long id);
    void deleteVideo(Long id);
    
    // Assignment operations
    VideoResponseDto updateAssignedStaff(Long id, String assignedStaff);
    
    // Status update operations
    VideoResponseDto updateVideoStatus(Long id, String statusString);
    VideoResponseDto updateDeliveryStatus(Long id, String statusString);
    VideoResponseDto updatePaymentStatus(Long id, String statusString);
    VideoResponseDto cancelVideo(Long id);
    
    // URL operations
    VideoResponseDto updateVideoUrl(Long id, String videoUrl);
    
    // Query operations
    Page<VideoResponseDto> getAllVideos(int page, int size, String sortBy, String sortDirection,
                                       VideoStatus videoStatus, String assignedStaff, 
                                       DeliveryStatus deliveryStatus, 
                                       PaymentStatus paymentStatus,
                                       LocalDate fromPaymentDate, LocalDate toPaymentDate,
                                       LocalDate fromDateCreatedVideo, LocalDate toDateCreatedVideo,
                                       String createdBy);
    List<VideoResponseDto> getAllVideos();
    List<VideoResponseDto> searchByCustomerName(String customerName);
    List<VideoResponseDto> getVideosByStatus(String statusString);
    List<VideoResponseDto> searchByAssignedStaff(String assignedStaff);
    Page<VideoResponseDto> getVideosByDateRange(LocalDateTime startDate, LocalDateTime endDate,
                                               int page, int size, String sortBy, String sortDirection);
    List<VideoResponseDto> advancedSearch(String customerName, VideoStatus status,
                                         String assignedStaff, LocalDateTime startDate, LocalDateTime endDate);
    List<VideoResponseDto> filterVideos(String assignedStaff, String deliveryStatus, String paymentStatus);
    
    // Statistics operations
    Map<VideoStatus, Long> getVideoStatusStatistics();
    List<String> getDistinctAssignedStaff();
    List<String> getDistinctCreatedBy();
    
    // Salary calculations
    List<StaffSalaryDto> calculateStaffSalaries(LocalDate date);
    List<StaffSalaryDto> calculateStaffSalariesByDateRange(LocalDate startDate, LocalDate endDate);
    List<SalesSalaryDto> calculateSalesSalariesByDate(LocalDate targetDate);
    List<SalesSalaryDto> calculateSalesSalariesByDateRange(LocalDate startDate, LocalDate endDate);
    List<SalesSalaryDto> calculateSalesSalariesByDateRangeForCurrentUser(LocalDate startDate, LocalDate endDate, String currentSalesName);
    List<SalesSalaryDto> calculateSalesSalariesByDateForCurrentUser(LocalDate targetDate, String currentSalesName);
    
    // Validation operations
    boolean checkCustomerExists(String customerName);
    
    // Search operations
    List<VideoResponseDto> searchById(Long id);
    
    // Staff limit operations
    void setStaffLimit(String staffName, Integer lockDays);
    void setStaffLimit(String staffName, Integer lockDays, Integer maxOrdersPerDay);
    void removeStaffLimit(String staffName);
    List<Map<String, Object>> getActiveStaffLimits();
    boolean isStaffLimited(String staffName);
    Map<String, Object> getStaffQuotaInfo(String staffName);
}