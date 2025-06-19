package com.ptit.google.veo3.mapper;

import com.ptit.google.veo3.common.mapper.AbstractBaseMapper;
import com.ptit.google.veo3.dto.AuditLogResponseDto;
import com.ptit.google.veo3.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper extends AbstractBaseMapper<AuditLog, AuditLogResponseDto> {

    @Override
    public AuditLogResponseDto toDto(AuditLog entity) {
        if (entity == null) {
            return null;
        }
        
        AuditLogResponseDto dto = new AuditLogResponseDto();
        
        dto.setId(entity.getId());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setAction(entity.getAction());
        dto.setActionDescription(entity.getActionDescription());
        dto.setFieldName(entity.getFieldName());
        dto.setOldValue(entity.getOldValue());
        dto.setNewValue(entity.getNewValue());
        dto.setPerformedBy(entity.getPerformedBy());
        dto.setPerformedAt(entity.getPerformedAt());
        dto.setIpAddress(entity.getIpAddress());
        dto.setUserAgent(entity.getUserAgent());
        dto.setTenantId(entity.getTenantId());
        
        return dto;
    }

    @Override
    public AuditLog toEntity(AuditLogResponseDto dto) {
        if (dto == null) {
            return null;
        }
        
        AuditLog entity = new AuditLog();
        
        entity.setId(dto.getId());
        entity.setEntityType(dto.getEntityType());
        entity.setEntityId(dto.getEntityId());
        entity.setAction(dto.getAction());
        entity.setActionDescription(dto.getActionDescription());
        entity.setFieldName(dto.getFieldName());
        entity.setOldValue(dto.getOldValue());
        entity.setNewValue(dto.getNewValue());
        entity.setPerformedBy(dto.getPerformedBy());
        entity.setPerformedAt(dto.getPerformedAt());
        entity.setIpAddress(dto.getIpAddress());
        entity.setUserAgent(dto.getUserAgent());
        entity.setTenantId(dto.getTenantId());
        
        return entity;
    }

    @Override
    public void updateEntityFromDto(AuditLogResponseDto dto, AuditLog entity) {
        if (dto == null || entity == null) {
            return;
        }
        
        entity.setEntityType(dto.getEntityType());
        entity.setEntityId(dto.getEntityId());
        entity.setAction(dto.getAction());
        entity.setActionDescription(dto.getActionDescription());
        entity.setFieldName(dto.getFieldName());
        entity.setOldValue(dto.getOldValue());
        entity.setNewValue(dto.getNewValue());
        entity.setPerformedBy(dto.getPerformedBy());
        entity.setIpAddress(dto.getIpAddress());
        entity.setUserAgent(dto.getUserAgent());
        entity.setTenantId(dto.getTenantId());
    }
}