package com.turfbook.backend.service;

import com.turfbook.backend.dto.*;

public interface VenueService {

    VenueSummaryPage listVenues(String city, String sport, String search, int page, int size);

    VenueDetailDto getVenue(Long id);

    VenueDetailDto createVenue(Long ownerId, CreateVenueRequest request);

    VenueDetailDto updateVenue(Long id, Long ownerId, UpdateVenueRequest request);

    VenueDetailDto updateVenueStatus(Long id, VenueStatusRequest request);

    VenueSummaryPage listOwnerVenues(Long ownerId, int page, int size);

    VenueSummaryPage adminListVenues(int page, int size, String status);

    // Courts
    java.util.List<CourtDto> listCourts(Long venueId);

    CourtDto createCourt(Long venueId, Long ownerId, CreateCourtRequest request);

    CourtDto updateCourt(Long venueId, Long courtId, Long ownerId, UpdateCourtRequest request);

    void deleteCourt(Long venueId, Long courtId, Long ownerId);

    // Slots
    java.util.List<SlotDto> listSlots(Long courtId, java.time.LocalDate date);

    java.util.List<CourtSlotsDto> listSlotsByVenue(Long venueId, java.time.LocalDate date, Long sportId);

    SlotDto blockSlot(Long slotId, Long ownerId);

    SlotDto blockSlotByTime(Long courtId, Long ownerId, String date, String startTime, String endTime);

    SlotDto unblockSlot(Long slotId, Long ownerId);

    java.util.List<SlotDto> bulkBlockSlots(Long courtId, Long ownerId, BulkBlockRequest request);

    OwnerStatsDto getOwnerStats(Long ownerId);
}
