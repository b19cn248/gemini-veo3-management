package com.ptit.google.veo3.aspect;

import com.ptit.google.veo3.entity.BaseEntity;
import com.ptit.google.veo3.entity.Video;
import com.ptit.google.veo3.repository.VideoRepository;
import com.ptit.google.veo3.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Aspect để tự động track audit logs cho entity operations
 * 
 * Chức năng:
 * - Automatic audit logging cho tất cả CRUD operations
 * - Capture before/after state để detect changes
 * - Non-intrusive - không cần modify existing service code
 * - Error handling để đảm bảo main operation không bị ảnh hưởng
 * 
 * Tuân thủ SOLID Principles:
 * - Single Responsibility: Chỉ handle audit logging
 * - Open/Closed: Có thể extend cho new entities mà không sửa code
 * - Dependency Inversion: Depend on service abstractions
 * 
 * AOP Best Practices:
 * - Non-intrusive design
 * - Error isolation (audit failure không làm fail main operation)
 * - Performance optimization (async processing)
 * - Clear pointcut definitions
 * 
 * Performance Considerations:
 * - Minimal overhead trên main operations
 * - Async audit processing
 * - Efficient change detection
 * - Error handling để tránh blocking
 * 
 * @author System
 * @since 1.6.0
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {
    
    private final AuditService auditService;
    private final VideoRepository videoRepository;
    
    // ============= POINTCUT DEFINITIONS =============
    
    /**
     * Pointcut cho Video service CREATE operations
     * Match: VideoService.createVideo() method
     */
    @Pointcut("execution(* com.ptit.google.veo3.service.VideoService.createVideo(..))")
    public void videoCreateOperation() {}
    
    /**
     * Pointcut cho Video service UPDATE operations  
     * Match: VideoService.updateVideo() và similar methods
     */
    @Pointcut("execution(* com.ptit.google.veo3.service.VideoService.updateVideo(..))")
    public void videoUpdateOperation() {}
    
    /**
     * Pointcut cho Video service DELETE operations
     * Match: VideoService.deleteVideo() method
     */
    @Pointcut("execution(* com.ptit.google.veo3.service.VideoService.deleteVideo(..))")
    public void videoDeleteOperation() {}
    
    /**
     * Pointcut cho tất cả Video repository save operations
     * Match: VideoRepository.save() method calls
     */
    @Pointcut("execution(* com.ptit.google.veo3.repository.VideoRepository.save(..))")
    public void videoRepositorySave() {}
    
    /**
     * Pointcut cho tất cả Video repository delete operations
     * Match: VideoRepository.delete*() method calls
     */
    @Pointcut("execution(* com.ptit.google.veo3.repository.VideoRepository.delete*(..))")
    public void videoRepositoryDelete() {}
    
    // ============= CREATE OPERATION ADVICE =============
    
    /**
     * After advice cho video creation
     * Log entity creation sau khi video được tạo thành công
     * 
     * @param joinPoint Join point information
     * @param result Video entity vừa được tạo
     */
    @AfterReturning(pointcut = "videoCreateOperation()", returning = "result")
    public void auditVideoCreation(JoinPoint joinPoint, Object result) {
        try {
            if (result instanceof Video) {
                Video video = (Video) result;
                log.debug("Auditing video creation for ID: {}", video.getId());
                
                // Async audit logging để không ảnh hưởng performance
                auditService.logEntityCreation(video)
                    .exceptionally(throwable -> {
                        log.error("Failed to audit video creation for ID {}: {}", 
                            video.getId(), throwable.getMessage());
                        return null;
                    });
            }
        } catch (Exception e) {
            // Error isolation - audit failure không được ảnh hưởng main operation
            log.error("Audit aspect error in video creation: {}", e.getMessage(), e);
        }
    }
    
    // ============= UPDATE OPERATION ADVICE =============
    
    /**
     * Around advice cho video update operations
     * Capture before/after state để detect changes
     * 
     * @param proceedingJoinPoint Proceeding join point
     * @return Result từ original method
     * @throws Throwable Exception từ original method
     */
    @Around("videoUpdateOperation()")
    public Object auditVideoUpdate(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Video oldVideo = null;
        Object[] args = proceedingJoinPoint.getArgs();
        
        try {
            // Capture old state trước khi update
            if (args.length > 0 && args[0] instanceof Long) {
                Long videoId = (Long) args[0];
                Optional<Video> optionalVideo = videoRepository.findById(videoId);
                if (optionalVideo.isPresent()) {
                    oldVideo = cloneVideo(optionalVideo.get());
                    log.debug("Captured old state for video ID: {}", videoId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to capture old video state: {}", e.getMessage());
        }
        
        // Execute original method
        Object result = proceedingJoinPoint.proceed();
        
        try {
            // Audit after successful update
            if (result instanceof Video && oldVideo != null) {
                Video newVideo = (Video) result;
                log.debug("Auditing video update for ID: {}", newVideo.getId());
                
                // Async audit logging
                auditService.logEntityUpdate(oldVideo, newVideo)
                    .exceptionally(throwable -> {
                        log.error("Failed to audit video update for ID {}: {}", 
                            newVideo.getId(), throwable.getMessage());
                        return null;
                    });
            }
        } catch (Exception e) {
            // Error isolation
            log.error("Audit aspect error in video update: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    // ============= DELETE OPERATION ADVICE =============
    
    /**
     * Before advice cho video deletion
     * Capture entity state trước khi bị xóa
     * 
     * @param joinPoint Join point information
     */
    @Before("videoDeleteOperation()")
    public void auditVideoDeletion(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            
            if (args.length > 0 && args[0] instanceof Long) {
                Long videoId = (Long) args[0];
                Optional<Video> optionalVideo = videoRepository.findById(videoId);
                
                if (optionalVideo.isPresent()) {
                    Video video = optionalVideo.get();
                    log.debug("Auditing video deletion for ID: {}", videoId);
                    
                    // Async audit logging
                    auditService.logEntityDeletion(video)
                        .exceptionally(throwable -> {
                            log.error("Failed to audit video deletion for ID {}: {}", 
                                videoId, throwable.getMessage());
                            return null;
                        });
                }
            }
        } catch (Exception e) {
            // Error isolation
            log.error("Audit aspect error in video deletion: {}", e.getMessage(), e);
        }
    }
    
    // ============= REPOSITORY LEVEL AUDITING =============
    
    /**
     * Around advice cho repository save operations
     * Backup approach để capture repository-level changes
     * 
     * @param proceedingJoinPoint Proceeding join point
     * @return Result từ repository save
     * @throws Throwable Exception từ repository
     */
    @Around("videoRepositorySave()")
    public Object auditRepositorySave(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object[] args = proceedingJoinPoint.getArgs();
        Video oldVideo = null;
        
        try {
            // Check if this is an update (entity has ID)
            if (args.length > 0 && args[0] instanceof Video) {
                Video video = (Video) args[0];
                if (video.getId() != null) {
                    // This is an update - capture old state
                    Optional<Video> optionalOldVideo = videoRepository.findById(video.getId());
                    if (optionalOldVideo.isPresent()) {
                        oldVideo = cloneVideo(optionalOldVideo.get());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to capture old state in repository save: {}", e.getMessage());
        }
        
        // Execute original save
        Object result = proceedingJoinPoint.proceed();
        
        try {
            if (result instanceof Video) {
                Video savedVideo = (Video) result;
                
                if (oldVideo == null) {
                    // This was a CREATE operation
                    log.debug("Repository-level audit: Video created with ID: {}", savedVideo.getId());
                    
                    auditService.logEntityCreation(savedVideo)
                        .exceptionally(throwable -> {
                            log.error("Failed to audit repository video creation: {}", throwable.getMessage());
                            return null;
                        });
                } else {
                    // This was an UPDATE operation
                    log.debug("Repository-level audit: Video updated with ID: {}", savedVideo.getId());
                    
                    auditService.logEntityUpdate(oldVideo, savedVideo)
                        .exceptionally(throwable -> {
                            log.error("Failed to audit repository video update: {}", throwable.getMessage());
                            return null;
                        });
                }
            }
        } catch (Exception e) {
            // Error isolation
            log.error("Audit aspect error in repository save: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    // ============= EXCEPTION HANDLING =============
    
    /**
     * After throwing advice để log audit khi có exception
     * Track failed operations cho security và debugging
     * 
     * @param joinPoint Join point information
     * @param exception Exception đã xảy ra
     */
    @AfterThrowing(pointcut = "videoCreateOperation() || videoUpdateOperation() || videoDeleteOperation()", 
                   throwing = "exception")
    public void auditVideoOperationException(JoinPoint joinPoint, Exception exception) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            
            log.warn("Video operation failed - Method: {}, Args: {}, Exception: {}", 
                methodName, args, exception.getMessage());
            
            // Có thể log failed operations để security monitoring
            // auditService.logFailedOperation(methodName, args, exception);
            
        } catch (Exception e) {
            log.error("Failed to audit exception: {}", e.getMessage(), e);
        }
    }
    
    // ============= UTILITY METHODS =============
    
    /**
     * Clone Video entity để capture state
     * Tránh reference issues khi so sánh old/new state
     * 
     * @param original Original video entity
     * @return Cloned video entity
     */
    private Video cloneVideo(Video original) {
        try {
            return Video.builder()
                .id(original.getId())
                .customerName(original.getCustomerName())
                .videoContent(original.getVideoContent())
                .imageUrl(original.getImageUrl())
                .videoDuration(original.getVideoDuration())
                .deliveryTime(original.getDeliveryTime())
                .assignedAt(original.getAssignedAt())
                .assignedStaff(original.getAssignedStaff())
                .status(original.getStatus())
                .deliveryStatus(original.getDeliveryStatus())
                .paymentStatus(original.getPaymentStatus())
                .videoUrl(original.getVideoUrl())
                .completedTime(original.getCompletedTime())
                .customerApproved(original.getCustomerApproved())
                .customerNote(original.getCustomerNote())
                .checked(original.getChecked())
                .paymentDate(original.getPaymentDate())
                .orderValue(original.getOrderValue())
                .price(original.getPrice())
                .build();
        } catch (Exception e) {
            log.warn("Failed to clone video entity: {}", e.getMessage());
            return original; // Fallback to original reference
        }
    }
    
    /**
     * Check if entity is auditable
     * Extensible method để support thêm entity types trong tương lai
     * 
     * @param entity Entity to check
     * @return true if entity should be audited
     */
    private boolean isAuditableEntity(Object entity) {
        return entity instanceof BaseEntity;
    }
    
    /**
     * Extract entity ID từ method arguments
     * Helper method để get entity ID từ các method signatures khác nhau
     * 
     * @param args Method arguments
     * @return Entity ID hoặc null
     */
    private Long extractEntityId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
            if (arg instanceof BaseEntity) {
                return ((BaseEntity) arg).getId();
            }
        }
        return null;
    }
    
    /**
     * Log aspect performance metrics
     * Monitor aspect overhead để ensure performance
     */
    private void logAspectPerformance(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 100) { // Log nếu audit takes > 100ms
            log.warn("Audit aspect performance warning - Operation: {}, Duration: {}ms", 
                operation, duration);
        } else {
            log.debug("Audit aspect completed - Operation: {}, Duration: {}ms", 
                operation, duration);
        }
    }
}