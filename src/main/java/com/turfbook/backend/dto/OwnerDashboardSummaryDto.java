package com.turfbook.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDashboardSummaryDto {

    @JsonProperty("earnings")
    private DashboardEarningsDto earnings;

    @JsonProperty("bookings")
    private DashboardBookingCountsDto bookings;

    @JsonProperty("stats")
    private DashboardStatsDto stats;

    @JsonProperty("todaySlots")
    private List<DashboardSlotDto> todaySlots;
}
