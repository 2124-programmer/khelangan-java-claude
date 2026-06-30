package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.AdminSummaryDto;
import com.turfbook.backend.dto.AuthResponse;
import com.turfbook.backend.dto.ChangeRoleRequest;
import com.turfbook.backend.dto.CreateAdminRequest;
import com.turfbook.backend.dto.DeleteAccountRequest;
import com.turfbook.backend.dto.MessageResponse;
import com.turfbook.backend.dto.PromoteAdminRequest;
import com.turfbook.backend.dto.SetAdminRoleRequest;
import com.turfbook.backend.dto.UpdateProfileRequest;
import com.turfbook.backend.dto.UserDto;
import com.turfbook.backend.dto.UserPage;
import com.turfbook.backend.entity.AdminAuditEntity;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.SlotEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.mapper.UserMapper;
import com.turfbook.backend.repository.AdminAuditRepository;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.SlotRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.security.JwtTokenProvider;
import com.turfbook.backend.service.AdminPermissionService;
import com.turfbook.backend.service.NotificationService;
import com.turfbook.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final NotificationService notificationService;
    private final AdminPermissionService adminPermissionService;
    private final AdminAuditRepository auditRepository;

    /** Statuses considered "upcoming" when cancelling a closing player's bookings. */
    private static final List<BookingEntity.BookingStatus> UPCOMING_STATUSES =
            List.of(BookingEntity.BookingStatus.PENDING, BookingEntity.BookingStatus.CONFIRMED);

    @Override
    @Transactional(readOnly = true)
    public UserDto getMe(Long userId) {
        log.info("UserService.getMe() called - userId={}", userId);
        return userMapper.toDto(getEntityById(userId));
    }

    @Override
    @Transactional
    public UserDto updateMe(Long userId, UpdateProfileRequest request) {
        log.info("UserService.updateMe() called - userId={}", userId);
        UserEntity user = getEntityById(userId);

        if (StringUtils.hasText(request.getName())) {
            user.setName(request.getName());
        }
        // Phone is intentionally NOT updated here: changing it requires OTP verification +
        // active_phone uniqueness via the phone-change flow (see PhoneChangeService). A phone in
        // this payload is ignored so a profile edit can't silently bypass verification.
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPreferredSports() != null) {
            user.setPreferredSports(request.getPreferredSports());
        }

        UserDto result = userMapper.toDto(userRepository.save(user));
        log.info("UserService.updateMe() completed - userId={}", userId);
        return result;
    }

    @Override
    @Transactional
    public MessageResponse deleteMe(Long userId, DeleteAccountRequest request) {
        log.info("UserService.deleteMe() called - userId={}", userId);
        UserEntity user = getEntityById(userId);

        if (user.getStatus() == UserEntity.AccountStatus.DELETED) {
            throw new ConflictException("This account is already closed.");
        }
        if (request == null || !StringUtils.hasText(request.getPassword())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Incorrect password");
        }

        // Cancel the player's upcoming bookings: free the slot and notify the venue owner. (Owners
        // closing their own account is out of scope here — owner closure with its venue/subscription
        // cascade remains an admin action via AdminOwnerService.)
        int bookingsCancelled = 0;
        List<BookingEntity> upcoming = bookingRepository.findUpcomingForPlayer(
                user, UPCOMING_STATUSES, LocalDate.now());
        for (BookingEntity b : upcoming) {
            b.setStatus(BookingEntity.BookingStatus.CANCELLED);
            b.setCancellationReason(BookingEntity.CancellationReason.PLAYER);
            SlotEntity slot = b.getSlot();
            if (slot != null) {
                slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
                slotRepository.save(slot);
            }
            bookingRepository.save(b);
            if (b.getVenue() != null && b.getVenue().getOwner() != null) {
                notificationService.createNotification(
                        b.getVenue().getOwner(),
                        "Booking Cancelled",
                        String.format("A booking at %s on %s was cancelled because the player closed their account.",
                                b.getVenue().getName(), b.getDate()),
                        NotificationEntity.NotificationType.BOOKING,
                        String.valueOf(b.getId()), "BOOKING");
            }
            bookingsCancelled++;
        }

        // Soft-delete: retain the row for history, free active_* for reuse, force-logout all sessions.
        user.setStatus(UserEntity.AccountStatus.DELETED);
        user.setBlocked(true);
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setActiveEmail(null);
        user.setActivePhone(null);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(userId); // self
        user.setDeletedReason(StringUtils.hasText(request.getReason())
                ? request.getReason() : "Account closed by user.");
        user.setDeletedBookingsCancelled(bookingsCancelled);
        userRepository.save(user);

        log.info("UserService.deleteMe() completed - userId={} self-closed, {} booking(s) cancelled.",
                userId, bookingsCancelled);
        MessageResponse response = new MessageResponse();
        response.setMessage("Your account has been closed.");
        return response;
    }

    @Override
    @Transactional
    public AuthResponse changeRole(Long userId, ChangeRoleRequest request) {
        log.info("UserService.changeRole() called - userId={}, targetRole={}", userId, request.getTargetRole());
        UserEntity user = getEntityById(userId);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Incorrect password");
        }

        UserEntity.Role targetRole;
        try {
            targetRole = UserEntity.Role.valueOf(request.getTargetRole().getValue());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + request.getTargetRole());
        }

        if (targetRole == UserEntity.Role.ADMIN) {
            throw new IllegalArgumentException("Cannot self-assign ADMIN role");
        }
        if (user.getRole() == targetRole) {
            throw new IllegalArgumentException("Account is already " + targetRole.name());
        }

        user.setRole(targetRole);
        userRepository.save(user);

        String newToken = tokenProvider.generateToken(user.getId(), targetRole.name(), user.getTokenVersion());

        AuthResponse response = new AuthResponse();
        response.setToken(newToken);
        response.setRefreshToken(newToken);
        response.setUser(userMapper.toDto(user));

        log.info("UserService.changeRole() completed - userId={}, newRole={}", userId, targetRole);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserPage listUsers(int page, int size, String role, String search) {
        log.info("UserService.listUsers() called - page={}, size={}, role={}, search={}", page, size, role, search);
        Pageable pageable = PageRequest.of(page, size);
        UserEntity.Role roleEnum = null;
        if (StringUtils.hasText(role)) {
            try {
                roleEnum = UserEntity.Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        String searchParam = StringUtils.hasText(search) ? search : null;
        Page<UserEntity> entityPage = userRepository.findAllByRoleAndSearch(roleEnum, searchParam, pageable);

        UserPage dto = new UserPage();
        dto.setContent(entityPage.getContent().stream().map(userMapper::toDto).toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        log.info("UserService.listUsers() completed - total={}", entityPage.getTotalElements());
        return dto;
    }

    @Override
    @Transactional
    public UserDto blockUser(Long id) {
        log.info("UserService.blockUser() called - id={}", id);
        UserEntity user = getEntityById(id);
        user.setBlocked(true);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto unblockUser(Long id) {
        log.info("UserService.unblockUser() called - id={}", id);
        UserEntity user = getEntityById(id);
        user.setBlocked(false);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<AdminSummaryDto> listAdmins(Long actorId) {
        adminPermissionService.requireModerateHard(actorId); // SUPER_ADMIN only
        return userRepository.findByRole(UserEntity.Role.ADMIN).stream()
                .filter(u -> u.getStatus() != UserEntity.AccountStatus.DELETED)
                .sorted(java.util.Comparator.comparing(UserEntity::getId))
                .map(u -> toAdminSummary(u, actorId))
                .toList();
    }

    /** Map an ADMIN user to its summary DTO; effective role legacy-NULL ⇒ SUPER_ADMIN. */
    private AdminSummaryDto toAdminSummary(UserEntity u, Long actorId) {
        AdminSummaryDto dto = new AdminSummaryDto();
        dto.setId(u.getId());
        dto.setName(u.getName());
        dto.setEmail(u.getEmail());
        dto.setAvatarUrl(u.getAvatarUrl());
        // Effective role: legacy NULL ⇒ SUPER_ADMIN (mirrors AdminPermissionService.roleOf).
        dto.setAdminRole(u.getAdminRole() != null ? u.getAdminRole().name() : "SUPER_ADMIN");
        dto.setSelf(u.getId().equals(actorId));
        return dto;
    }

    /** Parse & validate an admin sub-role string into the enum, or throw a 400. */
    private UserEntity.AdminRole parseAdminRole(String raw) {
        try {
            return UserEntity.AdminRole.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new com.turfbook.backend.exception.BadRequestException(
                    "adminRole must be one of SUPER_ADMIN, SUPPORT, READ_ONLY.");
        }
    }

    @Override
    @Transactional
    public MessageResponse setAdminRole(Long actorId, Long targetUserId, SetAdminRoleRequest request) {
        adminPermissionService.requireModerateHard(actorId); // SUPER_ADMIN only
        UserEntity.AdminRole role = parseAdminRole(request.getAdminRole());
        UserEntity target = getEntityById(targetUserId);
        if (target.getRole() != UserEntity.Role.ADMIN) {
            throw new com.turfbook.backend.exception.ConflictException("Admin sub-roles apply only to ADMIN users.");
        }
        UserEntity.AdminRole previous = target.getAdminRole();
        target.setAdminRole(role);
        target.setTokenVersion(target.getTokenVersion() + 1); // re-issue so the new role takes effect
        userRepository.save(target);
        audit(actorId, target, "ADMIN_ROLE_CHANGE", null,
                previous != null ? previous.name() : null, role.name());
        log.info("UserService.setAdminRole() - actor={} set user={} adminRole={}", actorId, targetUserId, role);
        MessageResponse response = new MessageResponse();
        response.setMessage("Admin role updated to " + role.name() + ".");
        return response;
    }

    @Override
    @Transactional
    public AdminSummaryDto createAdmin(Long actorId, CreateAdminRequest request) {
        adminPermissionService.requireModerateHard(actorId); // SUPER_ADMIN only
        UserEntity.AdminRole role = parseAdminRole(request.getAdminRole());

        String email = request.getEmail().toLowerCase().trim();
        // Mirror the AuthService password policy so admin-created accounts are just as strong.
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new com.turfbook.backend.exception.BadRequestException("Password must be at least 8 characters.");
        }
        boolean hasLetter = request.getPassword().chars().anyMatch(Character::isLetter);
        boolean hasDigit = request.getPassword().chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new com.turfbook.backend.exception.BadRequestException(
                    "Password must contain at least one letter and one digit.");
        }
        // Uniqueness against any live (non-deleted) account, matched on the raw email column so it
        // holds even for a legacy row with a NULL active_email; freed (deleted) identifiers don't collide.
        if (userRepository.isEmailInUseByLiveAccount(email, UserEntity.AccountStatus.DELETED)) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        UserEntity admin = UserEntity.builder()
                .name(request.getName().trim())
                .email(email)
                .activeEmail(email)
                // Admins are created without a phone, but the column is NOT NULL. Store an empty
                // string (active_phone stays NULL so it never collides on the unique index).
                .phone("")
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserEntity.Role.ADMIN)
                .adminRole(role)
                .phoneVerified(false)
                .acceptedTermsAt(LocalDateTime.now())
                .build();
        admin = userRepository.save(admin);
        sendAdminWelcomeNotification(admin, role);
        audit(actorId, admin, "ADMIN_CREATE", null, null, role.name());

        log.info("UserService.createAdmin() - actor={} created admin={} adminRole={}", actorId, admin.getId(), role);
        return toAdminSummary(admin, actorId);
    }

    /** Seed a new/promoted admin's inbox so notifications are visible immediately and the pipeline is confirmed. */
    private void sendAdminWelcomeNotification(UserEntity admin, UserEntity.AdminRole role) {
        String roleLabel = switch (role) {
            case SUPER_ADMIN -> "Super Admin";
            case SUPPORT -> "Support";
            case READ_ONLY -> "Read Only";
        };
        notificationService.createNotification(
                admin,
                "Welcome to the admin team",
                "Your " + roleLabel + " admin access is active. Platform alerts — venue approvals, "
                        + "disputes and subscription events — will appear here.",
                NotificationEntity.NotificationType.SYSTEM);
    }

    @Override
    @Transactional
    public AdminSummaryDto promoteToAdmin(Long actorId, PromoteAdminRequest request) {
        adminPermissionService.requireModerateHard(actorId); // SUPER_ADMIN only
        UserEntity.AdminRole role = parseAdminRole(request.getAdminRole());

        String email = request.getEmail().toLowerCase().trim();
        UserEntity target = userRepository.findByActiveEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active account found for email: " + request.getEmail()));
        if (target.getStatus() == UserEntity.AccountStatus.DELETED) {
            throw new ConflictException("That account is closed and cannot be promoted.");
        }
        if (target.getRole() == UserEntity.Role.ADMIN) {
            throw new ConflictException("That user is already an admin.");
        }

        UserEntity.Role priorRole = target.getRole();
        target.setRole(UserEntity.Role.ADMIN);
        target.setAdminRole(role);
        target.setTokenVersion(target.getTokenVersion() + 1); // re-issue so the new role takes effect
        userRepository.save(target);
        sendAdminWelcomeNotification(target, role);
        audit(actorId, target, "ADMIN_PROMOTE", null, priorRole.name(), "ADMIN:" + role.name());

        log.info("UserService.promoteToAdmin() - actor={} promoted user={} adminRole={}", actorId, target.getId(), role);
        return toAdminSummary(target, actorId);
    }

    @Override
    @Transactional
    public MessageResponse removeAdmin(Long actorId, Long targetUserId, String mode) {
        adminPermissionService.requireModerateHard(actorId); // SUPER_ADMIN only
        if (targetUserId.equals(actorId)) {
            throw new com.turfbook.backend.exception.BadRequestException(
                    "You can't remove your own admin access — ask another super-admin.");
        }
        UserEntity target = getEntityById(targetUserId);
        if (target.getRole() != UserEntity.Role.ADMIN) {
            throw new ConflictException("That user is not an admin.");
        }

        String prevAdminRole = target.getAdminRole() != null ? target.getAdminRole().name() : null;
        String normalized = mode == null ? "demote" : mode.trim().toLowerCase();
        MessageResponse response = new MessageResponse();
        if ("deactivate".equals(normalized)) {
            // Soft-delete: retain the row, free active_* for reuse, force-logout all sessions.
            target.setStatus(UserEntity.AccountStatus.DELETED);
            target.setBlocked(true);
            target.setTokenVersion(target.getTokenVersion() + 1);
            target.setActiveEmail(null);
            target.setActivePhone(null);
            target.setDeletedAt(LocalDateTime.now());
            target.setDeletedBy(actorId);
            target.setDeletedReason("Admin account deactivated by super-admin.");
            userRepository.save(target);
            audit(actorId, target, "ADMIN_DEACTIVATE", "Admin account deactivated by super-admin.",
                    prevAdminRole, "DELETED");
            log.info("UserService.removeAdmin() - actor={} deactivated admin={}", actorId, targetUserId);
            response.setMessage("Admin account deactivated.");
        } else if ("demote".equals(normalized)) {
            // Revoke admin access but keep the account: revert to a regular player.
            target.setRole(UserEntity.Role.PLAYER);
            target.setAdminRole(null);
            target.setTokenVersion(target.getTokenVersion() + 1);
            userRepository.save(target);
            audit(actorId, target, "ADMIN_DEMOTE", null, prevAdminRole, "PLAYER");
            log.info("UserService.removeAdmin() - actor={} demoted admin={} to player", actorId, targetUserId);
            response.setMessage("Admin access revoked. The account is now a regular user.");
        } else {
            throw new com.turfbook.backend.exception.BadRequestException(
                    "mode must be either 'demote' or 'deactivate'.");
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    /**
     * Append a row to the admin audit trail for a sensitive admin-account action
     * (create / promote / role-change / removal). Mirrors the moderation audit in
     * AdminPlayerServiceImpl/AdminOwnerServiceImpl so all admin actions land in one table.
     */
    private void audit(Long actorId, UserEntity target, String action, String reason, String from, String to) {
        UserEntity actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
        auditRepository.save(AdminAuditEntity.builder()
                .actor(actor).target(target).action(action).reason(reason)
                .fromStatus(from).toStatus(to).build());
    }
}
