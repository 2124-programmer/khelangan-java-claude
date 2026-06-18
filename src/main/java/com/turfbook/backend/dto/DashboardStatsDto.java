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
public class DashboardStatsDto {

    @JsonProperty("usersConnected")
    private long usersConnected;

    @JsonProperty("venueCount")
    private long venueCount;

    @JsonProperty("courtCount")
    private long courtCount;
}
