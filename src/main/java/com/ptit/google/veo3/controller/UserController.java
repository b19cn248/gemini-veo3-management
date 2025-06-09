package com.ptit.google.veo3.controller;

import com.ptit.google.veo3.entity.User;
import com.ptit.google.veo3.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    /**
     * Tạo mới user
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody User user) {
        log.info("Received request to create user: {}", user.getFullName());

        try {
            User createdUser = userService.createUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Nhân viên đã được tạo thành công");
            response.put("data", createdUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating user", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tạo nhân viên: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Cập nhật user
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User user) {

        log.info("Received request to update user with ID: {}", id);

        try {
            return userService.updateUser(id, user)
                    .map(updatedUser -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Nhân viên đã được cập nhật thành công");
                        response.put("data", updatedUser);

                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Không tìm thấy nhân viên với ID: " + id);

                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    });
        } catch (Exception e) {
            log.error("Error updating user", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi cập nhật nhân viên: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Xóa user
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        log.info("Received request to delete user with ID: {}", id);

        try {
            Map<String, Object> response = new HashMap<>();

            if (userService.deleteUser(id)) {
                response.put("success", true);
                response.put("message", "Nhân viên đã được xóa thành công");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy nhân viên với ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error deleting user", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi xóa nhân viên: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lấy chi tiết user theo ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        log.info("Received request to get user with ID: {}", id);

        return userService.getUserById(id)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", user);

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Không tìm thấy nhân viên với ID: " + id);

                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }

    /**
     * Lấy danh sách tất cả users với phân trang
     * GET /api/users?page=0&size=10&sort=fullName,asc
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Received request to get all users with pagination: page={}, size={}", page, size);

        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<User> userPage = userService.getAllUsers(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userPage.getContent());
            response.put("currentPage", userPage.getNumber());
            response.put("totalItems", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());
            response.put("pageSize", userPage.getSize());
            response.put("hasNext", userPage.hasNext());
            response.put("hasPrevious", userPage.hasPrevious());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching users", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tải danh sách nhân viên: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lấy danh sách users đang hoạt động (để gán video)
     * GET /api/users/active
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveUsers() {
        log.info("Received request to get active users");

        try {
            List<User> activeUsers = userService.getActiveUsers();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", activeUsers);
            response.put("totalItems", activeUsers.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching active users", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tải danh sách nhân viên: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Tìm user theo tên
     * GET /api/users/search?fullName=Nguyen
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsersByName(
            @RequestParam String fullName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Received request to search users by name: {}", fullName);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> userPage = userService.findByFullName(fullName, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userPage.getContent());
            response.put("currentPage", userPage.getNumber());
            response.put("totalItems", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching users by name", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tìm kiếm: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lấy thống kê users theo phòng ban
     * GET /api/users/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        log.info("Received request to get user statistics");

        try {
            List<Object[]> stats = userService.getUserStatsByDepartment();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching user statistics", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra khi tải thống kê: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
