package com.ptit.google.veo3.mapper;

import com.ptit.google.veo3.common.mapper.AbstractBaseMapper;
import com.ptit.google.veo3.dto.VideoRequestDto;
import com.ptit.google.veo3.dto.VideoResponseDto;
import com.ptit.google.veo3.entity.Video;
import org.springframework.stereotype.Component;

@Component
public class VideoMapper extends AbstractBaseMapper<Video, VideoResponseDto> {

    @Override
    public VideoResponseDto toDto(Video entity) {
        if (entity == null) {
            return null;
        }
        
        VideoResponseDto dto = new VideoResponseDto();
        
        dto.setId(entity.getId());
        dto.setCustomerName(entity.getCustomerName());
        dto.setVideoContent(entity.getVideoContent());
        dto.setImageUrl(entity.getImageUrl());
        dto.setVideoDuration(entity.getVideoDuration());
        dto.setDeliveryTime(entity.getDeliveryTime());
        dto.setAssignedStaff(entity.getAssignedStaff());
        dto.setAssignedAt(entity.getAssignedAt());
        dto.setStatus(entity.getStatus());
        dto.setVideoUrl(entity.getVideoUrl());
        dto.setCompletedTime(entity.getCompletedTime());
        dto.setCustomerApproved(entity.getCustomerApproved());
        dto.setCustomerNote(entity.getCustomerNote());
        dto.setChecked(entity.getChecked());
        dto.setDeliveryStatus(entity.getDeliveryStatus());
        dto.setPaymentStatus(entity.getPaymentStatus());
        dto.setPaymentDate(entity.getPaymentDate());
        dto.setOrderValue(entity.getOrderValue());
        dto.setPrice(entity.getPrice());
        dto.setBillImageUrl(entity.getBillImageUrl());
        dto.setLinkfb(entity.getLinkfb());
        dto.setPhoneNumber(entity.getPhoneNumber());
        
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        
        return dto;
    }

    @Override
    public Video toEntity(VideoResponseDto dto) {
        if (dto == null) {
            return null;
        }
        
        Video entity = new Video();
        
        entity.setId(dto.getId());
        entity.setCustomerName(dto.getCustomerName());
        entity.setVideoContent(dto.getVideoContent());
        entity.setImageUrl(dto.getImageUrl());
        entity.setVideoDuration(dto.getVideoDuration());
        entity.setDeliveryTime(dto.getDeliveryTime());
        entity.setAssignedStaff(dto.getAssignedStaff());
        entity.setAssignedAt(dto.getAssignedAt());
        entity.setStatus(dto.getStatus());
        entity.setVideoUrl(dto.getVideoUrl());
        entity.setCompletedTime(dto.getCompletedTime());
        entity.setCustomerApproved(dto.getCustomerApproved());
        entity.setCustomerNote(dto.getCustomerNote());
        entity.setChecked(dto.getChecked());
        entity.setDeliveryStatus(dto.getDeliveryStatus());
        entity.setPaymentStatus(dto.getPaymentStatus());
        entity.setPaymentDate(dto.getPaymentDate());
        entity.setOrderValue(dto.getOrderValue());
        entity.setPrice(dto.getPrice());
        entity.setBillImageUrl(dto.getBillImageUrl());
        entity.setLinkfb(dto.getLinkfb());
        entity.setPhoneNumber(dto.getPhoneNumber());
        
        return entity;
    }

    @Override
    public void updateEntityFromDto(VideoResponseDto dto, Video entity) {
        if (dto == null || entity == null) {
            return;
        }
        
        entity.setCustomerName(dto.getCustomerName());
        entity.setVideoContent(dto.getVideoContent());
        entity.setImageUrl(dto.getImageUrl());
        entity.setVideoDuration(dto.getVideoDuration());
        entity.setDeliveryTime(dto.getDeliveryTime());
        entity.setVideoUrl(dto.getVideoUrl());
        entity.setCompletedTime(dto.getCompletedTime());
        entity.setCustomerApproved(dto.getCustomerApproved());
        entity.setCustomerNote(dto.getCustomerNote());
        entity.setChecked(dto.getChecked());
        entity.setDeliveryStatus(dto.getDeliveryStatus());
        entity.setPaymentStatus(dto.getPaymentStatus());
        entity.setPaymentDate(dto.getPaymentDate());
        entity.setOrderValue(dto.getOrderValue());
        entity.setPrice(dto.getPrice());
        entity.setBillImageUrl(dto.getBillImageUrl());
        entity.setLinkfb(dto.getLinkfb());
        entity.setPhoneNumber(dto.getPhoneNumber());
    }

    public Video fromRequestDto(VideoRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }
        
        Video entity = new Video();
        
        entity.setCustomerName(requestDto.getCustomerName());
        entity.setVideoContent(requestDto.getVideoContent());
        entity.setImageUrl(requestDto.getImageUrl());
        entity.setVideoDuration(requestDto.getVideoDuration());
        entity.setDeliveryTime(requestDto.getDeliveryTime());
        entity.setAssignedStaff(requestDto.getAssignedStaff());
        entity.setStatus(requestDto.getStatus());
        entity.setVideoUrl(requestDto.getVideoUrl());
        entity.setCompletedTime(requestDto.getCompletedTime());
        entity.setCustomerApproved(requestDto.getCustomerApproved());
        entity.setCustomerNote(requestDto.getCustomerNote());
        entity.setChecked(requestDto.getChecked());
        entity.setDeliveryStatus(requestDto.getDeliveryStatus());
        entity.setPaymentStatus(requestDto.getPaymentStatus());
        entity.setPaymentDate(requestDto.getPaymentDate());
        entity.setOrderValue(requestDto.getOrderValue());
        entity.setPrice(requestDto.getPrice());
        entity.setBillImageUrl(requestDto.getBillImageUrl());
        entity.setLinkfb(requestDto.getLinkfb());
        entity.setPhoneNumber(requestDto.getPhoneNumber());
        
        return entity;
    }

    public void updateEntityFromRequestDto(VideoRequestDto requestDto, Video entity) {
        if (requestDto == null || entity == null) {
            return;
        }
        
        entity.setCustomerName(requestDto.getCustomerName());
        entity.setVideoContent(requestDto.getVideoContent());
        entity.setImageUrl(requestDto.getImageUrl());
        entity.setVideoDuration(requestDto.getVideoDuration());
        entity.setDeliveryTime(requestDto.getDeliveryTime());
        entity.setVideoUrl(requestDto.getVideoUrl());
        entity.setCompletedTime(requestDto.getCompletedTime());
        entity.setCustomerApproved(requestDto.getCustomerApproved());
        entity.setCustomerNote(requestDto.getCustomerNote());
        entity.setChecked(requestDto.getChecked());
        entity.setDeliveryStatus(requestDto.getDeliveryStatus());
        entity.setPaymentStatus(requestDto.getPaymentStatus());
        entity.setPaymentDate(requestDto.getPaymentDate());
        entity.setOrderValue(requestDto.getOrderValue());
        entity.setPrice(requestDto.getPrice());
        entity.setBillImageUrl(requestDto.getBillImageUrl());
        entity.setLinkfb(requestDto.getLinkfb());
        entity.setPhoneNumber(requestDto.getPhoneNumber());
    }
}