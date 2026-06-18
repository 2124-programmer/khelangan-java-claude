package com.turfbook.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardBookingCountsDto {

    @JsonProperty("today")
    private long today;

    @JsonProperty("upcoming")
    private long upcoming;

    @JsonProperty("completedLast30Days")
    private long completedLast30Days;

    @JsonProperty("cancelledLast30Days")
    private long cancelledLast30Days;
}
