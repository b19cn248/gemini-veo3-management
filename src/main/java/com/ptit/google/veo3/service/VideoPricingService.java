package com.ptit.google.veo3.service;

import com.ptit.google.veo3.service.interfaces.IVideoPricingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service implementation for video pricing calculations
 * 
 * This service encapsulates all pricing-related business logic and follows
 * Single Responsibility Principle by handling only pricing concerns.
 * 
 * Business Rules:
 * - Video duration determines order value
 * - Order value maps to selling price via pricing rules
 * - Pricing rules are centralized and easily maintainable
 * 
 * @author System
 * @since 2.0.0
 */
@Service
@Slf4j
public class VideoPricingService implements IVideoPricingService {

    /**
     * Mapping table for video duration to order value
     * Key: video duration in seconds, Value: order value
     */
    private static final Map<Integer, BigDecimal> DURATION_TO_ORDER_VALUE = Map.of(
            8, new BigDecimal("20000.00"),
            16, new BigDecimal("45000.00"),
            24, new BigDecimal("65000.00"),
            32, new BigDecimal("90000.00"),
            40, new BigDecimal("110000.00")
    );

    /**
     * Mapping table for order value to selling price
     * Key: order_value, Value: price
     */
    private static final Map<BigDecimal, BigDecimal> PRICING_MAP = Map.of(
            new BigDecimal("20000.00"), new BigDecimal("30000.00"),
            new BigDecimal("45000.00"), new BigDecimal("170000.00"),
            new BigDecimal("65000.00"), new BigDecimal("230000.00"),
            new BigDecimal("90000.00"), new BigDecimal("290000.00"),
            new BigDecimal("110000.00"), new BigDecimal("350000.00")
    );

    @Override
    public BigDecimal calculateOrderValue(Integer videoDuration) {
        if (videoDuration == null) {
            throw new IllegalArgumentException("Video duration cannot be null");
        }

        BigDecimal orderValue = DURATION_TO_ORDER_VALUE.get(videoDuration);
        
        if (orderValue == null) {
            log.warn("No order value mapping found for duration: {} seconds", videoDuration);
            throw new IllegalArgumentException("Unsupported video duration: " + videoDuration + 
                    " seconds. Supported durations: " + getSupportedDurations());
        }

        log.debug("Calculated order value {} for duration {} seconds", orderValue, videoDuration);
        return orderValue;
    }

    @Override
    public BigDecimal calculatePrice(BigDecimal orderValue) {
        if (orderValue == null) {
            log.warn("Order value is null, cannot calculate price");
            return null;
        }

        // Normalize orderValue to ensure exact match
        BigDecimal normalizedOrderValue = orderValue.setScale(2, BigDecimal.ROUND_HALF_UP);
        
        BigDecimal price = PRICING_MAP.get(normalizedOrderValue);
        
        if (price != null) {
            log.debug("Calculated price {} for order value {}", price, normalizedOrderValue);
        } else {
            log.warn("No pricing rule found for order value: {}", normalizedOrderValue);
        }
        
        return price;
    }

    @Override
    public BigDecimal[] calculateOrderValueAndPrice(Integer videoDuration) {
        BigDecimal orderValue = calculateOrderValue(videoDuration);
        BigDecimal price = calculatePrice(orderValue);
        
        log.debug("Calculated for duration {}: orderValue={}, price={}", 
                videoDuration, orderValue, price);
        
        return new BigDecimal[]{orderValue, price};
    }

    @Override
    public boolean hasPricingRule(BigDecimal orderValue) {
        if (orderValue == null) {
            return false;
        }
        
        BigDecimal normalizedOrderValue = orderValue.setScale(2, BigDecimal.ROUND_HALF_UP);
        return PRICING_MAP.containsKey(normalizedOrderValue);
    }

    @Override
    public Integer[] getSupportedDurations() {
        return DURATION_TO_ORDER_VALUE.keySet().toArray(new Integer[0]);
    }

    /**
     * Validate pricing consistency
     * Checks if price matches order_value according to business rules
     * 
     * @param orderValue Cost of video
     * @param price Selling price of video
     * @return true if price is correct according to business rules
     */
    public boolean validatePricing(BigDecimal orderValue, BigDecimal price) {
        if (orderValue == null || price == null) {
            return false;
        }

        BigDecimal expectedPrice = calculatePrice(orderValue);
        boolean isValid = expectedPrice != null && expectedPrice.compareTo(price) == 0;
        
        if (!isValid) {
            log.warn("Pricing validation failed: expected {} for order value {}, but got {}", 
                    expectedPrice, orderValue, price);
        }
        
        return isValid;
    }

    /**
     * Calculate profit margin based on order_value and price
     * 
     * @param orderValue Cost of video
     * @param price Selling price of video
     * @return Profit margin as percentage, null if cannot calculate
     */
    public BigDecimal calculateProfitMargin(BigDecimal orderValue, BigDecimal price) {
        if (orderValue == null || price == null || orderValue.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Cannot calculate profit margin: orderValue={}, price={}", orderValue, price);
            return null;
        }

        // Profit margin = ((price - cost) / cost) * 100
        BigDecimal profit = price.subtract(orderValue);
        BigDecimal margin = profit.divide(orderValue, 2, BigDecimal.ROUND_HALF_UP)
                                  .multiply(new BigDecimal("100"));
        
        log.debug("Profit margin {}% for cost {} and price {}", margin, orderValue, price);
        return margin;
    }

    /**
     * Get all available pricing rules
     * Useful for documentation or admin dashboard
     * 
     * @return Map containing all pricing rules
     */
    public Map<BigDecimal, BigDecimal> getAllPricingRules() {
        return Map.copyOf(PRICING_MAP);
    }
}