package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.AuthResponse;
import com.turfbook.backend.dto.ChangeRoleRequest;
import com.turfbook.backend.dto.DeleteAccountRequest;
import com.turfbook.backend.dto.MessageResponse;
import com.turfbook.backend.dto.UpdateProfileRequest;
import com.turfbook.backend.dto.UserDto;
import com.turfbook.backend.dto.UserPage;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.NotificationEntity;
import com.turfbook.backend.entity.SlotEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.mapper.UserMapper;
import com.turfbook.backend.repository.BookingRepository;
import com.turfbook.backend.repository.SlotRepository;
import com.turfbook.backend.repository.UserRepository;
import com.turfbook.backend.security.JwtTokenProvider;
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
    public UserEntity getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
