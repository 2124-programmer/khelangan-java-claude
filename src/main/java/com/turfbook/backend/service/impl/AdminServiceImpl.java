package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.AdminStatsDto;
import com.turfbook.backend.dto.PlatformSettingsDto;
import com.turfbook.backend.dto.UpdateSettingsRequest;
import com.turfbook.backend.entity.AdminAuditEntity;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.DisputeEntity;
import com.turfbook.backend.entity.PlatformSettingsEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.repository.*;
import com.turfbook.backend.service.AdminPermissionService;
import com.turfbook.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final DisputeRepository disputeRepository;
    private final PlatformSettingsRepository settingsRepository;
    private final AdminPermissionService adminPermissionService;
    private final AdminAuditRepository auditRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDto getAdminStats() {
        log.info("AdminService.getAdminStats() called");
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        long bookingsToday = bookingRepository.countTodayBookings(todayStart, todayEnd);
        long revenueToday = bookingRepository.sumRevenue(todayStart, todayEnd, BookingEntity.PaymentStatus.SUCCESS);

        long newUsers = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null
                        && !u.getCreatedAt().isBefore(todayStart)
                        && u.getCreatedAt().isBefore(todayEnd))
                .count();

        long activeVenues = venueRepository.countByStatus(VenueEntity.VenueStatus.LIVE);
        long pendingApprovals = venueRepository.countByStatusIn(List.of(VenueEntity.VenueStatus.PENDING));
        long openDisputes = disputeRepository.countByStatus(DisputeEntity.DisputeStatus.OPEN);

        AdminStatsDto dto = new AdminStatsDto();
        dto.setBookingsToday(bookingsToday);
        dto.setRevenueToday(revenueToday);
        dto.setNewUsers(newUsers);
        dto.setActiveVenues(activeVenues);
        dto.setPendingApprovals(pendingApprovals);
        dto.setOpenDisputes(openDisputes);

        log.info("AdminService.getAdminStats() completed - bookingsToday={}, revenueToday={}, openDisputes={}",
                bookingsToday, revenueToday, openDisputes);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformSettingsDto getSettings() {
        log.info("AdminService.getSettings() called");
        adminPermissionService.requireSuperAdmin(adminPermissionService.currentActorId()); // SUPER_ADMIN only
        PlatformSettingsEntity settings = settingsRepository.findById(1L)
                .orElseGet(this::createDefaultSettings);
        return toDto(settings);
    }

    @Override
    @Transactional
    public PlatformSettingsDto updateSettings(UpdateSettingsRequest request) {
        log.info("AdminService.updateSettings() called");
        Long actorId = adminPermissionService.currentActorId();
        adminPermissionService.requireSuperAdmin(actorId); // SUPER_ADMIN only
        PlatformSettingsEntity settings = settingsRepository.findById(1L)
                .orElseGet(this::createDefaultSettings);

        // Capture before-values so the audit trail records what actually changed.
        List<String> changes = new ArrayList<>();
        if (request.getCommissionPercent() != null
                && request.getCommissionPercent() != settings.getCommissionPercent()) {
            changes.add("commission% " + settings.getCommissionPercent() + "→" + request.getCommissionPercent());
            settings.setCommissionPercent(request.getCommissionPercent());
        }
        if (request.getConvenienceFee() != null
                && request.getConvenienceFee() != settings.getConvenienceFee()) {
            changes.add("convenienceFee " + settings.getConvenienceFee() + "→" + request.getConvenienceFee());
            settings.setConvenienceFee(request.getConvenienceFee());
        }
        if (request.getMaintenanceMode() != null
                && request.getMaintenanceMode() != settings.isMaintenanceMode()) {
            changes.add("maintenanceMode " + settings.isMaintenanceMode() + "→" + request.getMaintenanceMode());
            settings.setMaintenanceMode(request.getMaintenanceMode());
        }
        if (request.getAutoApproveVenues() != null
                && request.getAutoApproveVenues() != settings.isAutoApproveVenues()) {
            changes.add("autoApproveVenues " + settings.isAutoApproveVenues() + "→" + request.getAutoApproveVenues());
            settings.setAutoApproveVenues(request.getAutoApproveVenues());
        }

        PlatformSettingsDto result = toDto(settingsRepository.save(settings));
        auditSettingsChange(actorId, changes);
        log.info("AdminService.updateSettings() completed - commissionPercent={}, convenienceFee={}",
                settings.getCommissionPercent(), settings.getConvenienceFee());
        return result;
    }

    /**
     * Record a platform-settings change in the admin audit trail. Settings have no target user,
     * so the action is self-referenced to the acting super-admin (keeps the NOT-NULL target while
     * still recording who changed what). The changed fields (old→new) go in metadata.
     */
    private void auditSettingsChange(Long actorId, List<String> changes) {
        if (changes.isEmpty()) return; // no-op update — nothing worth recording
        UserEntity actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
        if (actor == null) return; // can't satisfy the non-null target without the actor
        String metadata = String.join(", ", changes);
        if (metadata.length() > 500) metadata = metadata.substring(0, 500); // column cap
        auditRepository.save(AdminAuditEntity.builder()
                .actor(actor).target(actor)
                .action("SETTINGS_UPDATE").metadata(metadata)
                .build());
    }

    private PlatformSettingsEntity createDefaultSettings() {
        PlatformSettingsEntity s = PlatformSettingsEntity.builder()
                .id(1L)
                .commissionPercent(10)
                .convenienceFee(20)
                .maintenanceMode(false)
                .autoApproveVenues(false)
                .build();
        return settingsRepository.save(s);
    }

    private PlatformSettingsDto toDto(PlatformSettingsEntity entity) {
        PlatformSettingsDto dto = new PlatformSettingsDto();
        dto.setId(entity.getId());
        dto.setCommissionPercent(entity.getCommissionPercent());
        dto.setConvenienceFee(entity.getConvenienceFee());
        dto.setMaintenanceMode(entity.isMaintenanceMode());
        dto.setAutoApproveVenues(entity.isAutoApproveVenues());
        return dto;
    }
}
