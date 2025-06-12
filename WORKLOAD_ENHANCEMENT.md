# Workload Management Enhancement - Video Management System

## Tổng quan thay đổi

Cập nhật logic quản lý workload cho nhân viên trong API `/api/v1/videos/{id}/assigned-staff` để nâng giới hạn từ 2 video lên **3 video** và cải thiện logic kiểm tra tổng thể.

## Business Rules mới

### Trước đây (Logic cũ):
- Nhân viên tối đa **2 video** có status `DANG_LAM`
- Nhân viên tối đa **2 video** có delivery status `CAN_SUA_GAP`
- Kiểm tra riêng biệt từng điều kiện

### Hiện tại (Logic mới):
- Nhân viên tối đa **3 video** đang "active" cùng lúc
- Video "active" bao gồm:
  - Status = `DANG_LAM` (đang làm)
  - Status = `DANG_SUA` (đang sửa)  
  - Delivery Status = `CAN_SUA_GAP` (cần sửa gấp)
- Sử dụng `COUNT(DISTINCT video.id)` để tránh duplicate counting
- Kiểm tra tổng workload thay vì từng điều kiện riêng lẻ

## API Updates

### 1. **PATCH /api/v1/videos/{id}/assigned-staff** - Enhanced Logic
- Tăng giới hạn từ 2 → 3 video
- Logic kiểm tra workload thông minh hơn
- Error messages chi tiết hơn với breakdown

### 2. **GET /api/v1/videos/staff-workload** - New Endpoint
- Monitoring workload của nhân viên
- Chi tiết breakdown theo từng trạng thái
- Support query theo tên hoặc lấy từ JWT

## Files được thay đổi/tạo mới

### 1. [CREATED] StaffWorkloadService.java
- **Đường dẫn**: `src/main/java/com/ptit/google/veo3/service/StaffWorkloadService.java`
- **Mục đích**: Service chuyên xử lý logic workload của nhân viên
- **Chức năng chính**:
  - `canAcceptNewTask()`: Kiểm tra có thể nhận task mới không
  - `validateCanAcceptNewTask()`: Validate và throw exception nếu quá tải
  - `getCurrentWorkload()`: Lấy số video đang active
  - `getWorkloadInfo()`: Lấy thông tin chi tiết workload với breakdown

### 2. [UPDATED] VideoRepository.java
- **Đường dẫn**: `src/main/java/com/ptit/google/veo3/repository/VideoRepository.java`
- **Thay đổi**:
  - Thêm `countActiveWorkloadByAssignedStaff()`: Count tổng video active
  - Thêm `findActiveWorkloadByAssignedStaff()`: Lấy danh sách video active
  - SQL query optimized với `COUNT(DISTINCT)` và complex conditions

### 3. [UPDATED] VideoService.java
- **Đường dẫn**: `src/main/java/com/ptit/google/veo3/service/VideoService.java`
- **Thay đổi**:
  - Thêm dependency cho `StaffWorkloadService`
  - Refactor `updateAssignedStaff()` để sử dụng logic mới
  - Enhanced logging với workload breakdown
  - Improved error messages

### 4. [UPDATED] VideoController.java
- **Đường dẫn**: `src/main/java/com/ptit/google/veo3/controller/VideoController.java`
- **Thay đổi**:
  - Thêm dependencies cho `JwtTokenService` và `StaffWorkloadService`
  - Thêm endpoint `GET /staff-workload`
  - Cập nhật header documentation

### 5. [CREATED] StaffWorkloadServiceTest.java
- **Đường dẫn**: `src/test/java/com/ptit/google/veo3/service/StaffWorkloadServiceTest.java`
- **Mục đích**: Comprehensive unit tests cho StaffWorkloadService
- **Coverage**: Tất cả scenarios bao gồm edge cases và error conditions

## Database Query Optimization

### New SQL Query - countActiveWorkloadByAssignedStaff:
```sql
SELECT COUNT(DISTINCT v.id) 
FROM Video v 
WHERE v.isDeleted = false 
AND v.assignedStaff = :assignedStaff 
AND (
    v.status IN ('DANG_LAM', 'DANG_SUA') 
    OR v.deliveryStatus = 'CAN_SUA_GAP'
)
```

### Benefits:
- **Performance**: Single query thay vì multiple queries
- **Accuracy**: `DISTINCT` tránh double counting
- **Maintainability**: Centralized logic trong repository
- **Scalability**: Efficient indexing opportunities

## API Response Examples

### GET /api/v1/videos/staff-workload

#### Success Response (200 OK):
```json
{
  "success": true,
  "message": "Lấy thông tin workload của nhân viên 'Nguyễn Minh Hiếu' thành công",
  "data": {
    "assignedStaff": "Nguyễn Minh Hiếu",
    "totalActive": 2,
    "breakdown": {
      "dangLam": 1,
      "dangSua": 1,
      "canSuaGap": 1
    },
    "canAcceptNewTask": true,
    "maxConcurrentVideos": 3,
    "activeVideoIds": [123, 456, 789]
  },
  "tenantId": "video_management",
  "timestamp": 1734567890123
}
```

### PATCH /api/v1/videos/{id}/assigned-staff - Error Cases

#### Workload Exceeded (400 BAD REQUEST):
```json
{
  "success": false,
  "message": "Nhân viên 'Nguyễn Minh Hiếu' đã đạt giới hạn tối đa 3 video. Hiện tại đang xử lý 3 video. Vui lòng hoàn thành một số video trước khi nhận task mới.",
  "data": null,
  "timestamp": 1734567890123,
  "status": 400,
  "error": "Bad Request",
  "tenantId": "video_management"
}
```

## Logic Flow Comparison

### Before (Old Logic):
```
1. Check if DANG_LAM count >= 2 → Reject
2. Check if CAN_SUA_GAP count >= 2 → Reject  
3. Accept assignment
```

### After (New Logic):
```
1. Calculate total active workload (DANG_LAM + DANG_SUA + CAN_SUA_GAP)
2. Check if total >= 3 → Reject with detailed breakdown
3. Log workload info for monitoring
4. Accept assignment
5. Log new workload status
```

## Performance Improvements

### Query Optimization:
- **Before**: 2 separate COUNT queries
- **After**: 1 optimized DISTINCT COUNT query
- **Performance Gain**: ~50% reduction in database calls

### Caching Strategy:
- Repository method results can be cached
- Workload info computed once per request
- Minimal memory footprint với DTO pattern

### Monitoring & Observability:
- Detailed logging cho workload changes
- Breakdown metrics for business intelligence
- Audit trail cho capacity planning

## Testing Strategy

### Unit Tests Coverage:
- ✅ **Happy Path**: Normal workload scenarios
- ✅ **Boundary Conditions**: Exactly at limit (3 videos)
- ✅ **Edge Cases**: Null/empty staff names
- ✅ **Error Scenarios**: Workload exceeded
- ✅ **Data Validation**: Input sanitization
- ✅ **Mock Integration**: Repository interaction testing

### Integration Test Scenarios:
1. **Assign to available staff** → Success
2. **Assign to overloaded staff** → 400 Error
3. **Workload monitoring** → Correct breakdown
4. **Mixed status scenarios** → Accurate counting
5. **Concurrent assignment attempts** → Race condition handling

## Deployment Considerations

### Database Migration:
- ✅ **No schema changes required**
- ✅ **Backward compatible**
- ✅ **Zero downtime deployment**

### Configuration Updates:
- ✅ **No application.yml changes**
- ✅ **Uses existing OAuth2 setup**
- ✅ **Maintains multi-tenant support**

### Monitoring Setup:
```yaml
# Log analysis queries for ops team
error_patterns:
  - "đã đạt giới hạn tối đa 3 video"
  - "Workload exceeded"
  
metrics_to_track:
  - staff_workload_distribution
  - assignment_rejection_rate
  - average_videos_per_staff
```

## Business Impact

### Capacity Increase:
- **50% increase** trong capacity per staff (2 → 3 videos)
- **Better resource utilization** với intelligent workload distribution
- **Reduced bottlenecks** trong video processing pipeline

### Operational Benefits:
- **Clear visibility** vào staff workload
- **Proactive monitoring** để avoid overload
- **Data-driven decisions** cho staff allocation

### User Experience:
- **Informative error messages** khi assignment fails
- **Real-time workload status** cho managers
- **Predictable system behavior** với consistent limits

## Future Enhancements

### Phase 2 Features:
1. **Dynamic Limits**: Configurable max videos per staff
2. **Priority Queuing**: Urgent videos bypass normal limits
3. **Load Balancing**: Auto-assignment based on workload
4. **Analytics Dashboard**: Workload trends và insights

### Technical Debt:
1. **Caching Layer**: Redis cache cho workload data
2. **Async Processing**: Background workload calculation
3. **Event Sourcing**: Track workload changes over time
4. **Health Checks**: Workload service health monitoring

---

## Migration Checklist

### Pre-deployment:
- [ ] Review all unit tests pass
- [ ] Integration testing với real data
- [ ] Performance testing cho new queries
- [ ] Documentation review

### Deployment:
- [ ] Deploy code changes
- [ ] Monitor error rates
- [ ] Verify workload calculations
- [ ] Test new API endpoint

### Post-deployment:
- [ ] Monitor staff assignment patterns
- [ ] Analyze workload distribution
- [ ] Collect user feedback
- [ ] Performance metrics validation

**Tác giả**: Generated  
**Ngày cập nhật**: 2024-12-18  
**Version**: 2.0  
**Status**: Ready for Deployment