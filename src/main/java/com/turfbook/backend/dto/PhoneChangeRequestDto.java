package com.turfbook.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class PhoneChangeRequestDto {

    private Long id;
    private Long userId;
    private String currentPhone;
    private String newPhone;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime decidedAt;
    private String reason;
}
