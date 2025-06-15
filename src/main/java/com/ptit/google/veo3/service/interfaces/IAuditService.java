package com.ptit.google.veo3.service.interfaces;

import com.ptit.google.veo3.entity.AuditAction;
import com.ptit.google.veo3.entity.AuditLog;
import com.ptit.google.veo3.entity.BaseEntity;
import com.ptit.google.veo3.entity.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Audit Service operations
 * Following Interface Segregation Principle (ISP) and Dependency Inversion Principle (DIP)
 */
public interface IAuditService {
    
    // Entity audit operations
    <T extends BaseEntity> CompletableFuture<AuditLog> logEntityCreation(T entity);
    <T extends BaseEntity> CompletableFuture<List<AuditLog>> logEntityUpdate(T oldEntity, T newEntity);
    <T extends BaseEntity> CompletableFuture<AuditLog> logEntityDeletion(T entity);
    
    // Business action audit operations
    CompletableFuture<AuditLog> logVideoBusinessAction(
        Long videoId,
        AuditAction action,
        String description,
        String fieldName,
        Object oldValue,
        Object newValue
    );
    
    // Query operations
    List<AuditLog> getVideoHistory(Long videoId);
    Page<AuditLog> getVideoHistoryPaged(Long videoId, Pageable pageable);
    Page<AuditLog> searchAuditLogs(
        EntityType entityType,
        AuditAction action,
        String performedBy,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        Pageable pageable
    );
}