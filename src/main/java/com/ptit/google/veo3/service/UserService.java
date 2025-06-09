package com.ptit.google.veo3.service;

import com.ptit.google.veo3.entity.User;
import com.ptit.google.veo3.entity.UserStatus;
import com.ptit.google.veo3.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * Tạo mới user
     */
    public User createUser(User user) {
        log.info("Creating new user: {}", user.getFullName());

        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng: " + user.getEmail());
        }

        User savedUser = userRepository.save(user);
        log.info("Created user with ID: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Cập nhật user
     */
    public Optional<User> updateUser(Long id, User userUpdate) {
        log.info("Updating user with ID: {}", id);

        return userRepository.findById(id)
                .map(existingUser -> {
                    // Kiểm tra email conflict (trừ chính user đang update)
                    if (!existingUser.getEmail().equals(userUpdate.getEmail())
                            && userRepository.existsByEmail(userUpdate.getEmail())) {
                        throw new RuntimeException("Email đã được sử dụng: " + userUpdate.getEmail());
                    }

                    // Update fields
                    existingUser.setFullName(userUpdate.getFullName());
                    existingUser.setEmail(userUpdate.getEmail());
                    existingUser.setPhone(userUpdate.getPhone());
                    existingUser.setDepartment(userUpdate.getDepartment());
                    existingUser.setPosition(userUpdate.getPosition());
                    existingUser.setStatus(userUpdate.getStatus());
                    existingUser.setNotes(userUpdate.getNotes());

                    User updatedUser = userRepository.save(existingUser);
                    log.info("Updated user with ID: {}", updatedUser.getId());
                    return updatedUser;
                });
    }

    /**
     * Xóa user
     */
    public boolean deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Kiểm tra xem user có video đang được giao không
            if (user.getAssignedVideoCount() > 0) {
                throw new RuntimeException("Không thể xóa nhân viên đang có video được giao");
            }

            userRepository.deleteById(id);
            log.info("Deleted user with ID: {}", id);
            return true;
        }

        log.warn("User with ID: {} not found for deletion", id);
        return false;
    }

    /**
     * Lấy user theo ID
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        log.info("Fetching user with ID: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Lấy tất cả users với phân trang
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        log.info("Fetching all users with pagination");
        return userRepository.findAll(pageable);
    }

    /**
     * Lấy danh sách users đang hoạt động (để gán video)
     */
    @Transactional(readOnly = true)
    public List<User> getActiveUsers() {
        log.info("Fetching active users");
        return userRepository.findByStatusOrderByFullNameAsc(UserStatus.ACTIVE);
    }

    /**
     * Tìm user theo tên
     */
    @Transactional(readOnly = true)
    public Page<User> findByFullName(String fullName, Pageable pageable) {
        log.info("Searching users by name: {}", fullName);
        return userRepository.findByFullNameContainingIgnoreCase(fullName, pageable);
    }

    /**
     * Lấy thống kê users theo phòng ban
     */
    @Transactional(readOnly = true)
    public List<Object[]> getUserStatsByDepartment() {
        log.info("Fetching user statistics by department");
        return userRepository.countActiveUsersByDepartment();
    }
}