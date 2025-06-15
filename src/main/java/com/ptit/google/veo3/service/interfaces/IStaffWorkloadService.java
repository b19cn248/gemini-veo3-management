package com.ptit.google.veo3.service.interfaces;

import com.ptit.google.veo3.dto.WorkloadInfo;

/**
 * Interface for Staff Workload Service operations
 * Following Interface Segregation Principle (ISP) and Dependency Inversion Principle (DIP)
 */
public interface IStaffWorkloadService {
    
    /**
     * Check if staff can accept new task
     * @param assignedStaff Staff name to check
     * @return true if can accept new task, false otherwise
     */
    boolean canAcceptNewTask(String assignedStaff);
    
    /**
     * Get current workload count for staff
     * @param assignedStaff Staff name
     * @return Number of active videos
     */
    long getCurrentWorkload(String assignedStaff);
    
    /**
     * Validate if staff can accept new task, throw exception if not
     * @param assignedStaff Staff name to validate
     * @throws RuntimeException if staff cannot accept new task
     */
    void validateCanAcceptNewTask(String assignedStaff);
    
    /**
     * Get detailed workload information for staff
     * @param assignedStaff Staff name
     * @return WorkloadInfo object with detailed information
     */
    WorkloadInfo getWorkloadInfo(String assignedStaff);
}