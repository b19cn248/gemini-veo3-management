        video.autoReset(); // assignedStaff=null, status=CHUA_AI_NHAN
        videoRepository.save(video);
    }
}
```

### 3. Query tối ưu:
```sql
SELECT v FROM Video v 
WHERE v.isDeleted = false 
AND v.assignedAt IS NOT NULL 
AND v.assignedStaff IS NOT NULL 
AND v.status IN ('DANG_LAM', 'DANG_SUA') 
AND v.assignedAt < :expiredTime
```

## API Endpoints mới

### 1. Monitor Auto-Reset Status
```http
GET /api/v1/videos/auto-reset/status
```
**Response:**
```json
{
  "success": true,
  "message": "Lấy thông tin trạng thái auto-reset thành công",
  "data": {
    "expiredVideoCount": 2,
    "expiredVideos": [
      {
        "id": 123,
        "assignedStaff": "John Doe",
        "assignedAt": "2025-06-13T10:00:00",
        "status": "DANG_LAM",
        "customerName": "Customer ABC"
      }
    ],
    "systemStatus": "ACTIVE"
  }
}
```

### 2. Manual Reset Video
```http
POST /api/v1/videos/{id}/manual-reset
```
**Response:**
```json
{
  "success": true,
  "message": "Video ID 123 đã được reset thành công",
  "data": {
    "videoId": 123,
    "resetStatus": "SUCCESS"
  }
}
```

## Configuration

### Timeout Setting
```yaml
app:
  video:
    assignment-timeout-minutes: 15  # Có thể thay đổi theo nhu cầu
```

### Scheduling Configuration
- Scheduled job chạy mỗi phút: `@Scheduled(cron = "0 * * * * *")`
- Thread pool: 5 threads cho scheduled tasks
- Graceful shutdown: 60 seconds timeout

## Performance Considerations

### 1. Database Indexes
```sql
-- Index cho field assigned_at
CREATE INDEX idx_videos_assigned_at ON videos(assigned_at);

-- Composite index cho auto-reset query
CREATE INDEX idx_videos_auto_reset ON videos(assigned_at, status, is_deleted);
```

### 2. Query Optimization
- Sử dụng composite index để tối ưu query
- Batch processing với @Transactional
- Timeout 30 giây cho scheduled job

### 3. Monitoring
- Log chi tiết cho mỗi lần reset
- Count expired videos trước khi xử lý
- Error handling riêng cho từng video

## Deployment Instructions

### 1. Database Migration
```bash
# Liquibase sẽ tự động chạy khi start application
# File migration: 03-add-assigned-at-field.xml
```

### 2. Application Restart
```bash
# Stop application
# Deploy new code
# Start application
# Scheduled job sẽ tự động chạy sau 1 phút
```

### 3. Verification
```bash
# Check logs
grep "Auto-reset job" application.log

# Test API
curl -X GET "http://localhost:8080/api/v1/videos/auto-reset/status"

# Manual test
curl -X POST "http://localhost:8080/api/v1/videos/123/manual-reset"
```

## Testing Scenarios

### 1. Functional Testing
- Assign video cho nhân viên
- Đợi 15 phút (hoặc change config để test nhanh)
- Verify video được reset về CHUA_AI_NHAN

### 2. Unit Testing
```bash
mvn test -Dtest=VideoAutoResetServiceTest
```

### 3. Integration Testing
- Test với database thật
- Test scheduled job
- Test API endpoints

## Troubleshooting

### 1. Scheduled Job không chạy
```java
// Check @EnableScheduling annotation trong SchedulingConfig
// Check application logs for scheduler errors
```

### 2. Video không được reset
```sql
-- Check data trong database
SELECT id, assigned_staff, assigned_at, status 
FROM videos 
WHERE assigned_at < NOW() - INTERVAL 15 MINUTE
AND status IN ('DANG_LAM', 'DANG_SUA');
```

### 3. Performance Issues
```sql
-- Check index usage
EXPLAIN SELECT * FROM videos 
WHERE assigned_at < '2025-06-13 10:00:00' 
AND status IN ('DANG_LAM', 'DANG_SUA');
```

## Monitoring Queries

### 1. Expired Videos Count
```sql
SELECT COUNT(*) as expired_count
FROM videos 
WHERE is_deleted = false 
AND assigned_at IS NOT NULL 
AND assigned_staff IS NOT NULL 
AND status IN ('DANG_LAM', 'DANG_SUA') 
AND assigned_at < NOW() - INTERVAL 15 MINUTE;
```

### 2. Reset History (từ logs)
```bash
grep "Successfully auto-reset video" application.log | tail -10
```

### 3. System Health Check
```http
GET /api/v1/videos/auto-reset/status
```

## Business Impact

### 1. Benefits
- Tránh tình trạng video bị "lock" bởi nhân viên không active
- Tự động giải phóng workload để nhân viên khác có thể nhận
- Giảm manual intervention từ admin

### 2. Considerations  
- Video sẽ mất tiến độ đã làm được (nếu có)
- Cần thông báo cho nhân viên về timeout policy
- Monitor để đảm bảo timeout setting phù hợp

## Future Enhancements

### 1. Notification System
- Thông báo cho nhân viên trước khi reset (5 phút warning)
- Email/SMS alert khi video bị auto-reset

### 2. Flexible Timeout
- Timeout khác nhau cho từng loại video
- Timeout dựa trên workload hiện tại của nhân viên

### 3. Advanced Analytics
- Tracking tỷ lệ video bị auto-reset theo nhân viên
- Performance metrics cho scheduled job
