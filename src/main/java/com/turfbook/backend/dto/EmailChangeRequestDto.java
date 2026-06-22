package com.turfbook.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class EmailChangeRequestDto {

    private Long id;
    private Long userId;
    private String currentEmail;
    private String newEmail;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime decidedAt;
    private String reason;
}
