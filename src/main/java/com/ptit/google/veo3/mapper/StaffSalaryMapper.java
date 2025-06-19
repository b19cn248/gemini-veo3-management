package com.ptit.google.veo3.mapper;

import com.ptit.google.veo3.dto.StaffSalaryDto;
import com.ptit.google.veo3.entity.Video;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StaffSalaryMapper {

    public StaffSalaryDto buildStaffSalaryDto(String staffName, 
                                             Long totalVideos, 
                                             BigDecimal totalSalary) {
        if (staffName == null) {
            return null;
        }
        
        StaffSalaryDto dto = new StaffSalaryDto();
        
        dto.setStaffName(staffName);
        dto.setTotalVideos(totalVideos != null ? totalVideos : 0L);
        dto.setTotalSalary(totalSalary != null ? totalSalary : BigDecimal.ZERO);
        
        return dto;
    }
    
    public StaffSalaryDto buildStaffSalaryFromVideos(String staffName, List<Video> videos) {
        if (staffName == null) {
            return buildStaffSalaryDto(staffName, 0L, BigDecimal.ZERO);
        }
        
        if (videos == null || videos.isEmpty()) {
            return buildStaffSalaryDto(staffName, 0L, BigDecimal.ZERO);
        }
        
        long totalVideos = videos.size();
        BigDecimal totalSalary = videos.stream()
                .map(Video::getPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return buildStaffSalaryDto(staffName, totalVideos, totalSalary);
    }
    
    public List<StaffSalaryDto> buildStaffSalaryListFromVideoMap(Map<String, List<Video>> videosByStaff) {
        if (videosByStaff == null) {
            return List.of();
        }
        
        return videosByStaff.entrySet().stream()
                .map(entry -> buildStaffSalaryFromVideos(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
    
    public List<StaffSalaryDto> buildStaffSalaryList(Map<String, Long> videoCountByStaff, 
                                                    Map<String, BigDecimal> salaryByStaff) {
        if (videoCountByStaff == null) {
            return List.of();
        }
        
        return videoCountByStaff.entrySet().stream()
                .map(entry -> {
                    String staffName = entry.getKey();
                    Long videoCount = entry.getValue();
                    BigDecimal salary = salaryByStaff != null ? 
                            salaryByStaff.getOrDefault(staffName, BigDecimal.ZERO) : BigDecimal.ZERO;
                    
                    return buildStaffSalaryDto(staffName, videoCount, salary);
                })
                .collect(Collectors.toList());
    }
    
    public StaffSalaryDto createEmptyStaffSalary(String staffName) {
        return buildStaffSalaryDto(staffName, 0L, BigDecimal.ZERO);
    }
}