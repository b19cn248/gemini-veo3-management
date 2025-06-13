    (v.order_value = 45000 AND v.price != 150000) OR
    (v.order_value = 65000 AND v.price != 200000) OR
    (v.order_value = 100000 AND v.price != 250000)
);

-- Revenue analysis
SELECT 
    assigned_staff,
    COUNT(*) as total_videos,
    SUM(order_value) as total_cost,
    SUM(price) as total_revenue,
    SUM(price - order_value) as total_profit,
    AVG(((price - order_value) / order_value) * 100) as avg_profit_margin_percent
FROM videos 
WHERE price IS NOT NULL 
AND order_value IS NOT NULL
AND payment_status = 'DA_THANH_TOAN'
GROUP BY assigned_staff
ORDER BY total_profit DESC;
```

## Testing

### 1. Unit Tests
```bash
# Test pricing utility
mvn test -Dtest=VideoPricingUtilTest

# Expected results:
# ✓ calculatePrice với order_value = 15000 → price = 30000
# ✓ calculatePrice với order_value = 45000 → price = 150000  
# ✓ calculatePrice với order_value = 65000 → price = 200000
# ✓ calculatePrice với order_value = 100000 → price = 250000
# ✓ calculatePrice với unsupported order_value → null
# ✓ calculateProfitMargin calculations
# ✓ validatePricing consistency checks
```

### 2. Integration Tests
```bash
# Test API endpoints
curl -X POST "http://localhost:8080/api/v1/videos" \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Test Customer",
    "videoDuration": 8,
    "videoContent": "Test content"
  }'

# Verify pricing in response
curl -X GET "http://localhost:8080/api/v1/videos/{id}"

# Test pricing rules API
curl -X GET "http://localhost:8080/api/v1/videos/pricing-rules"
```

### 3. Manual Testing Scenarios
```
Scenario 1: Tạo video 8 giây
- Input: videoDuration = 8
- Expected: orderValue = 15000, price = 30000

Scenario 2: Tạo video 16 giây  
- Input: videoDuration = 16
- Expected: orderValue = 45000, price = 150000

Scenario 3: Tạo video 24 giây
- Input: videoDuration = 24
- Expected: orderValue = 65000, price = 200000

Scenario 4: Tạo video 32 giây
- Input: videoDuration = 32
- Expected: orderValue = 100000, price = 250000

Scenario 5: Update video duration
- Input: Change videoDuration từ 8 → 16
- Expected: orderValue từ 15000 → 45000, price từ 30000 → 150000
```

## Performance Considerations

### 1. Database Optimization
- **Index `idx_videos_price`**: Fast queries theo price
- **Composite Index `idx_videos_order_value_price`**: Fast queries kết hợp cost-revenue
- **DECIMAL(15,2)**: Precision phù hợp cho financial data

### 2. Memory Usage
- **Static Map**: PRICING_MAP được load 1 lần vào memory
- **Immutable**: Map.copyOf() để prevent modification
- **BigDecimal**: Thread-safe và accurate cho financial calculations

### 3. Query Performance
```sql
-- Optimal query với index
EXPLAIN SELECT * FROM videos WHERE price BETWEEN 50000 AND 200000;
-- Sử dụng idx_videos_price

-- Optimal query với composite index  
EXPLAIN SELECT * FROM videos WHERE order_value = 45000 AND price = 150000;
-- Sử dụng idx_videos_order_value_price
```

## Error Handling

### 1. Unsupported Order Values
```java
// VideoPricingUtil sẽ return null
BigDecimal price = VideoPricingUtil.calculatePrice(new BigDecimal("75000"));
// price = null

// VideoService sẽ log warning và preserve existing price hoặc accept manual price
if (calculatedPrice != null) {
    video.setPrice(calculatedPrice);
} else {
    log.warn("No pricing rule found for order value {}", orderValue);
    // Keep existing price hoặc use manual price từ request
}
```

### 2. Manual Price Override
```java
// Client có thể gửi manual price trong request
{
  "orderValue": 75000,  // Unsupported value
  "price": 180000       // Manual override
}

// Service sẽ accept manual price khi không có auto-calculation
```

### 3. Validation
```java
// Validate pricing consistency
boolean isValid = VideoPricingUtil.validatePricing(orderValue, price);
if (!isValid) {
    log.warn("Pricing validation failed: expected {} for order value {}, but got {}", 
            expectedPrice, orderValue, price);
}
```

## Monitoring & Analytics

### 1. Pricing Consistency Check
```sql
-- Daily check cho pricing inconsistencies
SELECT COUNT(*) as inconsistent_videos
FROM videos v
WHERE v.order_value IS NOT NULL 
AND v.price IS NOT NULL
AND NOT (
    (v.order_value = 15000 AND v.price = 30000) OR
    (v.order_value = 45000 AND v.price = 150000) OR
    (v.order_value = 65000 AND v.price = 200000) OR
    (v.order_value = 100000 AND v.price = 250000)
);
```

### 2. Revenue Analytics
```sql
-- Monthly revenue report
SELECT 
    YEAR(payment_date) as year,
    MONTH(payment_date) as month,
    COUNT(*) as paid_videos,
    SUM(order_value) as total_costs,
    SUM(price) as total_revenue,
    SUM(price - order_value) as total_profit,
    ROUND(AVG(((price - order_value) / order_value) * 100), 2) as avg_margin_percent
FROM videos 
WHERE payment_status = 'DA_THANH_TOAN'
AND price IS NOT NULL
AND order_value IS NOT NULL
GROUP BY YEAR(payment_date), MONTH(payment_date)
ORDER BY year DESC, month DESC;
```

### 3. Profit Margin Analysis
```sql
-- Profit margin by video duration/type
SELECT 
    order_value,
    price,
    COUNT(*) as video_count,
    ROUND(((price - order_value) / order_value) * 100, 2) as profit_margin_percent
FROM videos 
WHERE price IS NOT NULL AND order_value IS NOT NULL
GROUP BY order_value, price
ORDER BY order_value;
```

## Future Enhancements

### 1. Dynamic Pricing
- Admin dashboard để update pricing rules
- Database-driven pricing thay vì static Map
- Price history tracking

### 2. Advanced Pricing Models
- Tiered pricing dựa trên customer segment
- Volume discounts cho bulk orders
- Time-based pricing (rush orders)

### 3. Integration Features
- Export pricing reports
- Integration với accounting systems
- Automated invoice generation với price breakdown

## Deployment Checklist

### 1. Pre-deployment
- [ ] Run unit tests: `mvn test -Dtest=VideoPricingUtilTest`
- [ ] Verify database migration: `04-add-price-field.xml`
- [ ] Test API endpoints locally
- [ ] Review pricing rules business logic

### 2. Deployment
- [ ] Deploy database migration
- [ ] Deploy application code
- [ ] Verify pricing rules API: `GET /api/v1/videos/pricing-rules`
- [ ] Test create/update video với pricing

### 3. Post-deployment
- [ ] Monitor application logs cho pricing operations
- [ ] Run pricing consistency check query
- [ ] Verify existing videos có price values
- [ ] Test end-to-end workflow

### 4. Rollback Plan
```sql
-- Emergency rollback: Remove price column
ALTER TABLE videos DROP COLUMN price;

-- Remove indexes
DROP INDEX idx_videos_price;
DROP INDEX idx_videos_order_value_price;
```

## API Documentation Updates

### New Endpoints:
- `GET /api/v1/videos/pricing-rules` - Get all pricing rules

### Modified Responses:
- All video responses now include `price` field
- Create/Update operations auto-calculate price

### Request Examples:
See API Examples section above for detailed request/response formats.

## Conclusion

Price feature implementation provides:
- ✅ **Automated Pricing**: Tự động tính price từ order_value
- ✅ **Business Rules**: Centralized pricing logic trong VideoPricingUtil
- ✅ **Data Integrity**: Database constraints và validation
- ✅ **Performance**: Optimized indexes và efficient queries
- ✅ **Maintainability**: Clean separation of concerns và comprehensive testing
- ✅ **Monitoring**: APIs và queries để track pricing consistency
- ✅ **Documentation**: Comprehensive docs và examples

Feature này hoàn toàn tương thích với existing code và không phá vỡ backward compatibility.
