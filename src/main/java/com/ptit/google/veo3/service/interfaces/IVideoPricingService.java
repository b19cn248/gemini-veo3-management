package com.ptit.google.veo3.service.interfaces;

import java.math.BigDecimal;

/**
 * Service interface for video pricing calculations
 * 
 * Handles all pricing-related business logic including:
 * - Order value calculation based on video duration
 * - Price calculation based on order value  
 * - Pricing rule validation
 * 
 * This interface follows Single Responsibility Principle by separating
 * pricing concerns from other video management operations.
 */
public interface IVideoPricingService {
    
    /**
     * Calculate order value based on video duration
     * 
     * @param videoDuration Duration of video in seconds
     * @return Order value corresponding to the duration
     * @throws IllegalArgumentException if duration is invalid
     */
    BigDecimal calculateOrderValue(Integer videoDuration);
    
    /**
     * Calculate price based on order value using pricing rules
     * 
     * @param orderValue The order value to calculate price for
     * @return Calculated price, or null if no pricing rule exists
     */
    BigDecimal calculatePrice(BigDecimal orderValue);
    
    /**
     * Calculate both order value and price for a video duration
     * 
     * @param videoDuration Duration of video in seconds
     * @return Array containing [orderValue, price], price may be null
     * @throws IllegalArgumentException if duration is invalid
     */
    BigDecimal[] calculateOrderValueAndPrice(Integer videoDuration);
    
    /**
     * Validate if a pricing rule exists for the given order value
     * 
     * @param orderValue The order value to check
     * @return true if pricing rule exists, false otherwise
     */
    boolean hasPricingRule(BigDecimal orderValue);
    
    /**
     * Get all available video durations that have pricing rules
     * 
     * @return Array of supported video durations in seconds
     */
    Integer[] getSupportedDurations();
}