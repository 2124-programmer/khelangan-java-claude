package com.turfbook.backend.service.impl;

import com.turfbook.backend.dto.*;
import com.turfbook.backend.entity.*;
import com.turfbook.backend.exception.ConflictException;
import com.turfbook.backend.exception.ForbiddenException;
import com.turfbook.backend.exception.ResourceNotFoundException;
import com.turfbook.backend.exception.UnauthorizedException;
import com.turfbook.backend.mapper.CourtMapper;
import com.turfbook.backend.mapper.SlotMapper;
import com.turfbook.backend.mapper.VenueMapper;
import com.turfbook.backend.repository.*;
import com.turfbook.backend.service.NotificationService;
import com.turfbook.backend.service.VenueService;
import com.turfbook.backend.service.subscription.SubscriptionGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
    private final SubscriptionGate subscriptionGate;

    // ─── Venues ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public VenueSummaryPage listVenues(String city, String sport, String search, int page, int size) {
        log.info("VenueService.listVenues() called - city={}, sport={}, search={}", city, sport, search);
        Pageable pageable = PageRequest.of(page, size);
        String cityParam = StringUtils.hasText(city) ? city : null;
        Long sportId = null;
        if (StringUtils.hasText(sport)) {
            try { sportId = Long.parseLong(sport); } catch (NumberFormatException ignored) {}
        }
        String searchParam = StringUtils.hasText(search) ? search : null;

        Page<VenueEntity> entityPage = venueRepository.findLiveVenues(
                VenueEntity.VenueStatus.LIVE, cityParam, sportId, searchParam, pageable);
        return toVenueSummaryPage(entityPage);
    }

    @Override
    @Transactional(readOnly = true)
    public VenueDetailDto getVenue(Long id) {
        log.info("VenueService.getVenue() called - id={}", id);
        VenueEntity venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));
        return venueMapper.toDetailDto(venue);
    }

    @Override
    @Transactional
    public VenueDetailDto createVenue(Long ownerId, CreateVenueRequest request) {
        log.info("VenueService.createVenue() called - ownerId={}, name={}", ownerId, request.getName());
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        validateVenueRequest(request.getName(), request.getOpenTime(), request.getCloseTime(),
                request.getPricePerHour(), request.getContactPhone(), request.getContactEmail(),
                request.getPincode());

        if (venueRepository.existsByOwnerAndName(owner, request.getName())) {
            throw new ConflictException("You already have a venue named '" + request.getName() + "'");
        }

        Set<SportEntity> sports = resolveSports(request.getSportIds());

        String openTime  = request.getOpenTime()  != null ? request.getOpenTime()  : "05:00";
        String closeTime = request.getCloseTime() != null ? request.getCloseTime() : "23:00";

        VenueEntity venue = VenueEntity.builder()
                .owner(owner)
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .description(request.getDescription())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .openTime(openTime)
                .closeTime(closeTime)
                .pricePerHour(request.getPricePerHour() != null ? request.getPricePerHour() : 0)
                .amenities(request.getAmenities() != null ? request.getAmenities() : new ArrayList<>())
                .lat(request.getLat() != null ? request.getLat() : 0.0)
                .lng(request.getLng() != null ? request.getLng() : 0.0)
                .coverPhoto(request.getCoverPhoto())
                .photos(request.getPhotos() != null ? request.getPhotos() : new ArrayList<>())
                .sports(sports)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .status(VenueEntity.VenueStatus.PENDING)
                .build();

        venue = venueRepository.save(venue);

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
        log.info("VenueService.updateVenue() called - id={}, ownerId={}", id, ownerId);
        VenueEntity venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));

        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }

        // Validate fields being updated
        String newOpenTime  = StringUtils.hasText(request.getOpenTime())  ? request.getOpenTime()  : venue.getOpenTime();
        String newCloseTime = StringUtils.hasText(request.getCloseTime()) ? request.getCloseTime() : venue.getCloseTime();
        validateTimeFormat(newOpenTime, "openTime");
        validateTimeFormat(newCloseTime, "closeTime");
        validateTimeWindow(newOpenTime, newCloseTime);
        if (request.getPricePerHour() != null && request.getPricePerHour() < 0) {
            throw new IllegalArgumentException("pricePerHour must be ≥ 0");
        }
        if (StringUtils.hasText(request.getContactPhone())) {
            validatePhone(request.getContactPhone());
        }
        if (StringUtils.hasText(request.getPincode())) {
            validatePincode(request.getPincode());
        }

        if (StringUtils.hasText(request.getName()) && !request.getName().equals(venue.getName())) {
            if (venueRepository.existsByOwnerAndNameAndIdNot(venue.getOwner(), request.getName(), id)) {
                throw new ConflictException("You already have a venue named '" + request.getName() + "'");
            }
            venue.setName(request.getName());
        }
        if (StringUtils.hasText(request.getDescription())) venue.setDescription(request.getDescription());
        if (StringUtils.hasText(request.getAddress())) venue.setAddress(request.getAddress());
        if (StringUtils.hasText(request.getCity())) venue.setCity(request.getCity());
        if (StringUtils.hasText(request.getContactPhone())) venue.setContactPhone(request.getContactPhone());
        if (StringUtils.hasText(request.getContactEmail())) venue.setContactEmail(request.getContactEmail());
        if (StringUtils.hasText(request.getState())) venue.setState(request.getState());
        if (StringUtils.hasText(request.getPincode())) venue.setPincode(request.getPincode());
        if (StringUtils.hasText(request.getOpenTime())) venue.setOpenTime(request.getOpenTime());
        if (StringUtils.hasText(request.getCloseTime())) venue.setCloseTime(request.getCloseTime());
        if (request.getPricePerHour() != null) venue.setPricePerHour(request.getPricePerHour());
        if (request.getAmenities() != null) venue.setAmenities(request.getAmenities());
        if (request.getCoverPhoto() != null) venue.setCoverPhoto(request.getCoverPhoto());
        if (request.getPhotos() != null) {
            int photoLimit = subscriptionGate.photoLimitFor(id);
            if (photoLimit > 0 && request.getPhotos().size() > photoLimit) {
                throw new ForbiddenException(
                        "Your plan allows up to " + photoLimit + " photos. Upgrade to add more.");
            }
            venue.setPhotos(request.getPhotos());
        }
        if (request.getSportIds() != null && !request.getSportIds().isEmpty()) {
            venue.setSports(resolveSports(request.getSportIds()));
        }
        if (request.getLat() != null) venue.setLat(request.getLat());
        if (request.getLng() != null) venue.setLng(request.getLng());
        if (request.getIsActive() != null) venue.setActive(request.getIsActive());

        return venueMapper.toDetailDto(venueRepository.save(venue));
    }

    @Override
    @Transactional
    public VenueDetailDto updateVenueStatus(Long id, VenueStatusRequest request) {
        log.info("VenueService.updateVenueStatus() called - id={}, status={}", id, request.getStatus());
        VenueEntity venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));

        VenueEntity.VenueStatus newStatus;
        try {
            String statusStr = request.getStatus() != null ? request.getStatus().toString() : "";
            newStatus = VenueEntity.VenueStatus.valueOf(statusStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid venue status: " + request.getStatus());
        }

        VenueEntity.VenueStatus oldStatus = venue.getStatus();
        venue.setStatus(newStatus);
        venue = venueRepository.save(venue);

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
        log.info("VenueService.listOwnerVenues() called - ownerId={}", ownerId);
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));
        Pageable pageable = PageRequest.of(page, size);
        return toVenueSummaryPage(venueRepository.findByOwner(owner, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public VenueSummaryPage adminListVenues(int page, int size, String status) {
        log.info("VenueService.adminListVenues() called - status={}", status);
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
        log.info("VenueService.listCourts() called - venueId={}", venueId);
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        return courtRepository.findByVenue(venue).stream().map(courtMapper::toDto).toList();
    }

    @Override
    @Transactional
    public CourtDto createCourt(Long venueId, Long ownerId, CreateCourtRequest request) {
        log.info("VenueService.createCourt() called - venueId={}, ownerId={}", venueId, ownerId);
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }
        // Enforce the active plan's court limit (409 if no active plan or limit reached).
        subscriptionGate.assertCanAddCourt(venueId);
        return courtMapper.toDto(createCourtInternal(venue, request));
    }

    @Override
    @Transactional
    public CourtDto updateCourt(Long venueId, Long courtId, Long ownerId, UpdateCourtRequest request) {
        log.info("VenueService.updateCourt() called - venueId={}, courtId={}, ownerId={}", venueId, courtId, ownerId);
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));
        if (!venue.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this venue");
        }
        CourtEntity court = courtRepository.findByIdAndVenue(courtId, venue)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", courtId));

        if (StringUtils.hasText(request.getName()) && !request.getName().equals(court.getName())) {
            if (courtRepository.existsByVenueAndNameAndIdNot(venue, request.getName(), courtId)) {
                throw new ConflictException("This venue already has a court named '" + request.getName() + "'");
            }
            court.setName(request.getName());
        }
        if (request.getSportId() != null) {
            SportEntity sport = sportRepository.findById(request.getSportId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sport", "id", request.getSportId()));
            validateCourtSport(venue, sport);
            court.setSport(sport);
        }
        if (request.getType() != null) court.setType(request.getType());
        if (request.getPeakPrice() != null) court.setPeakPrice(request.getPeakPrice());
        if (request.getIsActive() != null) court.setActive(request.getIsActive());
        if (request.getSlotDurationMins() != null) court.setSlotDurationMins(request.getSlotDurationMins());

        // Handle time overrides — explicit null means "revert to inherit"
        if (request.getOpenTime() != null || request.getCloseTime() != null) {
            String newOpen  = request.getOpenTime()  != null ? request.getOpenTime()  : court.getOpenTime();
            String newClose = request.getCloseTime() != null ? request.getCloseTime() : court.getCloseTime();
            // Empty string = revert to inherit
            if ("".equals(newOpen)) {
                court.setOpenTime(null);
            } else if (StringUtils.hasText(newOpen)) {
                validateTimeFormat(newOpen, "openTime");
                court.setOpenTime(newOpen);
            }
            if ("".equals(newClose)) {
                court.setCloseTime(null);
            } else if (StringUtils.hasText(newClose)) {
                validateTimeFormat(newClose, "closeTime");
                court.setCloseTime(newClose);
            }
            // Validate final window vs venue
            String effectiveOpen  = court.getOpenTime()  != null ? court.getOpenTime()  : venue.getOpenTime();
            String effectiveClose = court.getCloseTime() != null ? court.getCloseTime() : venue.getCloseTime();
            validateTimeWindow(effectiveOpen, effectiveClose);
            if (court.getOpenTime() != null || court.getCloseTime() != null) {
                validateCourtTimeWithinVenue(court.getOpenTime(), court.getCloseTime(), venue);
            }
        }

        // pricePerHour: null string from request means "revert to inherit"
        if (request.getPricePerHour() != null) {
            if (request.getPricePerHour() < 0) throw new IllegalArgumentException("pricePerHour must be ≥ 0");
            court.setPricePerHour(request.getPricePerHour());
        }

        return courtMapper.toDto(courtRepository.save(court));
    }

    @Override
    @Transactional
    public void deleteCourt(Long venueId, Long courtId, Long ownerId) {
        log.info("VenueService.deleteCourt() called - venueId={}, courtId={}, ownerId={}", venueId, courtId, ownerId);
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
        log.info("VenueService.listSlots() called - courtId={}, date={}", courtId, date);
        CourtEntity court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", courtId));

        int openHour  = Integer.parseInt(court.effectiveOpenTime().split(":")[0]);
        int closeHour = Integer.parseInt(court.effectiveCloseTime().split(":")[0]);

        // Index existing DB slots by startTime (only BLOCKED/HELD/BOOKED rows live here now)
        Map<LocalTime, SlotEntity> existingByStart = slotRepository
                .findByCourtAndDateOrderByStartTime(court, date)
                .stream()
                .collect(Collectors.toMap(SlotEntity::getStartTime, s -> s));

        // Derive BOOKED/HELD from active bookings (CONFIRMED wins over PENDING for the same slot)
        Map<LocalTime, BookingEntity.BookingStatus> bookingByStart =
                bookingRepository.findByCourtAndDateAndStatusIn(
                        court, date,
                        List.of(BookingEntity.BookingStatus.CONFIRMED, BookingEntity.BookingStatus.PENDING))
                .stream()
                .collect(Collectors.toMap(
                        BookingEntity::getStartTime,
                        BookingEntity::getStatus,
                        (a, b) -> BookingEntity.BookingStatus.CONFIRMED.equals(a) ? a : b
                ));

        List<SlotDto> result = new ArrayList<>();
        for (int h = openHour; h < closeHour; h++) {
            LocalTime start = LocalTime.of(h, 0);
            LocalTime end   = LocalTime.of(h + 1, 0);

            SlotEntity slot = existingByStart.get(start);

            if (slot == null) {
                // No DB record — return a virtual AVAILABLE slot without persisting
                SlotDto dto = new SlotDto();
                dto.setId(null);
                dto.setCourtId(courtId);
                dto.setDate(date);
                dto.setStartTime(start.toString());
                dto.setEndTime(end.toString());
                dto.setStatus(SlotDto.StatusEnum.AVAILABLE);
                dto.setPrice(court.effectivePricePerHour());
                result.add(dto);
                continue;
            }

            // DB slot exists — BLOCKED is owner-managed; BOOKED/HELD derived from bookings
            SlotDto.StatusEnum status;
            if (slot.getStatus() == SlotEntity.SlotStatus.BLOCKED) {
                status = SlotDto.StatusEnum.BLOCKED;
            } else {
                BookingEntity.BookingStatus bs = bookingByStart.get(start);
                if (BookingEntity.BookingStatus.CONFIRMED.equals(bs)) {
                    status = SlotDto.StatusEnum.BOOKED;
                } else if (BookingEntity.BookingStatus.PENDING.equals(bs)) {
                    status = SlotDto.StatusEnum.HELD;
                } else {
                    status = SlotDto.StatusEnum.AVAILABLE;
                }
            }

            SlotDto dto = new SlotDto();
            dto.setId(slot.getId());
            dto.setCourtId(courtId);
            dto.setDate(date);
            dto.setStartTime(start.toString());
            dto.setEndTime(end.toString());
            dto.setStatus(status);
            dto.setPrice(slot.getPrice());
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtSlotsDto> listSlotsByVenue(Long venueId, LocalDate date, Long sportId) {
        log.info("VenueService.listSlotsByVenue() called - venueId={}, date={}, sportId={}", venueId, date, sportId);
        VenueEntity venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));

        List<CourtEntity> courts = courtRepository.findByVenue(venue);
        if (sportId != null) {
            courts = courts.stream()
                    .filter(c -> c.getSport().getId().equals(sportId))
                    .toList();
        }

        return courts.stream().map(court -> {
            List<SlotDto> slots = slotRepository
                    .findByCourtAndDateOrderByStartTime(court, date)
                    .stream().map(slotMapper::toDto).toList();
            CourtSlotsDto dto = new CourtSlotsDto();
            dto.setCourtId(court.getId());
            dto.setCourtName(court.getName());
            dto.setSlots(slots);
            return dto;
        }).toList();
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
    public SlotDto blockSlotByTime(Long courtId, Long ownerId, String dateStr, String startTimeStr, String endTimeStr) {
        CourtEntity court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", courtId));
        if (!court.getVenue().getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this court's venue");
        }
        LocalDate date      = LocalDate.parse(dateStr);
        LocalTime startTime = LocalTime.parse(startTimeStr);
        LocalTime endTime   = LocalTime.parse(endTimeStr);

        SlotEntity slot = slotRepository
                .findByCourtAndDateAndStartTime(court, date, startTime)
                .orElse(SlotEntity.builder()
                        .court(court).date(date).startTime(startTime).endTime(endTime)
                        .price(court.effectivePricePerHour())
                        .status(SlotEntity.SlotStatus.AVAILABLE)
                        .build());

        if (slot.getStatus() == SlotEntity.SlotStatus.BOOKED
                || slot.getStatus() == SlotEntity.SlotStatus.HELD) {
            throw new ConflictException("Cannot block a slot that is already booked");
        }
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
        log.info("VenueService.bulkBlockSlots() called - courtId={}, ownerId={}", courtId, ownerId);
        CourtEntity court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", courtId));
        if (!court.getVenue().getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this court's venue");
        }

        int openHour  = Integer.parseInt(court.effectiveOpenTime().split(":")[0]);
        int closeHour = Integer.parseInt(court.effectiveCloseTime().split(":")[0]);
        LocalDate blockDate = request.getDate();

        List<SlotDto> result = new ArrayList<>();
        for (int h = openHour; h < closeHour; h++) {
            LocalTime start = LocalTime.of(h, 0);
            LocalTime end   = LocalTime.of(h + 1, 0);

            SlotEntity slot = slotRepository
                    .findByCourtAndDateAndStartTime(court, blockDate, start)
                    .orElse(SlotEntity.builder()
                            .court(court)
                            .date(blockDate)
                            .startTime(start)
                            .endTime(end)
                            .price(court.effectivePricePerHour())
                            .status(SlotEntity.SlotStatus.AVAILABLE)
                            .build());

            // Never override an active booking
            if (slot.getStatus() != SlotEntity.SlotStatus.BOOKED
                    && slot.getStatus() != SlotEntity.SlotStatus.HELD) {
                slot.setStatus(SlotEntity.SlotStatus.BLOCKED);
                slot = slotRepository.save(slot);
            }
            result.add(slotMapper.toDto(slot));
        }
        return result;
    }

    @Override
    @Transactional
    public List<SlotDto> blockSlotsByTime(Long courtId, Long ownerId, BlockSelectedRequest request) {
        log.info("VenueService.blockSlotsByTime() called - courtId={}, ownerId={}, date={}, count={}",
                courtId, ownerId, request.getDate(),
                request.getStartTimes() == null ? 0 : request.getStartTimes().size());
        CourtEntity court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Court", "id", courtId));
        if (!court.getVenue().getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You do not own this court's venue");
        }

        LocalDate date = LocalDate.parse(request.getDate());
        List<SlotDto> result = new ArrayList<>();

        for (String startTimeStr : request.getStartTimes()) {
            LocalTime startTime = LocalTime.parse(startTimeStr);
            LocalTime endTime = startTime.plusHours(1);

            SlotEntity slot = slotRepository
                    .findByCourtAndDateAndStartTime(court, date, startTime)
                    .orElse(SlotEntity.builder()
                            .court(court).date(date).startTime(startTime).endTime(endTime)
                            .price(court.effectivePricePerHour())
                            .status(SlotEntity.SlotStatus.AVAILABLE)
                            .build());

            if (slot.getStatus() == SlotEntity.SlotStatus.BOOKED
                    || slot.getStatus() == SlotEntity.SlotStatus.HELD) {
                throw new ConflictException("Cannot block slot " + startTimeStr + " — already booked");
            }
            slot.setStatus(SlotEntity.SlotStatus.BLOCKED);
            result.add(slotMapper.toDto(slotRepository.save(slot)));
        }
        return result;
    }

    // ─── Owner Stats ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OwnerStatsDto getOwnerStats(Long ownerId) {
        log.info("VenueService.getOwnerStats() called - ownerId={}", ownerId);
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd   = todayStart.plusDays(1);
        LocalDateTime weekStart  = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long todayBookings = bookingRepository.countByOwnerAndDateRange(owner, todayStart, todayEnd);
        long todayRevenue  = bookingRepository.sumRevenueByOwnerAndDateRange(owner, todayStart, todayEnd, BookingEntity.PaymentStatus.SUCCESS);
        long weekRevenue   = bookingRepository.sumRevenueByOwnerAndDateRange(owner, weekStart,  todayEnd, BookingEntity.PaymentStatus.SUCCESS);
        long monthRevenue  = bookingRepository.sumRevenueByOwnerAndDateRange(owner, monthStart, todayEnd, BookingEntity.PaymentStatus.SUCCESS);
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

        validateCourtSport(venue, sport);

        if (courtRepository.existsByVenueAndName(venue, req.getName())) {
            throw new ConflictException("This venue already has a court named '" + req.getName() + "'");
        }

        // Validate custom times when provided
        if (req.getOpenTime() != null || req.getCloseTime() != null) {
            if (req.getOpenTime() != null)  validateTimeFormat(req.getOpenTime(), "openTime");
            if (req.getCloseTime() != null) validateTimeFormat(req.getCloseTime(), "closeTime");
            String effectiveOpen  = req.getOpenTime()  != null ? req.getOpenTime()  : venue.getOpenTime();
            String effectiveClose = req.getCloseTime() != null ? req.getCloseTime() : venue.getCloseTime();
            validateTimeWindow(effectiveOpen, effectiveClose);
            validateCourtTimeWithinVenue(req.getOpenTime(), req.getCloseTime(), venue);
        }

        if (req.getPricePerHour() != null && req.getPricePerHour() < 0) {
            throw new IllegalArgumentException("pricePerHour must be ≥ 0");
        }

        CourtEntity court = CourtEntity.builder()
                .venue(venue)
                .name(req.getName())
                .sport(sport)
                .type(req.getType())
                .openTime(req.getOpenTime())
                .closeTime(req.getCloseTime())
                .pricePerHour(req.getPricePerHour())
                .peakPrice(req.getPeakPrice() != null ? req.getPeakPrice() : 0)
                .slotDurationMins(req.getSlotDurationMins() != null ? req.getSlotDurationMins() : 60)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .build();

        return courtRepository.save(court);
    }

    /**
     * Validates all venue-level fields: time format, window, price, phone, email, pincode.
     */
    private void validateVenueRequest(String name, String openTime, String closeTime,
                                       Integer pricePerHour, String contactPhone,
                                       String contactEmail, String pincode) {
        if (!StringUtils.hasText(name)) throw new IllegalArgumentException("name is required");
        String open  = StringUtils.hasText(openTime)  ? openTime  : "05:00";
        String close = StringUtils.hasText(closeTime) ? closeTime : "23:00";
        validateTimeFormat(open, "openTime");
        validateTimeFormat(close, "closeTime");
        validateTimeWindow(open, close);
        if (pricePerHour != null && pricePerHour < 0) throw new IllegalArgumentException("pricePerHour must be ≥ 0");
        if (StringUtils.hasText(contactPhone))  validatePhone(contactPhone);
        if (StringUtils.hasText(contactEmail))  validateEmail(contactEmail);
        if (StringUtils.hasText(pincode))       validatePincode(pincode);
    }

    /** Time must match HH:00 where HH is 00–23. */
    private void validateTimeFormat(String time, String field) {
        if (!StringUtils.hasText(time)) return;
        if (!time.matches("^([01]\\d|2[0-3]):00$")) {
            throw new IllegalArgumentException(field + " must be on the hour in HH:00 format (e.g. 06:00). Got: " + time);
        }
    }

    /** closeTime hour must be strictly greater than openTime hour. */
    private void validateTimeWindow(String openTime, String closeTime) {
        int open  = Integer.parseInt(openTime.split(":")[0]);
        int close = Integer.parseInt(closeTime.split(":")[0]);
        if (close <= open) {
            throw new IllegalArgumentException("closeTime (" + closeTime + ") must be after openTime (" + openTime + ")");
        }
    }

    /** Court custom times must fall within the venue's open/close window. */
    private void validateCourtTimeWithinVenue(String courtOpen, String courtClose, VenueEntity venue) {
        int venueOpen  = Integer.parseInt(venue.getOpenTime().split(":")[0]);
        int venueClose = Integer.parseInt(venue.getCloseTime().split(":")[0]);
        if (courtOpen != null) {
            int co = Integer.parseInt(courtOpen.split(":")[0]);
            if (co < venueOpen) {
                throw new IllegalArgumentException(
                        "Court openTime (" + courtOpen + ") cannot be earlier than venue openTime (" + venue.getOpenTime() + ")");
            }
        }
        if (courtClose != null) {
            int cc = Integer.parseInt(courtClose.split(":")[0]);
            if (cc > venueClose) {
                throw new IllegalArgumentException(
                        "Court closeTime (" + courtClose + ") cannot be later than venue closeTime (" + venue.getCloseTime() + ")");
            }
        }
    }

    /** Court sport must be one of the venue's offered sports. */
    private void validateCourtSport(VenueEntity venue, SportEntity sport) {
        boolean offered = venue.getSports().stream()
                .anyMatch(s -> s.getId().equals(sport.getId()));
        if (!offered) {
            throw new IllegalArgumentException(
                    "Sport '" + sport.getName() + "' is not offered at this venue. "
                            + "Add it to the venue's sports first.");
        }
    }

    private void validatePhone(String phone) {
        if (!phone.matches("^[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("contactPhone must be a valid 10-digit Indian mobile number");
        }
    }

    private void validateEmail(String email) {
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("contactEmail is not a valid email address");
        }
    }

    private void validatePincode(String pincode) {
        if (!pincode.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("pincode must be exactly 6 digits");
        }
    }

    private Set<SportEntity> resolveSports(List<Long> sportIds) {
        Set<SportEntity> sports = new HashSet<>();
        if (sportIds != null) {
            for (Long sportId : sportIds) {
                SportEntity sport = sportRepository.findById(sportId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sport", "id", sportId));
                sports.add(sport);
            }
        }
        return sports;
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
