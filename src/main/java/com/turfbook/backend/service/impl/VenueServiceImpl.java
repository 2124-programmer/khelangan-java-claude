package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.*;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.mapper.CourtMapper;
import com.turfbook.backend.mapper.SlotMapper;
import com.turfbook.backend.mapper.VenueMapper;
import com.turfbook.backend.repository.*;
import com.turfbook.backend.service.NotificationService;
import com.turfbook.backend.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final SportRepository sportRepository;
    private final BookingRepository bookingRepository;
    private final PayoutRepository payoutRepository;
    private final VenueMapper venueMapper;
    private final CourtMapper courtMapper;
    private final SlotMapper slotMapper;
    private final NotificationService notificationService;

    // ─── Venues ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public VenueSummaryPage listVenues(String city, String sport, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String cityParam = StringUtils.hasText(city) ? city : null;
        String sportParam = StringUtils.hasText(sport) ? sport : null;
        String searchParam = StringUtils.hasText(search) ? search : null;

        Page<VenueEntity> entityPage = venueRepository.findLiveVenues(
                VenueEntity.VenueStatus.LIVE, cityParam, sportParam, searchParam, pageable);
        return toVenueSummaryPage(entityPage);
    }

    @Override
    @Transactional(readOnly = true)
    public VenueDetailDto getVenue(Long id) {
        VenueEntity venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));
        return venueMapper.toDetailDto(venue);
    }

    @Override
    @Transactional
    public VenueDetailDto createVenue(Long ownerId, CreateVenueRequest request) {
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        Set<SportEntity> sports = new HashSet<>();
        if (request.getSportIds() != null) {
            for (Long sportId : request.getSportIds()) {
                SportEntity sport = sportRepository.findById(sportId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sport", "id", sportId));
                sports.add(sport);
            }
        }

        VenueEntity venue = VenueEntity.builder()
                .owner(owner)
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .description(request.getDescription())
                .pricePerSlot(request.getPricePerSlot() != null ? request.getPricePerSlot() : 0)
                .amenities(request.getAmenities() != null ? request.getAmenities() : new ArrayList<>())
                .lat(request.getLat() != null ? request.getLat() : 0.0)
                .lng(request.getLng() != null ? request.getLng() : 0.0)
                .coverPhoto(request.getCoverPhoto())
                .photos(request.getPhotos() != null ? request.getPhotos() : new ArrayList<>())
                .sports(sports)
                .status(VenueEntity.VenueStatus.PENDING)
                .build();

        venue = venueRepository.save(venue);

        // Create courts if provided
        if (request.getCourts() != null) {
            for (CreateCourtRequest courtReq : request.getCourts()) {
                createCourtInternal(venue, courtReq);
            }
            venue = venueRepository.findById(venue.getId()).orElseThrow();
        }

        return venueMapper.toDetailDto(venue);
    }

    @Override
    @Transactional
    public VenueDetailDto updateVenue(Long id, Long ownerId, UpdateVenueRequest request) {
        VenueEntity venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));

        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }

        if (StringUtils.hasText(request.getName())) venue.setName(request.getName());
        if (StringUtils.hasText(request.getDescription())) venue.setDescription(request.getDescription());
        if (request.getPricePerSlot() != null) venue.setPricePerSlot(request.getPricePerSlot());
        if (request.getAmenities() != null) venue.setAmenities(request.getAmenities());
        if (request.getCoverPhoto() != null) venue.setCoverPhoto(request.getCoverPhoto());
        if (request.getPhotos() != null) venue.setPhotos(request.getPhotos());

        return venueMapper.toDetailDto(venueRepository.save(venue));
    }

    @Override
    @Transactional
    public VenueDetailDto updateVenueStatus(Long id, VenueStatusRequest request) {
        VenueEntity venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));

        VenueEntity.VenueStatus newStatus;
        try {
            // Generated enum value() or name() returns the string value
            String statusStr = request.getStatus() != null ? request.getStatus().toString() : "";
            newStatus = VenueEntity.VenueStatus.valueOf(statusStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid venue status: " + request.getStatus());
        }

        VenueEntity.VenueStatus oldStatus = venue.getStatus();
        venue.setStatus(newStatus);
        venue = venueRepository.save(venue);

        // Notifications to owner
        if (newStatus == VenueEntity.VenueStatus.LIVE && oldStatus == VenueEntity.VenueStatus.PENDING) {
            notificationService.createNotification(
                    venue.getOwner(),
                    "Venue Approved!",
                    String.format("Your venue '%s' has been approved and is now live on TurfBook.", venue.getName()),
                    NotificationEntity.NotificationType.SYSTEM
            );
        } else if (newStatus == VenueEntity.VenueStatus.REJECTED) {
            String reason = StringUtils.hasText(request.getRejectionReason())
                    ? request.getRejectionReason() : "Does not meet our guidelines";
            notificationService.createNotification(
                    venue.getOwner(),
                    "Venue Rejected",
                    String.format("Your venue '%s' was rejected. Reason: %s", venue.getName(), reason),
                    NotificationEntity.NotificationType.SYSTEM
            );
        }

        return venueMapper.toDetailDto(venue);
    }

    @Override
    @Transactional(readOnly = true)
    public VenueSummaryPage listOwnerVenues(Long ownerId, int page, int size) {
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));
        Pageable pageable = PageRequest.of(page, size);
        return toVenueSummaryPage(venueRepository.findByOwner(owner, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public VenueSummaryPage adminListVenues(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VenueEntity> entityPage;
        if (StringUtils.hasText(status)) {
            try {
                VenueEntity.VenueStatus venueStatus = VenueEntity.VenueStatus.valueOf(status.toUpperCase());
                entityPage = venueRepository.findByStatus(venueStatus, pageable);
            } catch (IllegalArgumentException e) {
                entityPage = venueRepository.findAll(pageable);
            }
        } else {
            entityPage = venueRepository.findAll(pageable);
        }
        return toVenueSummaryPage(entityPage);
    }

    // ─── Courts ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CourtDto> listCourts(Long venueId) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        return courtRepository.findByVenue(venue).stream().map(courtMapper::toDto).toList();
    }

    @Override
    @Transactional
    public CourtDto createCourt(Long venueId, Long ownerId, CreateCourtRequest request) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }
        return courtMapper.toDto(createCourtInternal(venue, request));
    }

    @Override
    @Transactional
    public CourtDto updateCourt(Long venueId, Long courtId, Long ownerId, UpdateCourtRequest request) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }
        CourtEntity court = courtRepository.findByIdAndVenue(courtId, venue)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", courtId));

        if (StringUtils.hasText(request.getName())) court.setName(request.getName());
        if (request.getPricePerSlot() != null) court.setPricePerSlot(request.getPricePerSlot());
        if (request.getPeakPrice() != null) court.setPeakPrice(request.getPeakPrice());
        if (request.getType() != null) court.setType(request.getType());
        if (request.getSportId() != null) {
            SportEntity sport = sportRepository.findById(request.getSportId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sport", "id", request.getSportId()));
            court.setSport(sport);
        }

        return courtMapper.toDto(courtRepository.save(court));
    }

    @Override
    @Transactional
    public void deleteCourt(Long venueId, Long courtId, Long ownerId) {
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }
        CourtEntity court = courtRepository.findByIdAndVenue(courtId, venue)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", courtId));
        courtRepository.delete(court);
    }

    // ─── Slots ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SlotDto> listSlots(Long courtId, LocalDate date) {
        CourtEntity court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", courtId));
        return slotRepository.findByCourtAndDateOrderByStartTime(court, date)
                .stream().map(slotMapper::toDto).toList();
    }

    @Override
    @Transactional
    public SlotDto blockSlot(Long slotId, Long ownerId) {
        SlotEntity slot = getSlotOwnedBy(slotId, ownerId);
        slot.setStatus(SlotEntity.SlotStatus.BLOCKED);
        return slotMapper.toDto(slotRepository.save(slot));
    }

    @Override
    @Transactional
    public SlotDto unblockSlot(Long slotId, Long ownerId) {
        SlotEntity slot = getSlotOwnedBy(slotId, ownerId);
        slot.setStatus(SlotEntity.SlotStatus.AVAILABLE);
        return slotMapper.toDto(slotRepository.save(slot));
    }

    @Override
    @Transactional
    public List<SlotDto> bulkBlockSlots(Long courtId, Long ownerId, BulkBlockRequest request) {
        CourtEntity court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", courtId));
        if (!court.getVenue().getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this court's venue");
        }
        slotRepository.bulkUpdateStatusByCourtAndDate(court, request.getDate(),
                SlotEntity.SlotStatus.BLOCKED, SlotEntity.SlotStatus.AVAILABLE);
        return slotRepository.findByCourtAndDateOrderByStartTime(court, request.getDate())
                .stream().map(slotMapper::toDto).toList();
    }

    // ─── Owner Stats ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OwnerStatsDto getOwnerStats(Long ownerId) {
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long todayBookings = bookingRepository.countByOwnerAndDateRange(owner, todayStart, todayEnd);
        long todayRevenue = bookingRepository.sumRevenueByOwnerAndDateRange(
                owner, todayStart, todayEnd, BookingEntity.PaymentStatus.SUCCESS);
        long weekRevenue = bookingRepository.sumRevenueByOwnerAndDateRange(
                owner, weekStart, todayEnd, BookingEntity.PaymentStatus.SUCCESS);
        long monthRevenue = bookingRepository.sumRevenueByOwnerAndDateRange(
                owner, monthStart, todayEnd, BookingEntity.PaymentStatus.SUCCESS);
        long pendingPayout = payoutRepository.sumPendingByOwner(owner, PayoutEntity.PayoutStatus.PENDING);

        OwnerStatsDto dto = new OwnerStatsDto();
        dto.setTodayBookings(todayBookings);
        dto.setTodayRevenue(todayRevenue);
        dto.setWeekRevenue(weekRevenue);
        dto.setMonthRevenue(monthRevenue);
        dto.setPendingPayout(pendingPayout);
        return dto;
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private CourtEntity createCourtInternal(VenueEntity venue, CreateCourtRequest req) {
        SportEntity sport = sportRepository.findById(req.getSportId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport", "id", req.getSportId()));

        CourtEntity court = CourtEntity.builder()
                .venue(venue)
                .name(req.getName())
                .sport(sport)
                .type(req.getType())
                .pricePerSlot(req.getPricePerSlot() != null ? req.getPricePerSlot() : 0)
                .peakPrice(req.getPeakPrice() != null ? req.getPeakPrice() : 0)
                .build();

        return courtRepository.save(court);
    }

    private SlotEntity getSlotOwnedBy(Long slotId, Long ownerId) {
        SlotEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", "id", slotId));
        if (!slot.getCourt().getVenue().getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this slot's venue");
        }
        return slot;
    }

    private VenueSummaryPage toVenueSummaryPage(Page<VenueEntity> entityPage) {
        VenueSummaryPage dto = new VenueSummaryPage();
        dto.setContent(entityPage.getContent().stream().map(venueMapper::toSummaryDto).toList());
        dto.setTotalElements(entityPage.getTotalElements());
        dto.setTotalPages(entityPage.getTotalPages());
        dto.setSize(entityPage.getSize());
        dto.setNumber(entityPage.getNumber());
        return dto;
    }
}
