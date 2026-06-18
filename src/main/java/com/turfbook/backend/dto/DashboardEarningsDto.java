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
public class DashboardEarningsDto {

    @JsonProperty("todayAmount")
    private long todayAmount;

    @JsonProperty("weekAmount")
    private long weekAmount;

    @JsonProperty("monthAmount")
    private long monthAmount;

    @JsonProperty("pendingAmount")
    private long pendingAmount;

    @JsonProperty("todayBookingCount")
    private long todayBookingCount;
}
