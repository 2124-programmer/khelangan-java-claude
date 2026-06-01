package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.AdminStatsDto;
import com.turfbook.backend.dto.PlatformSettingsDto;
import com.turfbook.backend.dto.UpdateSettingsRequest;
import com.turfbook.backend.entity.BookingEntity;
import com.turfbook.backend.entity.DisputeEntity;
import com.turfbook.backend.entity.PlatformSettingsEntity;
import com.turfbook.backend.entity.UserEntity;
import com.turfbook.backend.entity.VenueEntity;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.repository.*;
import com.turfbook.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final DisputeRepository disputeRepository;
    private final PlatformSettingsRepository settingsRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDto getAdminStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        long bookingsToday = bookingRepository.countTodayBookings(todayStart, todayEnd);
        long revenueToday = bookingRepository.sumRevenue(todayStart, todayEnd, BookingEntity.PaymentStatus.SUCCESS);

        // New users today
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
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformSettingsDto getSettings() {
        PlatformSettingsEntity settings = settingsRepository.findById(1L)
                .orElseGet(this::createDefaultSettings);
        return toDto(settings);
    }

    @Override
    @Transactional
    public PlatformSettingsDto updateSettings(UpdateSettingsRequest request) {
        PlatformSettingsEntity settings = settingsRepository.findById(1L)
                .orElseGet(this::createDefaultSettings);

        if (request.getCommissionPercent() != null) settings.setCommissionPercent(request.getCommissionPercent());
        if (request.getConvenienceFee() != null) settings.setConvenienceFee(request.getConvenienceFee());
        if (request.getMaintenanceMode() != null) settings.setMaintenanceMode(request.getMaintenanceMode());
        if (request.getAutoApproveVenues() != null) settings.setAutoApproveVenues(request.getAutoApproveVenues());

        return toDto(settingsRepository.save(settings));
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
