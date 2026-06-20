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
public class DashboardSlotDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("courtName")
    private String courtName;

    @JsonProperty("startTime")
    private String startTime;

    @JsonProperty("endTime")
    private String endTime;

    @JsonProperty("playerName")
    private String playerName;

    @JsonProperty("sport")
    private String sport;

    @JsonProperty("status")
    private String status;
}
