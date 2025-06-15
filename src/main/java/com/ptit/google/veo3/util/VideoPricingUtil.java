package com.ptit.google.veo3.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Utility class để tính toán giá bán video dựa trên order_value
 * 
 * BUSINESS RULES:
 * - order_value = 15,000 → price = 30,000 (margin 100%)
 * - order_value = 45,000 → price = 150,000 (margin 233%)  
 * - order_value = 65,000 → price = 200,000 (margin 208%)
 * - order_value = 100,000 → price = 250,000 (margin 150%)
 * 
 * DESIGN PATTERN: Strategy Pattern
 * - Centralized pricing logic
 * - Easy to maintain and extend
 * - Consistent pricing across application
 * 
 * @author System
 * @since 1.5.0
 */
@Slf4j
public class VideoPricingUtil {

    /**
     * Mapping table cho việc tính giá bán dựa trên order_value
     * Key: order_value, Value: price
     * 
     * Sử dụng BigDecimal để đảm bảo precision cho financial calculations
     */
    private static final Map<BigDecimal, BigDecimal> PRICING_MAP = Map.of(
            new BigDecimal("20000.00"), new BigDecimal("30000.00"),
            new BigDecimal("45000.00"), new BigDecimal("170000.00"),
            new BigDecimal("65000.00"), new BigDecimal("230000.00"),
            new BigDecimal("900000.00"), new BigDecimal("290000.00"),
            new BigDecimal("110000.00"), new BigDecimal("350000.00")
    );

    /**
     * Tính giá bán video dựa trên order_value
     * 
     * @param orderValue Giá trị đơn hàng (cost)
     * @return Giá bán tương ứng, null nếu không có mapping
     */
    public static BigDecimal calculatePrice(BigDecimal orderValue) {
        if (orderValue == null) {
            log.warn("Order value is null, cannot calculate price");
            return null;
        }

        // Normalize orderValue để đảm bảo match chính xác
        BigDecimal normalizedOrderValue = orderValue.setScale(2, BigDecimal.ROUND_HALF_UP);
        
        BigDecimal price = PRICING_MAP.get(normalizedOrderValue);
        
        if (price != null) {
            log.debug("Calculated price {} for order value {}", price, normalizedOrderValue);
        } else {
            log.warn("No pricing rule found for order value: {}", normalizedOrderValue);
        }
        
        return price;
    }

    /**
     * Kiểm tra xem order_value có được support trong pricing rules không
     * 
     * @param orderValue Giá trị đơn hàng cần kiểm tra
     * @return true nếu có pricing rule tương ứng
     */
    public static boolean isSupportedOrderValue(BigDecimal orderValue) {
        if (orderValue == null) {
            return false;
        }
        
        BigDecimal normalizedOrderValue = orderValue.setScale(2, BigDecimal.ROUND_HALF_UP);
        return PRICING_MAP.containsKey(normalizedOrderValue);
    }

    /**
     * Tính profit margin dựa trên order_value và price
     * 
     * @param orderValue Cost của video
     * @param price Giá bán của video
     * @return Profit margin theo %, null nếu không thể tính
     */
    public static BigDecimal calculateProfitMargin(BigDecimal orderValue, BigDecimal price) {
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
     * Lấy tất cả pricing rules có sẵn
     * Hữu ích cho documentation hoặc admin dashboard
     * 
     * @return Map chứa tất cả pricing rules
     */
    public static Map<BigDecimal, BigDecimal> getAllPricingRules() {
        return Map.copyOf(PRICING_MAP);
    }

    /**
     * Validate pricing consistency
     * Kiểm tra xem price có match với order_value theo business rules không
     * 
     * @param orderValue Cost của video
     * @param price Giá bán của video
     * @return true nếu price đúng theo business rules
     */
    public static boolean validatePricing(BigDecimal orderValue, BigDecimal price) {
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
}
