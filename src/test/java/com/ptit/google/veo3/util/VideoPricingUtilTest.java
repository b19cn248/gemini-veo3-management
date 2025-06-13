package com.ptit.google.veo3.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho VideoPricingUtil
 * Test các business rules và edge cases
 */
class VideoPricingUtilTest {

    @ParameterizedTest
    @CsvSource({
            "15000.00, 30000.00",
            "45000.00, 150000.00", 
            "65000.00, 200000.00",
            "100000.00, 250000.00"
    })
    void testCalculatePrice_WithValidOrderValues_ShouldReturnCorrectPrice(String orderValueStr, String expectedPriceStr) {
        // Given
        BigDecimal orderValue = new BigDecimal(orderValueStr);
        BigDecimal expectedPrice = new BigDecimal(expectedPriceStr);

        // When
        BigDecimal actualPrice = VideoPricingUtil.calculatePrice(orderValue);

        // Then
        assertNotNull(actualPrice);
        assertEquals(0, expectedPrice.compareTo(actualPrice), 
                String.format("Expected %s but got %s for order value %s", expectedPrice, actualPrice, orderValue));
    }

    @Test
    void testCalculatePrice_WithNullOrderValue_ShouldReturnNull() {
        // When
        BigDecimal result = VideoPricingUtil.calculatePrice(null);

        // Then
        assertNull(result);
    }

    @Test
    void testCalculatePrice_WithUnsupportedOrderValue_ShouldReturnNull() {
        // Given
        BigDecimal unsupportedOrderValue = new BigDecimal("75000.00");

        // When
        BigDecimal result = VideoPricingUtil.calculatePrice(unsupportedOrderValue);

        // Then
        assertNull(result);
    }

    @Test
    void testCalculatePrice_WithDifferentScale_ShouldNormalizeAndMatch() {
        // Given - order value với scale khác nhau
        BigDecimal orderValue1 = new BigDecimal("15000"); // scale 0
        BigDecimal orderValue2 = new BigDecimal("15000.0"); // scale 1
        BigDecimal orderValue3 = new BigDecimal("15000.00"); // scale 2

        // When
        BigDecimal price1 = VideoPricingUtil.calculatePrice(orderValue1);
        BigDecimal price2 = VideoPricingUtil.calculatePrice(orderValue2);
        BigDecimal price3 = VideoPricingUtil.calculatePrice(orderValue3);

        // Then - tất cả nên trả về cùng price
        assertNotNull(price1);
        assertNotNull(price2);
        assertNotNull(price3);
        assertEquals(0, price1.compareTo(price2));
        assertEquals(0, price2.compareTo(price3));
        assertEquals(0, new BigDecimal("30000.00").compareTo(price1));
    }

    @ParameterizedTest
    @CsvSource({
            "15000.00, true",
            "45000.00, true",
            "65000.00, true", 
            "100000.00, true",
            "75000.00, false",
            "0.00, false"
    })
    void testIsSupportedOrderValue(String orderValueStr, boolean expectedSupported) {
        // Given
        BigDecimal orderValue = new BigDecimal(orderValueStr);

        // When
        boolean isSupported = VideoPricingUtil.isSupportedOrderValue(orderValue);

        // Then
        assertEquals(expectedSupported, isSupported);
    }

    @Test
    void testIsSupportedOrderValue_WithNull_ShouldReturnFalse() {
        // When
        boolean result = VideoPricingUtil.isSupportedOrderValue(null);

        // Then
        assertFalse(result);
    }

    @ParameterizedTest
    @CsvSource({
            "15000.00, 30000.00, 100.00", // (30000-15000)/15000 * 100 = 100%
            "45000.00, 150000.00, 233.33", // (150000-45000)/45000 * 100 = 233.33%
            "65000.00, 200000.00, 207.69", // (200000-65000)/65000 * 100 = 207.69%
            "100000.00, 250000.00, 150.00" // (250000-100000)/100000 * 100 = 150%
    })
    void testCalculateProfitMargin_WithValidValues_ShouldReturnCorrectMargin(
            String orderValueStr, String priceStr, String expectedMarginStr) {
        // Given
        BigDecimal orderValue = new BigDecimal(orderValueStr);
        BigDecimal price = new BigDecimal(priceStr);
        BigDecimal expectedMargin = new BigDecimal(expectedMarginStr);

        // When
        BigDecimal actualMargin = VideoPricingUtil.calculateProfitMargin(orderValue, price);

        // Then
        assertNotNull(actualMargin);
        assertEquals(0, expectedMargin.compareTo(actualMargin), 
                String.format("Expected margin %s%% but got %s%% for cost %s and price %s", 
                        expectedMargin, actualMargin, orderValue, price));
    }

    @Test
    void testCalculateProfitMargin_WithNullValues_ShouldReturnNull() {
        // Test null orderValue
        assertNull(VideoPricingUtil.calculateProfitMargin(null, new BigDecimal("100")));
        
        // Test null price
        assertNull(VideoPricingUtil.calculateProfitMargin(new BigDecimal("100"), null));
        
        // Test both null
        assertNull(VideoPricingUtil.calculateProfitMargin(null, null));
    }

    @Test
    void testCalculateProfitMargin_WithZeroOrderValue_ShouldReturnNull() {
        // Given
        BigDecimal zeroOrderValue = BigDecimal.ZERO;
        BigDecimal price = new BigDecimal("100.00");

        // When
        BigDecimal result = VideoPricingUtil.calculateProfitMargin(zeroOrderValue, price);

        // Then
        assertNull(result);
    }

    @Test
    void testGetAllPricingRules_ShouldReturnImmutableCopy() {
        // When
        Map<BigDecimal, BigDecimal> rules = VideoPricingUtil.getAllPricingRules();

        // Then
        assertNotNull(rules);
        assertEquals(4, rules.size()); // 4 pricing rules
        
        // Verify it's immutable
        assertThrows(UnsupportedOperationException.class, () -> {
            rules.put(new BigDecimal("1000"), new BigDecimal("2000"));
        });
    }

    @Test
    void testGetAllPricingRules_ShouldContainExpectedRules() {
        // When
        Map<BigDecimal, BigDecimal> rules = VideoPricingUtil.getAllPricingRules();

        // Then
        assertEquals(new BigDecimal("30000.00"), rules.get(new BigDecimal("15000.00")));
        assertEquals(new BigDecimal("150000.00"), rules.get(new BigDecimal("45000.00")));
        assertEquals(new BigDecimal("200000.00"), rules.get(new BigDecimal("65000.00")));
        assertEquals(new BigDecimal("250000.00"), rules.get(new BigDecimal("100000.00")));
    }

    @ParameterizedTest
    @CsvSource({
            "15000.00, 30000.00, true",
            "45000.00, 150000.00, true",
            "65000.00, 200000.00, true",
            "100000.00, 250000.00, true",
            "15000.00, 25000.00, false", // Wrong price
            "75000.00, 200000.00, false", // Unsupported order value
            "45000.00, 100000.00, false" // Wrong price for valid order value
    })
    void testValidatePricing(String orderValueStr, String priceStr, boolean expectedValid) {
        // Given
        BigDecimal orderValue = new BigDecimal(orderValueStr);
        BigDecimal price = new BigDecimal(priceStr);

        // When
        boolean isValid = VideoPricingUtil.validatePricing(orderValue, price);

        // Then
        assertEquals(expectedValid, isValid);
    }

    @Test
    void testValidatePricing_WithNullValues_ShouldReturnFalse() {
        // Test null orderValue
        assertFalse(VideoPricingUtil.validatePricing(null, new BigDecimal("100")));
        
        // Test null price
        assertFalse(VideoPricingUtil.validatePricing(new BigDecimal("100"), null));
        
        // Test both null
        assertFalse(VideoPricingUtil.validatePricing(null, null));
    }
}
