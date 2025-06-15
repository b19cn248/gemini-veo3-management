package com.ptit.google.veo3.service;

import com.ptit.google.veo3.service.interfaces.IVideoPricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VideoPricingService
 * Tests business rules and edge cases for the refactored pricing service
 */
class VideoPricingServiceTest {

    private IVideoPricingService videoPricingService;

    @BeforeEach
    void setUp() {
        videoPricingService = new VideoPricingService();
    }

    @ParameterizedTest
    @CsvSource({
            "8, 20000.00",
            "16, 45000.00",
            "24, 65000.00",
            "32, 90000.00",
            "40, 110000.00"
    })
    void testCalculateOrderValue_WithValidDurations_ShouldReturnCorrectValue(Integer duration, String expectedOrderValueStr) {
        // Given
        BigDecimal expectedOrderValue = new BigDecimal(expectedOrderValueStr);

        // When
        BigDecimal actualOrderValue = videoPricingService.calculateOrderValue(duration);

        // Then
        assertNotNull(actualOrderValue);
        assertEquals(0, expectedOrderValue.compareTo(actualOrderValue),
                String.format("Expected %s but got %s for duration %d", expectedOrderValue, actualOrderValue, duration));
    }

    @Test
    void testCalculateOrderValue_WithNullDuration_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            videoPricingService.calculateOrderValue(null);
        });

        assertEquals("Video duration cannot be null", exception.getMessage());
    }

    @Test
    void testCalculateOrderValue_WithUnsupportedDuration_ShouldThrowException() {
        // Given
        Integer unsupportedDuration = 30; // Not in supported list

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            videoPricingService.calculateOrderValue(unsupportedDuration);
        });

        assertTrue(exception.getMessage().contains("Unsupported video duration"));
    }

    @ParameterizedTest
    @CsvSource({
            "20000.00, 30000.00",
            "45000.00, 170000.00",
            "65000.00, 230000.00",
            "90000.00, 290000.00",
            "110000.00, 350000.00"
    })
    void testCalculatePrice_WithValidOrderValues_ShouldReturnCorrectPrice(String orderValueStr, String expectedPriceStr) {
        // Given
        BigDecimal orderValue = new BigDecimal(orderValueStr);
        BigDecimal expectedPrice = new BigDecimal(expectedPriceStr);

        // When
        BigDecimal actualPrice = videoPricingService.calculatePrice(orderValue);

        // Then
        assertNotNull(actualPrice);
        assertEquals(0, expectedPrice.compareTo(actualPrice),
                String.format("Expected %s but got %s for order value %s", expectedPrice, actualPrice, orderValue));
    }

    @Test
    void testCalculatePrice_WithNullOrderValue_ShouldReturnNull() {
        // When
        BigDecimal result = videoPricingService.calculatePrice(null);

        // Then
        assertNull(result);
    }

    @Test
    void testCalculatePrice_WithUnsupportedOrderValue_ShouldReturnNull() {
        // Given
        BigDecimal unsupportedOrderValue = new BigDecimal("75000.00");

        // When
        BigDecimal result = videoPricingService.calculatePrice(unsupportedOrderValue);

        // Then
        assertNull(result);
    }

    @ParameterizedTest
    @CsvSource({
            "8, 20000.00, 30000.00",
            "16, 45000.00, 170000.00",
            "24, 65000.00, 230000.00",
            "32, 90000.00, 290000.00",
            "40, 110000.00, 350000.00"
    })
    void testCalculateOrderValueAndPrice_WithValidDurations_ShouldReturnBothValues(
            Integer duration, String expectedOrderValueStr, String expectedPriceStr) {
        // Given
        BigDecimal expectedOrderValue = new BigDecimal(expectedOrderValueStr);
        BigDecimal expectedPrice = new BigDecimal(expectedPriceStr);

        // When
        BigDecimal[] result = videoPricingService.calculateOrderValueAndPrice(duration);

        // Then
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(0, expectedOrderValue.compareTo(result[0]));
        assertEquals(0, expectedPrice.compareTo(result[1]));
    }

    @ParameterizedTest
    @CsvSource({
            "20000.00, true",
            "45000.00, true",
            "65000.00, true",
            "90000.00, true",
            "110000.00, true",
            "75000.00, false",
            "0.00, false"
    })
    void testHasPricingRule(String orderValueStr, boolean expectedHasRule) {
        // Given
        BigDecimal orderValue = new BigDecimal(orderValueStr);

        // When
        boolean hasRule = videoPricingService.hasPricingRule(orderValue);

        // Then
        assertEquals(expectedHasRule, hasRule);
    }

    @Test
    void testHasPricingRule_WithNull_ShouldReturnFalse() {
        // When
        boolean result = videoPricingService.hasPricingRule(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetSupportedDurations_ShouldReturnAllSupportedValues() {
        // When
        Integer[] supportedDurations = videoPricingService.getSupportedDurations();

        // Then
        assertNotNull(supportedDurations);
        assertEquals(5, supportedDurations.length);
        
        // Check if contains expected durations (order doesn't matter)
        assertTrue(java.util.Arrays.asList(supportedDurations).contains(8));
        assertTrue(java.util.Arrays.asList(supportedDurations).contains(16));
        assertTrue(java.util.Arrays.asList(supportedDurations).contains(24));
        assertTrue(java.util.Arrays.asList(supportedDurations).contains(32));
        assertTrue(java.util.Arrays.asList(supportedDurations).contains(40));
    }

    @Test
    void testCalculatePrice_WithDifferentScale_ShouldNormalizeAndMatch() {
        // Given - order value with different scales
        BigDecimal orderValue1 = new BigDecimal("20000"); // scale 0
        BigDecimal orderValue2 = new BigDecimal("20000.0"); // scale 1
        BigDecimal orderValue3 = new BigDecimal("20000.00"); // scale 2

        // When
        BigDecimal price1 = videoPricingService.calculatePrice(orderValue1);
        BigDecimal price2 = videoPricingService.calculatePrice(orderValue2);
        BigDecimal price3 = videoPricingService.calculatePrice(orderValue3);

        // Then - all should return same price
        assertNotNull(price1);
        assertNotNull(price2);
        assertNotNull(price3);
        assertEquals(0, price1.compareTo(price2));
        assertEquals(0, price2.compareTo(price3));
        assertEquals(0, new BigDecimal("30000.00").compareTo(price1));
    }
}