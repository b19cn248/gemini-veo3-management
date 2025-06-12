# Authorization Enhancement - Video Management System

## Tổng quan thay đổi

Thực hiện thêm logic phân quyền cho 2 API endpoints để đảm bảo chỉ có nhân viên được giao video mới có quyền cập nhật status và video URL.

## API được cập nhật

1. **PATCH /api/v1/videos/{id}/status** - Cập nhật trạng thái video
2. **PATCH /api/v1/videos/{id}/video-url** - Cập nhật link video

## Logic phân quyền

### Cơ chế hoạt động:
1. Extract trường `name` từ JWT token hiện tại
2. So sánh với trường `assignedStaff` của video
3. Chỉ cho phép cập nhật khi tên khớp nhau (case-insensitive)
4. Trả về lỗi 403 FORBIDDEN nếu không có quyền

### JWT Token Structure:
```json
{
  "name": "Nguyễn Minh Hiếu",
  "email": "hieu1234.ptit@gmail.com",
  "preferred_username": "admin",
  // ... other claims
}
```

## Files được thay đổi

### 1. [CREATED] JwtTokenService.java
- **Đường dẫn**: `src/main/java/com/ptit/google/veo3/service/JwtTokenService.java`
- **Mục đích**: Service để xử lý JWT token và kiểm tra quyền truy cập
- **Chức năng chính**:
  - `getCurrentUserNameFromJwt()`: Lấy tên người dùng từ JWT
  - `hasPermissionToUpdateVideo()`: Kiểm tra quyền cập nhật video
  - `getCurrentUserInfoFromJwt()`: Lấy thông tin chi tiết người dùng

### 2. [UPDATED] VideoService.java
- **Đường dẫn**: `src/main/java/com/ptit/google/veo3/service/VideoService.java`
- **Thay đổi**:
  - Thêm dependency injection cho `JwtTokenService`
  - Cập nhật method `updateVideoStatus()` với logic phân quyền
  - Cập nhật method `updateVideoUrl()` với logic phân quyền
  - Thêm security checks và detailed logging

### 3. [UPDATED] VideoController.java  
- **Đường dẫn**: `src/main/java/com/ptit/google/veo3/controller/VideoController.java`
- **Thay đổi**:
  - Thêm exception handling cho `SecurityException`
  - Cập nhật JavaDoc documentation cho 2 endpoints
  - Thêm HTTP status 403 FORBIDDEN cho unauthorized access
  - Cập nhật header comments để reflect security changes

### 4. [CREATED] JwtTokenServiceTest.java
- **Đường dẫn**: `src/test/java/com/ptit/google/veo3/service/JwtTokenServiceTest.java`
- **Mục đích**: Unit tests cho JwtTokenService
- **Coverage**: Tất cả các scenarios bao gồm success cases và error cases

## Error Responses

### 403 FORBIDDEN - Unauthorized Access
```json
{
  "success": false,
  "message": "Không có quyền truy cập: Bạn không có quyền cập nhật trạng thái video này. Chỉ có nhân viên được giao video mới có thể cập nhật.",
  "data": null,
  "timestamp": 1734567890123,
  "status": 403,
  "error": "Forbidden",
  "tenantId": "video_management"
}
```

### 400 BAD REQUEST - Invalid Parameters  
```json
{
  "success": false,
  "message": "Tham số không hợp lệ: Trạng thái không được để trống",
  "data": null,
  "timestamp": 1734567890123,
  "status": 400,
  "error": "Bad Request",
  "tenantId": "video_management"
}
```

### 404 NOT FOUND - Video Not Found
```json
{
  "success": false,
  "message": "Không tìm thấy video với ID: 123",
  "data": null,
  "timestamp": 1734567890123,
  "status": 404,
  "error": "Not Found", 
  "tenantId": "video_management"
}
```

## Security Enhancements

### 1. **Authentication Required**
- Tất cả requests phải có valid JWT token
- JWT token được validate bởi Spring Security OAuth2

### 2. **Authorization Logic**
- Kiểm tra quyền truy cập based on video assignment
- Case-insensitive name comparison
- Graceful error handling when JWT parsing fails

### 3. **Audit Trail**
- Log chi tiết tất cả các attempts (successful và failed)
- Include user information và video ID trong logs
- Tenant-aware logging cho multi-tenant environment

## Testing

### Unit Tests
- Comprehensive test coverage cho JwtTokenService
- Mock SecurityContext và JWT token
- Test các edge cases và error scenarios

### Manual Testing Scenarios

#### Scenario 1: Successful Update (200 OK)
```bash
# User "Nguyễn Minh Hiếu" updates video assigned to "Nguyễn Minh Hiếu"
PATCH /api/v1/videos/1/status?status=DA_XONG
Authorization: Bearer <valid_jwt_token>
```

#### Scenario 2: Unauthorized Update (403 FORBIDDEN)  
```bash
# User "Nguyễn Minh Hiếu" tries to update video assigned to "Trần Văn Nam"
PATCH /api/v1/videos/2/status?status=DA_XONG
Authorization: Bearer <valid_jwt_token>
```

#### Scenario 3: Unassigned Video (403 FORBIDDEN)
```bash
# User tries to update video with assignedStaff = null
PATCH /api/v1/videos/3/video-url?videoUrl=https://example.com/video.mp4
Authorization: Bearer <valid_jwt_token>
```

## Performance Considerations

### 1. **JWT Token Parsing**
- JWT parsing được thực hiện một lần per request
- Cached trong SecurityContext để tránh repeated parsing
- Minimal overhead cho authorization checks

### 2. **Database Impact**
- Không thêm database queries cho authorization
- Sử dụng data đã load từ `findVideoByIdOrThrow()`
- Maintaining existing transaction boundaries

### 3. **Error Handling**
- Fail-fast approach cho unauthorized access
- Graceful degradation khi JWT parsing fails
- Detailed logging without performance impact

## Deployment Notes

### 1. **Backward Compatibility**
- Changes không breaking existing functionality
- API contract remains the same
- Chỉ thêm authorization layer

### 2. **Configuration**
- Không cần thay đổi configuration
- Sử dụng existing OAuth2 setup với Keycloak
- JWT validation đã được configure

### 3. **Monitoring**
- Monitor 403 errors để detect unauthorized access attempts
- Log analysis cho security incidents
- Performance metrics cho JWT processing

## Future Enhancements

### 1. **Role-Based Access Control**
- Extend để support admin role bypass
- Implement fine-grained permissions
- Add role hierarchy

### 2. **Audit Log**
- Dedicated audit table cho security events
- Track all video modifications với user context
- Compliance và security reporting

### 3. **Rate Limiting**
- Implement rate limiting cho video updates
- Prevent abuse của update endpoints
- Per-user rate limiting

---

**Tác giả**: Generated  
**Ngày tạo**: 2024-12-18  
**Version**: 1.0  
**Status**: Implemented và Tested