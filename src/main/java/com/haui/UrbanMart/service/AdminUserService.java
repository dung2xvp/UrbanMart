package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.AdminUserCreateRequest;
import com.haui.UrbanMart.dto.request.AdminUserUpdateRequest;
import com.haui.UrbanMart.dto.response.AdminUserResponse;
import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.User;
import com.haui.UrbanMart.entity.UserRole;
import com.haui.UrbanMart.entity.UserStatus;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(
            String keyword,
            UserRole role,
            UserStatus status,
            Pageable pageable
    ) {
        Page<User> users = userRepository.searchUsers(
                normalizeKeyword(keyword),
                role,
                status,
                pageable
        );
        Map<UUID, Branch> branchesByAccountId = getBranchesByAccountId(users.getContent());
        return users.map(user -> toResponse(user, branchesByAccountId.get(user.getId())));
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID userId) {
        User user = findUser(userId);
        Branch branch = branchRepository.findByAccount_Id(userId).orElse(null);
        return toResponse(user, branch);
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        validateBranchAssignment(request.role(), request.branchId());
        ensureUniqueCredentials(request.phone(), request.email(), null);

        User user = new User();
        applyUserDetails(
                user,
                request.fullName(),
                request.phone(),
                request.email(),
                request.birthDate(),
                request.gender()
        );
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.saveAndFlush(user);

        Branch branch = assignBranch(
                user, request.role(), user.getStatus(), request.branchId()
        );
        return toResponse(user, branch);
    }

    @Transactional
    public AdminUserResponse updateUser(
            UUID userId,
            UUID actingAdminId,
            AdminUserUpdateRequest request
    ) {
        User user = findUser(userId);
        if (userId.equals(actingAdminId)
                && (request.role() != UserRole.ADMIN
                || request.status() != UserStatus.ACTIVE)) {
            throw new BadRequestException(
                    "Không thể tự khóa tài khoản hoặc thay đổi vai trò của chính mình"
            );
        }

        validateBranchAssignment(request.role(), request.branchId());
        ensureUniqueCredentials(request.phone(), request.email(), userId);
        preventRemovingLastActiveAdmin(user, request.role(), request.status());

        applyUserDetails(
                user,
                request.fullName(),
                request.phone(),
                request.email(),
                request.birthDate(),
                request.gender()
        );
        user.setRole(request.role());
        user.setStatus(request.status());
        if (request.status() == UserStatus.ACTIVE) {
            user.setFailedLoginAttempts((short) 0);
            user.setLockedUntil(null);
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setFailedLoginAttempts((short) 0);
            user.setLockedUntil(null);
        }

        Branch branch = assignBranch(
                user, request.role(), request.status(), request.branchId()
        );
        user = userRepository.saveAndFlush(user);
        return toResponse(user, branch);
    }

    @Transactional
    public AdminUserResponse deactivateUser(UUID userId, UUID actingAdminId) {
        if (userId.equals(actingAdminId)) {
            throw new BadRequestException("Không thể tự khóa tài khoản của chính mình");
        }

        User user = findUser(userId);
        preventRemovingLastActiveAdmin(user, user.getRole(), UserStatus.LOCKED);
        user.setStatus(UserStatus.LOCKED);
        Branch branch = branchRepository.findByAccount_Id(userId).orElse(null);
        if (branch != null) {
            branch.setAccount(null);
            branchRepository.save(branch);
        }
        userRepository.save(user);
        return toResponse(user, null);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    private void ensureUniqueCredentials(String phone, String email, UUID excludedUserId) {
        boolean phoneTaken = excludedUserId == null
                ? userRepository.existsByPhone(phone)
                : userRepository.existsByPhoneAndIdNot(phone, excludedUserId);
        if (phoneTaken) {
            throw new ConflictException("Số điện thoại đã được sử dụng");
        }

        if (email != null && !email.isBlank()) {
            boolean emailTaken = excludedUserId == null
                    ? userRepository.existsByEmail(email)
                    : userRepository.existsByEmailAndIdNot(email, excludedUserId);
            if (emailTaken) {
                throw new ConflictException("Email đã được sử dụng");
            }
        }
    }

    private void validateBranchAssignment(UserRole role, UUID branchId) {
        if (role == UserRole.BRANCH && branchId == null) {
            throw new BadRequestException("Tài khoản chi nhánh phải được gán chi nhánh");
        }
        if (role != UserRole.BRANCH && branchId != null) {
            throw new BadRequestException(
                    "Chỉ tài khoản chi nhánh mới được gán với chi nhánh"
            );
        }
    }

    private Branch assignBranch(
            User user,
            UserRole role,
            UserStatus status,
            UUID branchId
    ) {
        Branch currentBranch = branchRepository.findByAccount_Id(user.getId()).orElse(null);
        if (role != UserRole.BRANCH || status != UserStatus.ACTIVE || branchId == null) {
            if (currentBranch != null) {
                currentBranch.setAccount(null);
                branchRepository.save(currentBranch);
            }
            return null;
        }

        Branch targetBranch = branchRepository.findForUpdateById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
        Branch assignedBranch = branchRepository.findByAccount_Id(user.getId()).orElse(null);
        if (assignedBranch != null && !assignedBranch.getId().equals(branchId)) {
            assignedBranch.setAccount(null);
            branchRepository.save(assignedBranch);
        }

        if (targetBranch.getAccount() != null
                && !targetBranch.getAccount().getId().equals(user.getId())) {
            throw new ConflictException("Chi nhánh đã được gán cho tài khoản khác");
        }

        targetBranch.setAccount(user);
        return branchRepository.save(targetBranch);
    }

    private void preventRemovingLastActiveAdmin(
            User user,
            UserRole newRole,
            UserStatus newStatus
    ) {
        if (user.getRole() == UserRole.ADMIN
                && user.getStatus() == UserStatus.ACTIVE
                && (newRole != UserRole.ADMIN || newStatus != UserStatus.ACTIVE)
                && userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new ConflictException("Không thể khóa hoặc hạ quyền tài khoản admin cuối cùng");
        }
    }

    private void applyUserDetails(
            User user,
            String fullName,
            String phone,
            String email,
            java.time.LocalDate birthDate,
            String gender
    ) {
        user.setFullName(fullName.trim());
        user.setPhone(phone.trim());
        user.setEmail(email == null || email.isBlank() ? null : email.trim());
        user.setBirthDate(birthDate);
        user.setGender(gender == null || gender.isBlank() ? null : gender.trim());
    }

    private Map<UUID, Branch> getBranchesByAccountId(List<User> users) {
        List<UUID> accountIds = users.stream().map(User::getId).toList();
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Branch> result = new HashMap<>();
        branchRepository.findAllByAccount_IdIn(accountIds).forEach(branch ->
                result.put(branch.getAccount().getId(), branch)
        );
        return result;
    }

    private AdminUserResponse toResponse(User user, Branch branch) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGender(),
                user.getRole(),
                user.getStatus(),
                branch == null ? null : branch.getId(),
                branch == null ? null : branch.getName(),
                user.getCreatedAt()
        );
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
